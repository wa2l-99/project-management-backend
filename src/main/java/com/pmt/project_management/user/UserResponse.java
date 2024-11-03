package com.pmt.project_management.user;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String role;

}

