package com.pmt.project_management.controller;

import com.pmt.project_management.task.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskControllerUnitTest {

    @Mock
    private TaskService taskService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskController taskController;

    private TaskRequest taskRequest;
    private TaskResponse taskResponse;
    private Integer projectId;
    private Integer taskId;
    private Integer memberId;

    @BeforeEach
    public void setUp() {
        projectId = 1;
        taskId = 100;
        memberId = 200;

        taskRequest = TaskRequest.builder()
                .name("Test Task")
                .description("Test Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.MEDIUM)
                .status(EStatus.TODO)
                .build();

        taskResponse = TaskResponse.builder()
                .id(taskId)
                .name(taskRequest.getName())
                .description(taskRequest.getDescription())
                .dueDate(taskRequest.getDueDate())
                .priority(taskRequest.getPriority())
                .status(taskRequest.getStatus())
                .build();
    }

    @Test
    public void testCreateTask_Success() {
        // Arrange
        when(taskService.createTask(eq(projectId), eq(taskRequest), eq(authentication)))
                .thenReturn(taskResponse);

        // Act
        ResponseEntity<TaskResponse> response = taskController.createTask(projectId, taskRequest, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(taskResponse, response.getBody());
        verify(taskService).createTask(projectId, taskRequest, authentication);
    }

    @Test
    public void testCreateTask_WhenTaskServiceThrowsException() {
        // Arrange
        when(taskService.createTask(eq(projectId), eq(taskRequest), eq(authentication)))
                .thenThrow(new IllegalStateException("Task creation failed"));

        // Act
        Exception exception = assertThrows(IllegalStateException.class, () ->
                taskController.createTask(projectId, taskRequest, authentication));

        // Assert
        assertNotNull(exception);
        assertEquals("Task creation failed", exception.getMessage());
        verify(taskService).createTask(projectId, taskRequest, authentication);
    }

    @Test
    public void testCreateTask_WithBoundaryDueDate() {
        // Arrange
        TaskRequest boundaryTaskRequest = TaskRequest.builder()
                .name("Boundary Task")
                .description("Task with a past due date")
                .dueDate(LocalDate.now().minusDays(1)) // Past date
                .priority(EPriority.LOW)
                .status(EStatus.DONE)
                .build();

        when(taskService.createTask(eq(projectId), eq(boundaryTaskRequest), eq(authentication)))
                .thenReturn(taskResponse);

        // Act
        ResponseEntity<TaskResponse> response = taskController.createTask(projectId, boundaryTaskRequest, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(taskResponse, response.getBody());
        verify(taskService).createTask(projectId, boundaryTaskRequest, authentication);
    }


    @Test
    public void testUpdateTask_Success() {
        // Arrange
        when(taskService.updateTask(eq(taskId), eq(taskRequest), eq(authentication)))
                .thenReturn(taskResponse);

        // Act
        ResponseEntity<TaskResponse> response = taskController.updateTask(taskId, taskRequest, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponse, response.getBody());
        verify(taskService).updateTask(taskId, taskRequest, authentication);
    }

    @Test
    public void testFindAllTasksByProject_Success() {
        // Arrange
        List<TaskResponse> taskResponses = List.of(taskResponse);
        when(taskService.getAllTasksByProject(eq(projectId), eq(authentication)))
                .thenReturn(taskResponses);

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.findAllTasksByProject(projectId, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponses, response.getBody());
        verify(taskService).getAllTasksByProject(projectId, authentication);
    }

    @Test
    public void testGetTaskById_Success() {
        // Arrange
        when(taskService.getTaskById(eq(taskId), eq(authentication)))
                .thenReturn(taskResponse);

        // Act
        ResponseEntity<TaskResponse> response = taskController.getTaskById(taskId, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponse, response.getBody());
        verify(taskService).getTaskById(taskId, authentication);
    }

    @Test
    public void testDeleteTask_Success() {
        // Act
        doNothing().when(taskService).deleteTask(eq(taskId), eq(authentication));
        ResponseEntity<String> response = taskController.deleteTask(taskId, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("La tâche a été supprimé avec succès.", response.getBody());
        verify(taskService).deleteTask(taskId, authentication);
    }

    @Test
    public void testGetTasksByStatus_Success() {
        // Arrange
        List<TaskResponse> taskResponses = List.of(taskResponse);
        EStatus status = EStatus.TODO;
        when(taskService.getTasksByStatus(eq(projectId), eq(status), eq(authentication)))
                .thenReturn(taskResponses);

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getTasksByStatus(projectId, status, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponses, response.getBody());
        verify(taskService).getTasksByStatus(projectId, status, authentication);
    }

    @Test
    public void testGetTasksByPriority_Success() {
        // Arrange
        List<TaskResponse> taskResponses = List.of(taskResponse);
        EPriority priority = EPriority.MEDIUM;
        when(taskService.getTasksByPriority(eq(projectId), eq(priority), eq(authentication)))
                .thenReturn(taskResponses);

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getTasksByPriority(projectId, priority, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponses, response.getBody());
        verify(taskService).getTasksByPriority(projectId, priority, authentication);
    }

    @Test
    public void testGetTaskModificationsForUserProjects_Success() {
        // Arrange
        List<TaskHistoryResponse> historyResponses = List.of(
                TaskHistoryResponse.builder()
                        .taskId(taskId)
                        .taskName("Modified Task")
                        .build()
        );
        when(taskService.getTaskModificationsForUserProjects(eq(authentication), eq(0), eq(10)))
                .thenReturn(historyResponses);

        // Act
        ResponseEntity<List<TaskHistoryResponse>> response = taskController.getTaskModificationsForUserProjects(authentication, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(historyResponses, response.getBody());
        verify(taskService).getTaskModificationsForUserProjects(authentication, 0, 10);
    }

    @Test
    public void testAssignTaskToMember_Success() throws MessagingException {
        when(taskService.assignTaskToMember(eq(taskId), eq(memberId), eq(authentication)))
                .thenReturn(taskResponse);

        ResponseEntity<TaskResponse> response = taskController.assignTaskToMember(taskId, memberId, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(taskResponse, response.getBody());
        verify(taskService).assignTaskToMember(taskId, memberId, authentication);
    }

    @Test
    public void testAssignTaskToMember_WhenUserUnauthorized() throws MessagingException {
        // Arrange
        doThrow(new IllegalStateException("Unauthorized access"))
                .when(taskService).assignTaskToMember(eq(taskId), eq(memberId), eq(authentication));

        // Act
        Exception exception = assertThrows(IllegalStateException.class, () ->
                taskController.assignTaskToMember(taskId, memberId, authentication));

        // Assert
        assertNotNull(exception);
        assertEquals("Unauthorized access", exception.getMessage());
        verify(taskService).assignTaskToMember(taskId, memberId, authentication);
    }

    @Test
    public void testGetAllTasks_Success() {
        // Arrange
        List<TaskResponse> tasks = List.of(taskResponse);
        when(taskService.getAllTasks(eq(authentication)))
                .thenReturn(tasks);

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getAllTasks(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tasks, response.getBody());
        verify(taskService).getAllTasks(authentication);
    }

    @Test
    public void testGetAllTasks_WhenNoTasksExist() {
        // Arrange
        when(taskService.getAllTasks(eq(authentication))).thenReturn(List.of());

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getAllTasks(authentication);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(taskService).getAllTasks(authentication);
    }

}