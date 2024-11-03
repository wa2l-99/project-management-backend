package com.pmt.project_management.project;

import com.pmt.project_management.task.TaskResponse;
import com.pmt.project_management.user.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectMapper {

    public Project toProject(ProjectRequest request) {
        if (request == null) {
            return null;
        }
        Project project = Project.builder().id(request.getId()).name(request.getName()).description(request.getDescription()).startDate(request.getStartDate()).build();

        // Initialiser explicitement la collection members
        if (project.getMembers() == null) {
            project.setMembers(new HashSet<>());
        }

        return project;
    }

    public ProjectResponse toProjectResponse(Project project) {
        if (project == null) {
            return null;
        }

        // Mapper les membres du projet avec leur nom et rôle
        Set<ProjectMemberResponse> members = project.getMembers().stream().map(member -> {
            String roleName = (member.getRole() != null) ? member.getRole().getNom().name() : "No role"; // Default if no role

            return ProjectMemberResponse.builder().nom(member.getNom())
                    .prenom(member.getPrenom())
                    .email(member.getEmail())
                    .role(roleName)// Rôle du membre
                    .build();
        }).collect(Collectors.toSet());

        // Map tasks with minimal assignedTo information
        Set<TaskResponse> tasks = project.getTasks().stream().map(task -> {
            boolean isAssigned = task.getAssignedTo() != null;

            String assignedTo = (isAssigned)
                    ? task.getAssignedTo().getFullName() + " (" + task.getAssignedTo().getEmail() + ")"
                    : "tâche non affectée";

            return TaskResponse.builder()
                    .id(task.getId())
                    .name(task.getName())
                    .description(task.getDescription())
                    .dueDate(task.getDueDate())
                    .priority(task.getPriority())
                    .status(task.getStatus())
                    .assignedTo(assignedTo)
                    .projectName(task.getProject().getName())
                    .isAssigned(isAssigned)
                    .build();
        }).collect(Collectors.toSet());

        return ProjectResponse.builder().id(project.getId()).name(project.getName()).description(project.getDescription()).startDate(project.getStartDate()).owner(project.getOwner().getNom()).members(members).tasks(tasks).build();
    }
}