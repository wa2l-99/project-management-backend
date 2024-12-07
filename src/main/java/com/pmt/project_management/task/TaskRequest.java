package com.pmt.project_management.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    private Integer id;

    @NotBlank(message = "Le nom de la tâche est obligatoire")
    private String name;

    @NotBlank(message = "La description de la tâche est obligatoire")
    private String description;

    @NotNull(message = "La date d'échéance de la tâche est obligatoire")
    private LocalDate dueDate;

    @NotNull(message = "La priorité de la tâche est obligatoire")
    private EPriority priority;

    private EStatus status;


}
