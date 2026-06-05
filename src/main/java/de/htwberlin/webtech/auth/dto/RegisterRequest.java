package de.htwberlin.webtech.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "must not be blank")
    private String username;

    @Email(message = "must be a valid email address")
    @NotBlank(message = "must not be blank")
    private String email;

    @Size(min = 8, message = "must be at least 8 characters")
    @NotBlank(message = "must not be blank")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
