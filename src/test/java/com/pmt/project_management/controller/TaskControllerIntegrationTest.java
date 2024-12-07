package com.pmt.project_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmt.project_management.project.Project;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.role.RoleRepository;
import com.pmt.project_management.task.*;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import com.pmt.project_management.project.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project testProject;
    private User adminUser;
    private User memberUser;
    private Authentication adminAuthentication;
    private Authentication memberAuthentication;

    @BeforeEach
    public void setup() {


        // Créer ou récupérer les rôles
        Role adminRole = roleRepository.findByNom(ERole.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setNom(ERole.ADMIN);
                    return roleRepository.save(role);
                });

        Role memberRole = roleRepository.findByNom(ERole.MEMBER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setNom(ERole.MEMBER);
                    return roleRepository.save(role);
                });

        // Créer ou récupérer les utilisateurs de test
        adminUser = userRepository.findByEmail("admin@example.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(1);
                    user.setEmail("admin@example.com");
                    user.setNom("Admin");
                    user.setPrenom("User");
                    user.setPassword("password");
                    user.setRole(adminRole);
                    user.setCreatedDate(LocalDateTime.now());
                    return userRepository.save(user);
                });

        memberUser = userRepository.findByEmail("member@example.com")
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(2);
                    user.setEmail("member@example.com");
                    user.setNom("Member");
                    user.setPrenom("User");
                    user.setPassword("password");
                    user.setRole(memberRole);
                    user.setCreatedDate(LocalDateTime.now());
                    return userRepository.save(user);
                });

        // Créer des authentifications manuellement
        adminAuthentication = new UsernamePasswordAuthenticationToken(
                adminUser,
                adminUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(adminUser.getRole().getNom().name()))
        );

        memberAuthentication = new UsernamePasswordAuthenticationToken(
                memberUser,
                memberUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(memberUser.getRole().getNom().name()))
        );


        // Récupérer ou créer un projet de test
        testProject = projectRepository.findById(1)
                .orElseGet(() -> {
                    Project project = new Project();
                    project.setId(1);
                    project.setCreatedBy(1);
                    project.setOwner(adminUser);
                    project.setName("Test Project");
                    project.setDescription("Description du projet de test");
                    project.setStartDate(LocalDate.now());
                    project.setMembers(Set.of(memberUser, adminUser));
                    return projectRepository.save(project);
                });

        // Définir le contexte de sécurité
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
    }

    @Test
    public void testCreateTask_Success() throws Exception {

        // Préparer la requête de création de tâche
        TaskRequest taskRequest = TaskRequest.builder()
                .name("Test Task")
                .description("Test Description")
                .dueDate(LocalDate.now().plusDays(7))
                .priority(EPriority.MEDIUM)
                .status(EStatus.TODO)
                .build();

        // Simuler la création de tâche pour un projet existant
        mockMvc.perform(post("/api/tasks/projectId=" + testProject.getId() + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest))
                        .principal(adminAuthentication))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    public void testUpdateTask_Success() throws Exception {
        // Créer d'abord une tâche pour pouvoir la mettre à jour
        TaskResponse createdTask = taskService.createTask(testProject.getId(), TaskRequest.builder()
                .name("Original Task")
                .description("Original Description")
                .priority(EPriority.LOW)
                .status(EStatus.TODO)
                .dueDate(LocalDate.of(2024, 12, 10))
                .build(), memberAuthentication);

        // Préparer la requête de mise à jour
        TaskRequest updateRequest = TaskRequest.builder()
                .name("Updated Task")
                .description("Updated Description")
                .priority(EPriority.HIGH)
                .dueDate(LocalDate.of(2024, 12, 15))
                .status(EStatus.IN_PROGRESS)
                .build();

        mockMvc.perform(put("/api/tasks/" + createdTask.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .principal(memberAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Task"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        ;
    }

    @Test
    public void testGetTaskById_Success() throws Exception {
        // Créer une tâche pour ensuite la récupérer
        TaskResponse createdTask = taskService.createTask(testProject.getId(), TaskRequest.builder()
                .name("Task to Retrieve")
                .description("Retrieve Description")
                .build(), memberAuthentication);

        mockMvc.perform(get("/api/tasks/" + createdTask.getId())
                        .principal(memberAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Task to Retrieve"));
    }

    @Test
    public void testDeleteTask_Success() throws Exception {
        // Créer une tâche à supprimer
        TaskResponse createdTask = taskService.createTask(testProject.getId(), TaskRequest.builder()
                .name("Task to Delete")
                .build(), memberAuthentication);

        mockMvc.perform(delete("/api/tasks/" + createdTask.getId())
                        .principal(adminAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().string("La tâche a été supprimé avec succès."));
    }

    @Test
    public void testAssignTaskToMember_Success() throws Exception {
        // Créer une tâche à assigner
        TaskResponse createdTask = taskService.createTask(testProject.getId(), TaskRequest.builder()
                .id(1)
                .name("Original Task")
                .description("Original Description")
                .priority(EPriority.LOW)
                .status(EStatus.TODO)
                .dueDate(LocalDate.of(2024, 12, 10))
                .build(), adminAuthentication);

        // Assigner la tâche à un autre membre
        mockMvc.perform(post("/api/tasks/" + createdTask.getId() + "/assign")
                        .param("memberId", adminUser.getId().toString())
                        .principal(adminAuthentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value(adminUser.getFullName() + " (" + adminUser.getEmail() + ")"))
                .andExpect(jsonPath("$.assigned").value(true));
    }

    @Test
    public void testGetTasksByStatus_Success() throws Exception {
        mockMvc.perform(get("/api/tasks/projectId=" + testProject.getId() + "/tasksByStatus")
                        .param("status", EStatus.TODO.name())
                        .principal(memberAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testGetTasksByPriority_Success() throws Exception {
        mockMvc.perform(get("/api/tasks/projectId=" + testProject.getId() + "/tasksByPriority")
                        .param("priority", EPriority.MEDIUM.name())
                        .principal(memberAuthentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}