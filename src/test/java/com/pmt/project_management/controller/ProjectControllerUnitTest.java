package com.pmt.project_management.controller;


import com.pmt.project_management.common.PageResponse;
import com.pmt.project_management.project.*;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.xml.bind.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerUnitTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjectController projectController;

    private ProjectRequest projectRequest;
    private User connectedUser;

    private Validator validator;

    @BeforeEach
    void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        projectRequest = new ProjectRequest();
        projectRequest.setName("Test Project");

        connectedUser = User.builder()
                .id(1)
                .email("test@example.com")
                .build();
    }

    @Test
    void saveProject_ShouldReturnProjectId() {
        // Arrange
        when(projectService.save(projectRequest, authentication)).thenReturn(1);

        // Act
        ResponseEntity<Integer> response = projectController.saveProject(projectRequest, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody());
        verify(projectService).save(projectRequest, authentication);
    }

    @Test
    void saveProject_WhenProjectNameIsEmpty_ShouldThrowValidationException() {
        // Arrange
        ProjectRequest invalidRequest = new ProjectRequest();
        invalidRequest.setName(""); // Nom vide
        invalidRequest.setDescription("Description");
        invalidRequest.setStartDate(LocalDate.now());

        // Act
        var violations = validator.validate(invalidRequest);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Le nom du projet ne doit pas être vide")));
    }

    @Test
    void saveProject_WhenStartDateIsNull_ShouldThrowValidationException() {
        // Arrange
        ProjectRequest invalidRequest = new ProjectRequest();
        invalidRequest.setName("Valid Name");
        invalidRequest.setDescription("Valid Description");
        invalidRequest.setStartDate(null); // Date de début nulle

        // Act
        Set<ConstraintViolation<ProjectRequest>> violations = validator.validate(invalidRequest);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().equals("La date de début ne doit pas être nulle")));
    }

    @Test
    void findProjectById_ShouldReturnProjectResponse() {
        // Arrange
        ProjectResponse projectResponse = new ProjectResponse();
        projectResponse.setId(1);
        when(projectService.findById(1)).thenReturn(projectResponse);

        // Act
        ResponseEntity<ProjectResponse> response = projectController.findProjectById(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(projectResponse, response.getBody());
    }

    @Test
    void findProjectById_WhenProjectNotFound_ShouldThrowException() {
        // Arrange
        when(projectService.findById(99)).thenThrow(new EntityNotFoundException("Projet introuvable"));

        // Act & Assert
        Exception exception = assertThrows(EntityNotFoundException.class, () -> {
            projectController.findProjectById(99);
        });
        assertEquals("Projet introuvable", exception.getMessage());
        verify(projectService).findById(99);
    }

    @Test
    void findAllProjects_ShouldReturnPagedProjects() {
        // Arrange
        PageResponse<ProjectResponse> pagedProjects = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
        when(projectService.findAllProjects(0, 10)).thenReturn(pagedProjects);

        // Act
        ResponseEntity<PageResponse<ProjectResponse>> response = projectController.findAllProjects(0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pagedProjects, response.getBody());
    }

    @Test
    void findAllProjectsByOwner_ShouldReturnPagedProjects() {
        // Arrange
        PageResponse<ProjectResponse> pagedProjects = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
        when(projectService.findAllProjectsByOwner(0, 10, authentication)).thenReturn(pagedProjects);

        // Act
        ResponseEntity<PageResponse<ProjectResponse>> response = projectController.findAllProjectsByOwner(0, 10, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pagedProjects, response.getBody());
    }

    @Test
    void deleteProject_ShouldReturnSuccessMessage() {
        // Act
        ResponseEntity<String> response = projectController.deleteProject(1);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Le projet a été supprimé avec succès.", response.getBody());
        verify(projectService).deleteProject(1);
    }

    @Test
    void deleteProject_WhenProjectNotFound_ShouldThrowException() {
        // Arrange
        doThrow(new EntityNotFoundException("Projet introuvable")).when(projectService).deleteProject(99);

        // Act & Assert
        Exception exception = assertThrows(EntityNotFoundException.class, () -> {
            projectController.deleteProject(99);
        });
        assertEquals("Projet introuvable", exception.getMessage());
        verify(projectService).deleteProject(99);
    }


    @Test
    void inviteMemberToProject_ShouldReturnUpdatedProject() {
        // Arrange
        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        ProjectResponse updatedProject = new ProjectResponse();
        when(projectService.inviteMemberToProject(1, inviteRequest, authentication)).thenReturn(updatedProject);

        // Act
        ResponseEntity<ProjectResponse> response = projectController.inviteMemberToProject(1, inviteRequest, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedProject, response.getBody());
    }

    @Test
    void assignRoleToMember_ShouldReturnSuccessMessage() {
        // Arrange
        AssignRoleRequest roleRequest = new AssignRoleRequest();
        when(projectService.assignRoleToMember(1, roleRequest, authentication)).thenReturn("ADMIN");

        // Act
        ResponseEntity<String> response = projectController.assignRoleToMember(1, roleRequest, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rôle : ADMIN attribué avec succès !", response.getBody());
    }

    @Test
    void assignRoleToMember_WhenUserIsNotAdmin_ShouldThrowAccessDeniedException() {
        // Arrange
        AssignRoleRequest roleRequest = new AssignRoleRequest();
        doThrow(new AccessDeniedException("Accès refusé")).when(projectService).assignRoleToMember(anyInt(), any(), any());

        // Act & Assert
        Exception exception = assertThrows(AccessDeniedException.class, () -> {
            projectController.assignRoleToMember(1, roleRequest, authentication);
        });
        assertEquals("Accès refusé", exception.getMessage());
        verify(projectService).assignRoleToMember(anyInt(), any(), any());
    }


    @Test
    void updateMemberRole_ShouldReturnSuccessMessage() {
        // Arrange
        AssignRoleRequest roleRequest = new AssignRoleRequest();
        when(projectService.updateMemberRole(1, roleRequest, authentication)).thenReturn("MEMBER");

        // Act
        ResponseEntity<String> response = projectController.updateMemberRole(1, roleRequest, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Le rôle a été mis à jour : MEMBER", response.getBody());
    }

    @Test
    void getMemberDetails_ShouldReturnUserResponse() {
        // Arrange
        UserResponse userResponse = new UserResponse();
        when(projectService.getMemberDetails(1, "test@example.com")).thenReturn(userResponse);

        // Act
        ResponseEntity<UserResponse> response = projectController.getMemberDetails(1, "test@example.com");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }

    @Test
    void getMyProjects_WhenProjectsExist_ShouldReturnProjects() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        PageResponse<ProjectResponse> projects = new PageResponse<>(
                List.of(new ProjectResponse(), new ProjectResponse()), 2, 10, 1, 0, false, false
        );
        when(projectService.getProjectsForUser(0, 10, connectedUser)).thenReturn(projects);

        // Act
        ResponseEntity<PageResponse<ProjectResponse>> response = projectController.getMyProjects(authentication, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
    }

    @Test
    void getMyProjects_WhenNoProjectsExist_ShouldReturnEmptyPageResponse() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(connectedUser);
        when(projectService.getProjectsForUser(0, 10, connectedUser)).thenReturn(null);

        // Act
        ResponseEntity<PageResponse<ProjectResponse>> response = projectController.getMyProjects(authentication, 0, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getContent().isEmpty());
    }

    @Test
    void getProjectMembers_ShouldReturnListOfMembers() {
        // Arrange
        List<UserResponse> members = List.of(new UserResponse(), new UserResponse());
        when(projectService.getProjectMembers(1, authentication)).thenReturn(members);

        // Act
        ResponseEntity<List<UserResponse>> response = projectController.getProjectMembers(1, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}