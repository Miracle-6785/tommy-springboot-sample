package com.example.demo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "Name is required")
        private String name;
        
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is required")
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        @NotBlank(message = "Name is required")
        private String name;
        
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is required")
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        
        public static UserResponse fromEntity(User user) {
            return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
            );
        }
    }
} 