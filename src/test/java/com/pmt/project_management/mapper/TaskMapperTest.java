package com.pmt.project_management.mapper;

import com.pmt.project_management.project.Project;
import com.pmt.project_management.task.*;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskMapper taskMapper;

    private Project project;
    private Task task;
    private User user;
    private TaskRequest taskRequest;

    @BeforeEach
    public void setUp() {
        project = Project.builder()
                .id(1)
                .name("Test Project")
                .build();

        user = User.builder()
                .id(100)
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .build();

        task = Task.builder()
                .id(1)
                .name("Original Task")
                .description("Original Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.MEDIUM)
                .status(EStatus.TODO)
                .project(project)
                .assignedTo(user)
                .lastModifiedBy(user.getId())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        taskRequest = TaskRequest.builder()
                .name("New Task")
                .description("New Description")
                .dueDate(LocalDate.now().plusDays(10))
                .priority(EPriority.HIGH)
                .build();
    }

    @Test
    public void testToTaskRequest_ValidInput() {
        // Act
        Task mappedTask = taskMapper.toTaskRequest(taskRequest, project);

        // Assert
        assertNotNull(mappedTask);
        assertEquals(taskRequest.getName(), mappedTask.getName());
        assertEquals(taskRequest.getDescription(), mappedTask.getDescription());
        assertEquals(taskRequest.getDueDate(), mappedTask.getDueDate());
        assertEquals(taskRequest.getPriority(), mappedTask.getPriority());
        assertEquals(EStatus.TODO, mappedTask.getStatus());
        assertEquals(project, mappedTask.getProject());
        assertNull(mappedTask.getAssignedTo());
    }

    @Test
    public void testToTaskRequest_NullInput() {
        // Act
        Task mappedTask = taskMapper.toTaskRequest(null, project);

        // Assert
        assertNull(mappedTask);
    }

    @Test
    public void testToTaskResponse_AssignedTask() {
        // Act
        TaskResponse taskResponse = taskMapper.toTaskResponse(task);

        // Assert
        assertNotNull(taskResponse);
        assertEquals(task.getId(), taskResponse.getId());
        assertEquals(task.getName(), taskResponse.getName());
        assertEquals(task.getDescription(), taskResponse.getDescription());
        assertEquals(task.getDueDate(), taskResponse.getDueDate());
        assertEquals(task.getPriority(), taskResponse.getPriority());
        assertEquals(task.getStatus(), taskResponse.getStatus());
        assertTrue(taskResponse.isAssigned());
        assertEquals("John Doe (john.doe@example.com)", taskResponse.getAssignedTo());
        assertEquals(project.getName(), taskResponse.getProjectName());
    }

    @Test
    public void testToTaskResponse_UnassignedTask() {
        // Arrange
        Task unassignedTask = Task.builder()
                .id(1)
                .name("Unassigned Task")
                .description("Unassigned Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.LOW)
                .status(EStatus.TODO)
                .project(project)
                .assignedTo(null)
                .build();

        // Act
        TaskResponse taskResponse = taskMapper.toTaskResponse(unassignedTask);

        // Assert
        assertNotNull(taskResponse);
        assertFalse(taskResponse.isAssigned());
        assertEquals("tâche non affectée", taskResponse.getAssignedTo());
    }

    @Test
    public void testToTaskResponse_NullInput() {
        // Act
        TaskResponse taskResponse = taskMapper.toTaskResponse(null);

        // Assert
        assertNull(taskResponse);
    }

    @Test
    public void testToTaskHistoryResponse_Success() {
        // Arrange
        Task newTask = Task.builder()
                .id(1)
                .name("Updated Task")
                .description("Updated Description")
                .dueDate(LocalDate.now().plusDays(14))
                .priority(EPriority.HIGH)
                .status(EStatus.IN_PROGRESS)
                .project(project)
                .lastModifiedBy(user.getId())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Act
        TaskHistoryResponse historyResponse = taskMapper.toTaskHistoryResponse(task, newTask);

        // Assert
        assertNotNull(historyResponse);
        assertEquals(newTask.getId(), historyResponse.getTaskId());
        assertEquals(newTask.getName(), historyResponse.getTaskName());
        assertEquals(project.getName(), historyResponse.getProjectName());
        assertEquals(user.getId(), historyResponse.getLastModifiedById());
        assertEquals(user.getFullName(), historyResponse.getLastModifiedByName());
        assertNotNull(historyResponse.getLastModifiedDate());
        assertNotNull(historyResponse.getModificationDescription());
    }

    @Test
    public void testToTaskHistoryResponse_UserNotFound() {
        // Arrange
        Task newTask = Task.builder()
                .id(1)
                .lastModifiedBy(999)
                .build();

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                taskMapper.toTaskHistoryResponse(task, newTask)
        );
    }

    @Test
    public void testGenerateHistoryDescription_AllModifications() {
        // Arrange
        Task newTask = Task.builder()
                .name("Updated Task")
                .description("Updated Description")
                .dueDate(LocalDate.now().plusDays(14))
                .priority(EPriority.HIGH)
                .status(EStatus.IN_PROGRESS)
                .build();

        // Act
        String description = taskMapper.generateHistoryDescription(task, newTask);

        // Assert
        assertTrue(description.contains("Nom modifié"));
        assertTrue(description.contains("Description modifiée"));
        assertTrue(description.contains("Date d'échéance modifiée"));
        assertTrue(description.contains("Priorité modifiée"));
        assertTrue(description.contains("Statut modifié"));
    }

    @Test
    public void testGenerateHistoryDescription_NoModifications() {
        // Act
        String description = taskMapper.generateHistoryDescription(task, task);

        // Assert
        assertEquals("Modifications apportées : Aucune modification détectée.", description);
    }
}