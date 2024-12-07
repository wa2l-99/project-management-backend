package com.pmt.project_management.service;


import com.pmt.project_management.common.PageResponse;
import com.pmt.project_management.exception.AlreadyExistsException;
import com.pmt.project_management.project.*;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.role.RoleRepository;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserMapper;
import com.pmt.project_management.user.UserRepository;
import com.pmt.project_management.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjectService projectService;

    private User adminUser;
    private Project project;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setNom(ERole.ADMIN);

        adminUser = User.builder()
                .id(1)
                .email("admin@example.com")
                .role(adminRole)
                .build();

        project = Project.builder()
                .id(1)
                .name("Test Project")
                .owner(adminUser)
                .members(new HashSet<>())
                .build();
    }

    @Test
    void save_WhenProjectDoesNotExist_ShouldCreateProject() {
        // Arrange
        ProjectRequest request = new ProjectRequest();
        request.setName("New Project");

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.existsByName(anyString())).thenReturn(false);
        when(projectMapper.toProject(request)).thenReturn(project);
        when(authentication.getPrincipal()).thenReturn(adminUser);

        // Act
        Integer projectId = projectService.save(request, authentication);

        // Assert
        assertEquals(1, projectId);
        verify(projectRepository).save(project);
    }

    @Test
    void save_WhenProjectAlreadyExists_ShouldThrowException() {
        // Arrange
        ProjectRequest request = new ProjectRequest();
        request.setName("Existing Project");

        User mockAdminUser = User.builder()
                .id(1)
                .email("admin@example.com")
                .role(adminRole)
                .build();

        when(authentication.getPrincipal()).thenReturn(mockAdminUser);

        Project mockProject = new Project();
        mockProject.setName("Existing Project");
        mockProject.setOwner(mockAdminUser);
        mockProject.setMembers(new HashSet<>());

        when(projectMapper.toProject(request)).thenReturn(mockProject);

        when(projectRepository.existsByName(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () ->
                projectService.save(request, authentication)
        );
    }

    @Test
    void save_WhenUserHasNoRole_ShouldAssignAdminRole() {
        // Arrange
        User userWithoutRole = User.builder()
                .id(1)
                .email("user@example.com")
                .build();

        when(authentication.getPrincipal()).thenReturn(userWithoutRole);
        when(roleRepository.findByNom(ERole.ADMIN)).thenReturn(Optional.of(adminRole));
        when(projectMapper.toProject(any(ProjectRequest.class))).thenReturn(project);
        when(projectRepository.existsByName(anyString())).thenReturn(false);

        // Act
        Integer projectId = projectService.save(new ProjectRequest(), authentication);

        // Assert
        assertNotNull(projectId);
        assertEquals(adminRole, userWithoutRole.getRole());
        verify(userRepository).save(userWithoutRole); // Vérifie que le rôle a été persisté
    }

    @Test
    void findById_WhenProjectExists_ShouldReturnProjectResponse() {
        // Arrange
        ProjectResponse expectedResponse = new ProjectResponse();
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectResponse(project)).thenReturn(expectedResponse);

        // Act
        ProjectResponse response = projectService.findById(1);

        // Assert
        assertEquals(expectedResponse, response);
    }

    @Test
    void findById_WhenProjectNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange
        when(projectRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                projectService.findById(1)
        );
    }

    @Test
    void findAllProjects_ShouldReturnPagedProjects() {
        // Arrange
        Page<Project> projectPage = new PageImpl<>(Collections.singletonList(project));
        ProjectResponse projectResponse = new ProjectResponse();

        when(projectRepository.findAll(any(Pageable.class))).thenReturn(projectPage);
        when(projectMapper.toProjectResponse(project)).thenReturn(projectResponse);

        // Act
        PageResponse result = projectService.findAllProjects(0, 10);

        // Assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findAllProjects_WhenUserIsUnauthenticated_ShouldThrowUnauthorized() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                projectService.findAllProjectsByOwner(0, 10, authentication)
        );
    }

    @Test
    void findAllProjects_WhenNoProjectsExist_ShouldReturnEmptyList() {
        // Arrange
        Page<Project> emptyPage = new PageImpl<>(Collections.emptyList());
        when(projectRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResponse<ProjectResponse> response = projectService.findAllProjects(0, 10);

        // Assert
        assertTrue(response.getContent().isEmpty());
    }


    @Test
    void inviteMemberToProject_ShouldAddMemberToProject() {
        // Arrange
        User invitedUser = User.builder()
                .id(2)
                .email("invited@example.com")
                .build();

        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        inviteRequest.setEmail("invited@example.com");

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("invited@example.com")).thenReturn(Optional.of(invitedUser));

        // Act
        ProjectResponse response = projectService.inviteMemberToProject(1, inviteRequest, authentication);

        // Assert
        assertTrue(project.getMembers().contains(invitedUser));
        verify(projectRepository).save(project);
    }

    @Test
    void inviteMemberToProject_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        inviteRequest.setEmail("nonexistent@example.com");

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                projectService.inviteMemberToProject(1, inviteRequest, authentication)
        );
    }

    @Test
    void inviteMemberToProject_WhenUserIsNotOwner_ShouldThrowException() {
        // Arrange
        User nonOwnerUser = User.builder()
                .id(2)
                .email("nonowner@example.com")
                .build();

        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        inviteRequest.setEmail("invited@example.com");

        when(authentication.getPrincipal()).thenReturn(nonOwnerUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                projectService.inviteMemberToProject(1, inviteRequest, authentication)
        );
    }

    @Test
    void inviteMemberToProject_WhenUserAlreadyMember_ShouldThrowException() {
        // Arrange
        User existingMember = User.builder()
                .id(2)
                .email("member@example.com")
                .build();

        InviteMemberRequest inviteRequest = new InviteMemberRequest();
        inviteRequest.setEmail("member@example.com");

        project.getMembers().add(existingMember);

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(existingMember));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                projectService.inviteMemberToProject(1, inviteRequest, authentication)
        );

        assertEquals("L'utilisateur est déjà membre de ce projet.", exception.getMessage());
    }


    @Test
    void assignRoleToMember_ShouldAssignRoleToMember() {
        // Arrange
        User memberUser = User.builder()
                .id(2)
                .email("member@example.com")
                .build();

        Role memberRole = new Role();
        memberRole.setNom(ERole.MEMBER);

        AssignRoleRequest roleRequest = new AssignRoleRequest();
        roleRequest.setEmail("member@example.com");
        roleRequest.setRole("MEMBER");

        project.getMembers().add(memberUser);

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(memberUser));
        when(roleRepository.findByNom(ERole.MEMBER)).thenReturn(Optional.of(memberRole));

        // Act
        String assignedRole = projectService.assignRoleToMember(1, roleRequest, authentication);

        // Assert
        assertEquals("MEMBER", assignedRole);
        assertEquals(memberRole, memberUser.getRole());
        verify(userRepository).save(memberUser);
    }

    @Test
    void assignRoleToMember_WhenUserIsNotAdmin_ShouldReturn403() {
        // Arrange
        Role observerRole = new Role();
        observerRole.setNom(ERole.OBSERVER);

        User observerUser = User.builder()
                .id(2)
                .email("observer@example.com")
                .role(observerRole)
                .build();

        when(authentication.getPrincipal()).thenReturn(observerUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                projectService.assignRoleToMember(1, new AssignRoleRequest(), authentication)
        );
    }

    @Test
    void assignRoleToMember_WhenUserNotMember_ShouldThrowException() {
        // Arrange
        AssignRoleRequest roleRequest = new AssignRoleRequest();
        roleRequest.setEmail("nonmember@example.com");
        roleRequest.setRole("MEMBER");

        User nonMemberUser = User.builder()
                .id(2)
                .email("nonmember@example.com")
                .build();

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("nonmember@example.com")).thenReturn(Optional.of(nonMemberUser));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                projectService.assignRoleToMember(1, roleRequest, authentication)
        );

        assertEquals("L'utilisateur n'est pas membre de ce projet.", exception.getMessage());
    }

    @Test
    void getProjectMembers_ShouldReturnListOfMembers() {
        // Arrange
        User memberUser = User.builder()
                .id(2)
                .email("member@example.com")
                .build();

        project.getMembers().add(memberUser);
        project.getMembers().add(adminUser);

        UserResponse userResponse = new UserResponse();

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(userMapper.fromUser(any(User.class))).thenReturn(userResponse);

        // Act
        List<UserResponse> members = projectService.getProjectMembers(1, authentication);

        // Assert
        assertEquals(2, members.size());
    }

    @Test
    void getProjectMembers_WhenUserIsNotOwnerOrMember_ShouldThrowException() {
        // Arrange
        User nonMemberUser = User.builder()
                .id(3)
                .email("nonmember@example.com")
                .build();

        when(authentication.getPrincipal()).thenReturn(nonMemberUser);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                projectService.getProjectMembers(1, authentication)
        );
    }
    @Test
    void getProjectsForUser_WhenNoProjectsExist_ShouldReturnEmptyResponse() {
        // Arrange
        Page<Project> emptyPage = new PageImpl<>(Collections.emptyList());
        when(projectRepository.findByMembersContaining(any(), any(Pageable.class))).thenReturn(emptyPage);

        // Act
        PageResponse<ProjectResponse> response = projectService.getProjectsForUser(0, 10, adminUser);

        // Assert
        assertTrue(response.getContent().isEmpty());
    }

}