package com.pmt.project_management.mapper;

import com.pmt.project_management.project.Project;
import com.pmt.project_management.project.ProjectMapper;
import com.pmt.project_management.project.ProjectRequest;
import com.pmt.project_management.project.ProjectResponse;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.task.EPriority;
import com.pmt.project_management.task.EStatus;
import com.pmt.project_management.task.Task;
import com.pmt.project_management.task.TaskResponse;
import com.pmt.project_management.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperTest {

    private ProjectMapper projectMapper;

    @BeforeEach
    void setUp() {
        projectMapper = new ProjectMapper();
    }

    @Test
    void toProject_WhenRequestIsValid_ShouldReturnProject() {
        // Arrange
        ProjectRequest request = ProjectRequest.builder()
                .id(1)
                .name("Test Project")
                .description("Test Description")
                .startDate(LocalDate.now())
                .build();

        // Act
        Project project = projectMapper.toProject(request);

        // Assert
        assertNotNull(project);
        assertEquals(request.getId(), project.getId());
        assertEquals(request.getName(), project.getName());
        assertEquals(request.getDescription(), project.getDescription());
        assertEquals(request.getStartDate(), project.getStartDate());
        assertNotNull(project.getMembers());
    }

    @Test
    void toProject_WhenRequestIsNull_ShouldReturnNull() {
        // Act
        Project project = projectMapper.toProject(null);

        // Assert
        assertNull(project);
    }

    @Test
    void toProjectResponse_WhenProjectIsValid_ShouldReturnProjectResponse() {
        // Arrange
        Role role = new Role();
        role.setNom(ERole.MEMBER);

        User owner = User.builder()
                .id(1)
                .nom("Owner Name")
                .prenom("Owner Firstname")
                .email("owner@example.com")
                .role(role)
                .build();

        User member = User.builder()
                .id(2)
                .nom("Member Name")
                .prenom("Member Firstname")
                .email("member@example.com")
                .role(role)
                .build();

        // Créer le projet
        Project project = Project.builder()
                .id(1)
                .name("Test Project")
                .description("Test Description")
                .startDate(LocalDate.now())
                .owner(owner)
                .members(Set.of(member))
                .tasks(new ArrayList<>()) // Initialiser les tâches
                .build();

        // Créer une tâche et l'associer au projet
        Task task = Task.builder()
                .id(1)
                .name("Test Task")
                .description("Task Description")
                .dueDate(LocalDate.now().plusDays(5))
                .priority(EPriority.HIGH)
                .status(EStatus.IN_PROGRESS)
                .assignedTo(member)
                .project(project) // Associer la tâche au projet
                .build();

        project.getTasks().add(task); // Ajouter la tâche au projet

        // Act
        ProjectResponse response = projectMapper.toProjectResponse(project);

        // Assert
        assertNotNull(response);
        assertEquals(project.getId(), response.getId());
        assertEquals(project.getName(), response.getName());
        assertEquals(project.getDescription(), response.getDescription());
        assertEquals(project.getStartDate(), response.getStartDate());
        assertEquals(project.getOwner().getNom(), response.getOwner());
        assertNotNull(response.getMembers());
        assertEquals(1, response.getMembers().size());
        assertNotNull(response.getTasks());
        assertEquals(1, response.getTasks().size());

        TaskResponse taskResponse = response.getTasks().iterator().next();
        assertEquals(task.getId(), taskResponse.getId());
        assertEquals(task.getName(), taskResponse.getName());
        assertEquals(task.getDescription(), taskResponse.getDescription());
        assertEquals(task.getDueDate(), taskResponse.getDueDate());
        assertEquals(task.getPriority(), taskResponse.getPriority());
        assertEquals(task.getStatus(), taskResponse.getStatus());
        assertTrue(taskResponse.isAssigned());
        assertEquals(task.getAssignedTo().getFullName() + " (" + task.getAssignedTo().getEmail() + ")", taskResponse.getAssignedTo());
        assertEquals(project.getName(), taskResponse.getProjectName());
    }

    @Test
    void toProjectResponse_WhenProjectIsNull_ShouldReturnNull() {
        // Act
        ProjectResponse response = projectMapper.toProjectResponse(null);

        // Assert
        assertNull(response);
    }

    @Test
    void toProjectResponse_WhenProjectHasNoMembersOrTasks_ShouldHandleGracefully() {
        // Arrange
        User owner = User.builder()
                .id(1)
                .nom("Owner Name")
                .prenom("Owner Firstname")
                .email("owner@example.com")
                .build();

        Project project = Project.builder()
                .id(1)
                .name("Test Project")
                .description("Test Description")
                .startDate(LocalDate.now())
                .owner(owner)
                .members(new HashSet<>()) // No members
                .tasks(new ArrayList<>()) // No tasks
                .build();

        // Act
        ProjectResponse response = projectMapper.toProjectResponse(project);

        // Assert
        assertNotNull(response);
        assertEquals(project.getId(), response.getId());
        assertEquals(project.getName(), response.getName());
        assertEquals(project.getDescription(), response.getDescription());
        assertEquals(project.getStartDate(), response.getStartDate());
        assertEquals(project.getOwner().getNom(), response.getOwner());
        assertTrue(response.getMembers().isEmpty());
        assertTrue(response.getTasks().isEmpty());
    }
}