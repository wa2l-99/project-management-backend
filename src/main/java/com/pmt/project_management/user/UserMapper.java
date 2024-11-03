package com.pmt.project_management.user;


import com.pmt.project_management.auth.RegistrationRequest;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class UserMapper {

    public User toUser(RegistrationRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .id(request.getId())
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .build();
    }

    public UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        String roleName = (user.getRole() != null) ? user.getRole().getNom().name() : "No role";

        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(roleName)  // Obtenir le nom unique du rôle
                .build();
    }
}
