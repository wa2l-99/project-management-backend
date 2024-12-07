package com.pmt.project_management.project;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    private Integer id;

    @NotEmpty(message = "Le nom du projet ne doit pas être vide")
    @Size(max = 100, message = "Le nom du projet ne doit pas dépasser 100 caractères")
    private String name;

    @NotNull(message = "La description ne doit pas être nulle")
    @Size(max = 500, message = "Le texte de la description ne doit pas dépasser 500 caractères")
    private String description;

    @NotNull(message = "La date de début ne doit pas être nulle")
    @Column(nullable = false)
    private LocalDate startDate;

}
