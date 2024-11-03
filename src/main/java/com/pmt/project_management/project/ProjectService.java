package com.pmt.project_management.project;

import com.pmt.project_management.common.PageResponse;
import com.pmt.project_management.exception.AlreadyExistsException;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.role.RoleRepository;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserMapper;
import com.pmt.project_management.user.UserRepository;
import com.pmt.project_management.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.pmt.project_management.project.ProjectSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final UserMapper userMapper;

    public Integer save(ProjectRequest request, Authentication connectedUser) {
        // Récupérer l'utilisateur connecté
        User user = (User) connectedUser.getPrincipal();

        Project project = projectMapper.toProject(request);

        // Vérifier si un projet avec le même nom existe déjà
        if (projectRepository.existsByName(project.getName())) {
            throw new AlreadyExistsException("Le projet avec le nom " + project.getName() + " existe déjà");
        }

        // Vérifier si l'utilisateur n'a aucun rôle
        if (user.getRole() == null) {
            // Récupérer le rôle ADMIN depuis la base de données
            Role adminRole = roleRepository.findByNom(ERole.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Error: Role ADMIN is not found."));

            // Assigner le rôle ADMIN à l'utilisateur
            user.setRole(adminRole);
            userRepository.save(user);  // Persister la modification du rôle de l'utilisateur
        }
        // Si l'utilisateur a un rôle autre que ADMIN, il ne peut pas créer un projet
        else if (!user.getRole().getNom().equals(ERole.ADMIN)) {
            throw new IllegalStateException("Vous ne possédez pas les autorisations nécessaires pour créer un projet.");
        }

        // Définir l'utilisateur comme propriétaire du projet
        project.setOwner(user);

        // Ajouter l'utilisateur créateur comme membre du projet
        project.getMembers().add(user);

        projectRepository.save(project);

        return project.getId();
    }


    public ProjectResponse findById(Integer projectId) {
        return projectRepository.findById(projectId).map(projectMapper::toProjectResponse).orElseThrow(() -> new EntityNotFoundException("Aucun projet trouvé avec l'id: " + projectId));
    }

    public PageResponse findAllProjects(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<Project> projects = projectRepository.findAll(pageable);

        List<ProjectResponse> projectResponses = projects.stream().map(projectMapper::toProjectResponse).toList();

        return new PageResponse<>(projectResponses, projects.getNumber(), projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isFirst(), projects.isLast());

    }

    public PageResponse<ProjectResponse> findAllProjectsByOwner(int page, int size, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<Project> projects = projectRepository.findAll(withOwnerId(user.getId()), pageable);

        List<ProjectResponse> projectResponses = projects.stream().map(projectMapper::toProjectResponse).toList();

        return new PageResponse<>(projectResponses, projects.getNumber(), projects.getSize(), projects.getTotalElements(), projects.getTotalPages(), projects.isFirst(), projects.isLast());
    }

    public void deleteProject(Integer projectId) {
        Project existingProject = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));
        projectRepository.delete(existingProject);
    }

    public ProjectResponse inviteMemberToProject(Integer projectId, InviteMemberRequest request, Authentication connectedUser) {
        // Récupérer l'utilisateur connecté (administrateur)
        User adminUser = (User) connectedUser.getPrincipal();

        // Récupérer le projet par ID
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        // Vérifier si l'utilisateur connecté est l'administrateur (propriétaire du projet)
        if (!project.getOwner().getId().equals(adminUser.getId())) {
            throw new IllegalStateException("Vous devez être administrateur du projet pour inviter des membres.");
        }

        // Rechercher l'utilisateur à inviter par e-mail
        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'email : " + request.getEmail()));

        // Vérifier si l'utilisateur est déjà membre du projet
        if (project.getMembers().contains(invitedUser)) {
            throw new IllegalStateException("L'utilisateur est déjà membre de ce projet.");
        }

        // Ajouter l'utilisateur à la liste des membres du projet sans rôle
        project.getMembers().add(invitedUser);

        // Sauvegarder les modifications dans la base de données
        projectRepository.save(project);   // Sauvegarder le projet avec le nouveau membre

        return projectMapper.toProjectResponse(project);
    }


    public String assignRoleToMember(Integer projectId, AssignRoleRequest request, Authentication connectedUser) {
        User adminUser = (User) connectedUser.getPrincipal();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        if (!project.getOwner().getId().equals(adminUser.getId())) {
            throw new IllegalStateException("Vous devez être administrateur du projet pour attribuer des rôles.");
        }

        User member = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'email : " + request.getEmail()));

        // Vérifier si l'utilisateur est membre du projet
        if (!project.getMembers().contains(member)) {
            throw new IllegalStateException("L'utilisateur n'est pas membre de ce projet.");
        }

        // Récupérer le rôle à attribuer depuis la base de données
        Role role = roleRepository.findByNom(ERole.valueOf(request.getRole().toUpperCase()))
                .orElseThrow(() -> new IllegalStateException("Rôle non trouvé : " + request.getRole()));

        // Ajouter le rôle à l'utilisateur
        member.setRole(role);

        userRepository.save(member);  // Sauvegarder l'utilisateur avec son nouveau rôle
        return role.getNom().name();
    }


    public String updateMemberRole(Integer projectId, AssignRoleRequest request, Authentication connectedUser) {
        User adminUser = (User) connectedUser.getPrincipal();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        if (!project.getOwner().getId().equals(adminUser.getId())) {
            throw new IllegalStateException("Vous devez être administrateur du projet pour modifier les rôles.");
        }

        User member = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'email : " + request.getEmail()));

        if (!project.getMembers().contains(member)) {
            throw new IllegalStateException("L'utilisateur n'est pas membre de ce projet.");
        }

        Role newRole = roleRepository.findByNom(ERole.valueOf(request.getRole().toUpperCase()))
                .orElseThrow(() -> new IllegalStateException("Rôle non trouvé : " + request.getRole()));

        // Ajouter le nouveau rôle
        member.setRole(newRole);

        userRepository.save(member);
        return newRole.getNom().name();
    }


    public UserResponse getMemberDetails(Integer projectId, String email) {

        // Récupérer le projet par ID
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        // Rechercher le membre par e-mail
        User member = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'email : " + email));

        // Vérifier si le membre appartient au projet
        if (!project.getMembers().contains(member)) {
            throw new IllegalStateException("L'utilisateur n'est pas membre de ce projet.");
        }

        return userMapper.fromUser(member);
    }

    // Récupérer tous les projets où l'utilisateur est membre
    public PageResponse<ProjectResponse> getProjectsForUser(int page, int size, User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Project> projects = projectRepository.findByMembersContaining(user, pageable);

        List<ProjectResponse> projectResponse = projects.stream()
                .map(projectMapper::toProjectResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                projectResponse,
                projects.getNumber(),
                projects.getSize(),
                projects.getTotalElements(),
                projects.getTotalPages(),
                projects.isFirst(),
                projects.isLast()
        );

    }


    // Récupérer les membres d'un projet
    public List<UserResponse> getProjectMembers(Integer projectId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        // Récupérer le projet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        // Vérifier si l'utilisateur est membre ou administrateur du projet
        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre ou administrateur pour voir les membres.");
        }

        return project.getMembers().stream()
                .map(userMapper::fromUser)
                .collect(Collectors.toList());
    }

}
