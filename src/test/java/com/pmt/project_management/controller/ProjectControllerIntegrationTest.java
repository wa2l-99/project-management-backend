package com.pmt.project_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmt.project_management.common.PageResponse;
import com.pmt.project_management.project.Project;
import com.pmt.project_management.project.ProjectRepository;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.RoleRepository;
import com.pmt.project_management.project.ProjectRequest;
import com.pmt.project_management.project.InviteMemberRequest;
import com.pmt.project_management.project.ProjectResponse;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        // Supprimer les données pour éviter les conflits
        projectRepository.deleteAll();

        // Chercher ou créer un utilisateur de test
        Optional<User> existingUser = userRepository.findByEmail("test@example.com");

        if (existingUser.isEmpty()) {
            // Récupérer ou créer un rôle par défaut
            Role defaultRole = roleRepository.findByNom(ERole.ADMIN).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setNom(ERole.ADMIN);
                return roleRepository.save(newRole);
            });

            // Créer un nouvel utilisateur de test
            testUser = User.builder().nom("Test").prenom("Utilisateur").email("test@example.com").password(passwordEncoder.encode("password123")).role(defaultRole).build();

            testUser = userRepository.save(testUser);
        } else {
            testUser = existingUser.get();
        }

        // Configurer l'authentification
        authentication = new UsernamePasswordAuthenticationToken(testUser, testUser.getPassword(), testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testSaveProject_Success() throws Exception {
        // Créer une requête de projet
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setName("Nouveau Projet Test");
        projectRequest.setDescription("Description du projet de test");
        projectRequest.setStartDate(LocalDate.now());

        // Exécuter la requête de création de projet
        MvcResult result = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(projectRequest)).principal(authentication)).andExpect(status().isOk()).andReturn();

        // Vérifier l'ID du projet créé
        Integer projectId = objectMapper.readValue(result.getResponse().getContentAsString(), Integer.class);
        assertNotNull(projectId);

        // Vérifier que le projet a été sauvegardé en base de données
        Project savedProject = projectRepository.findById(projectId).orElseThrow(() -> new AssertionError("Le projet n'a pas été sauvegardé"));

        assertEquals(projectRequest.getName(), savedProject.getName());
        assertEquals(testUser, savedProject.getOwner());
    }

    @Test
    public void testSaveProject_WhenValidationFails() throws Exception {
        // Créer une requête de projet avec des données invalides
        ProjectRequest invalidRequest = new ProjectRequest();
        invalidRequest.setName(""); // Nom vide
        invalidRequest.setDescription("Description du projet");
        invalidRequest.setStartDate(LocalDate.now());

        // Exécuter la requête
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .principal(authentication))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindProjectById_Success() throws Exception {
        // Créer un projet de test
        Project testProject = createTestProject();

        // Exécuter la requête de recherche de projet
        mockMvc.perform(get("/api/projects/" + testProject.getId()).principal(authentication)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(testProject.getId())).andExpect(jsonPath("$.name").value(testProject.getName()));
    }

    @Test
    public void testFindAllProjects_Success() throws Exception {
        // Créer quelques projets de test
        createMultipleTestProjects();

        // Exécuter la requête de recherche de tous les projets
        MvcResult result = mockMvc.perform(get("/api/projects/all-Projects").param("page", "0").param("size", "10").principal(authentication)).andExpect(status().isOk()).andReturn();

        PageResponse<ProjectResponse> pageResponse = objectMapper.readValue(result.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructParametricType(PageResponse.class, ProjectResponse.class));

        assertNotNull(pageResponse);
        assertTrue(pageResponse.getContent().size() > 0);
    }

    @Test
    public void testDeleteProject_Success() throws Exception {
        // Créer un projet de test
        Project testProject = createTestProject();

        // Exécuter la requête de suppression de projet
        mockMvc.perform(delete("/api/projects/" + testProject.getId()).principal(authentication)).andExpect(status().isOk()).andExpect(content().string("Le projet a été supprimé avec succès."));

        // Vérifier que le projet a été supprimé
        assertFalse(projectRepository.existsById(testProject.getId()));
    }

    @Test
    public void testInviteMemberToProject_Success() throws Exception {
        // Créer un projet de test
        Project testProject = createTestProject();

        // Créer une requête d'invitation
        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        inviteRequest.setEmail(testUser.getEmail());

        // Exécuter la requête d'invitation
        mockMvc.perform(post("/api/projects/" + testProject.getId() + "/invite").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(inviteRequest)).principal(authentication)).andExpect(status().isOk()).andExpect(jsonPath("$.members").exists());
    }


    // Méthode utilitaire pour créer un projet de test
    private Project createTestProject() {
        Project project = new Project();
        project.setName("Projet Test");
        project.setDescription("Description du projet de test");
        project.setStartDate(LocalDate.now());
        project.setOwner(testUser);
        return projectRepository.save(project);
    }

    // Méthode utilitaire pour créer plusieurs projets de test
    private void createMultipleTestProjects() {
        for (int i = 0; i < 5; i++) {
            Project project = new Project();
            project.setName("Projet Test " + i);
            project.setDescription("Description du projet de test " + i);
            project.setStartDate(LocalDate.now());
            project.setOwner(testUser);
            projectRepository.save(project);
        }
    }
}
