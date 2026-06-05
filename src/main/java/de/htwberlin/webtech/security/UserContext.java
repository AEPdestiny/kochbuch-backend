package de.htwberlin.webtech.security;

import de.htwberlin.webtech.auth.service.TokenService;
import de.htwberlin.webtech.shared.exception.UnauthorizedException;
import de.htwberlin.webtech.user.entity.AppUser;
import de.htwberlin.webtech.user.repository.AppUserRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserContext {

    private final TokenService tokenService;
    private final AppUserRepository userRepository;

    public UserContext(TokenService tokenService, AppUserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    public AppUser requireUser(String authorizationHeader) {
        String email = tokenService.emailFromBearerToken(authorizationHeader);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user could not be found."));
    }
}
