package com.pmt.project_management.service;

import com.pmt.project_management.email.EmailService;
import com.pmt.project_management.history.TaskModifiedHistory;
import com.pmt.project_management.history.TaskModifiedHistoryRepository;
import com.pmt.project_management.project.Project;
import com.pmt.project_management.project.ProjectRepository;
import com.pmt.project_management.task.*;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskModifiedHistoryRepository taskModifiedHistoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Project testProject;
    private Task testTask;
    private TaskRequest taskRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        // Configurez un utilisateur fictif
        testUser = User.builder()
                .id(1)
                .nom("Test User")
                .build();

        // Configurez un projet fictif
        testProject = Project.builder()
                .id(1)
                .name("Test Project")
                .owner(testUser)
                .members(new HashSet<>())
                .build();

        // Configurez une tâche fictive
        testTask = Task.builder()
                .id(1)
                .name("Test Task")
                .description("Test Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.MEDIUM)
                .status(EStatus.TODO)
                .project(testProject)
                .build();

        // Configurez un TaskRequest fictif
        taskRequest = TaskRequest.builder()
                .name("New Task")
                .description("New Task Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.HIGH)
                .status(EStatus.IN_PROGRESS)
                .build();

        // Configurez un TaskResponse fictif
        taskResponse = TaskResponse.builder()
                .id(1)
                .name("New Task")
                .description("New Task Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.HIGH)
                .status(EStatus.IN_PROGRESS)
                .build();

        // Simulez le principal dans Authentication
        when(authentication.getPrincipal()).thenReturn(testUser);
    }

    @Test
    void createTask_ShouldCreateAndReturnTaskResponse() {
        // Configurez les mocks
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(taskMapper.toTaskRequest(taskRequest, testProject)).thenReturn(testTask);
        when(taskRepository.save(testTask)).thenReturn(testTask);
        when(taskMapper.toTaskResponse(testTask)).thenReturn(taskResponse);

        // Appelez le service
        TaskResponse result = taskService.createTask(testProject.getId(), taskRequest, authentication);

        // Vérifiez les interactions et les assertions
        verify(projectRepository).findById(testProject.getId());
        verify(taskRepository).save(testTask);
        verify(taskMapper).toTaskResponse(testTask);
        assertEquals(taskResponse, result);
    }

    @Test
    void createTask_ShouldThrowException_WhenUserNotMemberOrAdmin() {
        // Arrange
        Project mockProject = Project.builder()
                .id(1)
                .name("Mock Project")
                .owner(User.builder().id(999).build()) // Utilisateur différent
                .members(Collections.emptySet()) // Pas de membres
                .build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(mockProject));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            taskService.createTask(1, taskRequest, authentication);
        });

        assertEquals("Vous devez être membre ou administrateur pour créer une tâche dans ce projet.", exception.getMessage());
    }

    @Test
    void createTask_ShouldThrowException_WhenProjectNotFound() {
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                taskService.createTask(testProject.getId(), taskRequest, authentication));

        assertEquals("Projet non trouvé avec l'ID : " + testProject.getId(), exception.getMessage());
    }

    @Test
    void updateTask_ShouldUpdateAndReturnTaskResponse() {
        // Mock des données
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
        when(taskMapper.toTaskHistoryResponse(any(Task.class), any(Task.class)))
                .thenReturn(TaskHistoryResponse.builder()
                        .taskId(testTask.getId())
                        .modificationDescription("Changements effectués.")
                        .build());
        when(taskMapper.toTaskResponse(testTask)).thenReturn(taskResponse);

        TaskResponse result = taskService.updateTask(testTask.getId(), taskRequest, authentication);

        verify(taskRepository).save(testTask);
        verify(taskMapper).toTaskResponse(testTask);
        assertEquals(taskResponse, result);
    }

    @Test
    void getTaskById_ShouldReturnTaskResponse() {
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
        when(taskMapper.toTaskResponse(testTask)).thenReturn(taskResponse);

        TaskResponse result = taskService.getTaskById(testTask.getId(), authentication);

        verify(taskRepository).findById(testTask.getId());
        assertEquals(taskResponse, result);
    }

    @Test
    void deleteTask_ShouldDeleteTask() {
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));

        taskService.deleteTask(testTask.getId(), authentication);

        verify(taskRepository).delete(testTask);
    }

    @Test
    void assignTaskToMember_ShouldAssignTaskAndSendEmail() throws MessagingException {
        // Configurez testUser avec un email valide et un nom complet
        testUser.setEmail("testuser@example.com");
        testUser.setNom("User");
        testUser.setPrenom("Test");

        // Configurez les membres du projet
        Set<User> projectMembers = new HashSet<>();
        projectMembers.add(testUser);
        testProject.setMembers(projectMembers);

        // Configurez la relation entre la tâche et le projet
        testTask.setProject(testProject);

        // Simulez les appels des mocks
        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(taskMapper.toTaskResponse(testTask)).thenReturn(taskResponse);

        // Appel de la méthode à tester
        TaskResponse result = taskService.assignTaskToMember(testTask.getId(), testUser.getId(), authentication);

        // Vérifiez les interactions
        verify(taskRepository).save(testTask);
        verify(emailService).sendTaskAssignmentEmail(
                eq("testuser@example.com"),      // Email
                eq("Test User"),                // Nom complet
                eq(testTask.getName()),         // Nom de la tâche
                eq(testProject.getName()),      // Nom du projet
                eq("Affectation de tâche")      // Objet du message
        );

        // Assertion sur le résultat
        assertEquals(taskResponse, result);
    }

    @Test
    void assignTaskToMember_ShouldThrowException_WhenMemberNotInProject() {
        // Arrange
        testProject.setOwner(testUser); // Assurez-vous que le champ owner est configuré

        testTask.setProject(testProject);

        User nonMember = User.builder()
                .id(3)
                .nom("Non-Member")
                .build();

        when(taskRepository.findById(testTask.getId())).thenReturn(Optional.of(testTask));
        when(userRepository.findById(nonMember.getId())).thenReturn(Optional.of(nonMember));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            taskService.assignTaskToMember(testTask.getId(), nonMember.getId(), authentication);
        });

        assertEquals("L'utilisateur n'est pas membre de ce projet.", exception.getMessage());
    }


    @Test
    void getTaskModificationsForUserProjects_ShouldReturnEmptyList_WhenNoProjectsFound() {
        // Arrange
        when(projectRepository.findByMembersContaining(eq(testUser), any(Pageable.class)))
                .thenReturn(Page.empty()); // Aucun projet trouvé

        // Act
        List<TaskHistoryResponse> historyResponses = taskService.getTaskModificationsForUserProjects(authentication, 0, 10);

        // Assert
        assertNotNull(historyResponses);
        assertTrue(historyResponses.isEmpty());
    }


}