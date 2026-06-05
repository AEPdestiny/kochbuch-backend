package de.htwberlin.webtech.auth.service;

import de.htwberlin.webtech.auth.dto.AuthResponse;
import de.htwberlin.webtech.auth.dto.LoginRequest;
import de.htwberlin.webtech.auth.dto.RegisterRequest;
import de.htwberlin.webtech.security.PasswordService;
import de.htwberlin.webtech.shared.exception.ConflictException;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.dto.UserResponse;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.entity.Role;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;

    public AuthService(AppUserRepository userRepository, PasswordService passwordService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already registered.");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordService.hash(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.persistAndFlush(user);
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        return authResponse(user);
    }

    public UserResponse currentUser(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user could not be found."));
        return toUserResponse(user);
    }

    private AuthResponse authResponse(AppUser user) {
        return new AuthResponse(
                tokenService.createAccessToken(user),
                "Bearer",
                tokenService.getExpiresInSeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(AppUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
