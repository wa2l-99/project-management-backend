package com.pmt.project_management.task;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Integer id;
    private String name;
    private String description;
    private LocalDate dueDate;
    private EPriority priority;
    private EStatus status;
    private boolean isAssigned;
    private String assignedTo;
    private String projectName;
}