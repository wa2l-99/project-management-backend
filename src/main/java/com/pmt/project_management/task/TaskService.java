package com.pmt.project_management.task;

import com.pmt.project_management.email.EmailService;
import com.pmt.project_management.history.TaskModifiedHistory;
import com.pmt.project_management.history.TaskModifiedHistoryRepository;
import com.pmt.project_management.project.Project;
import com.pmt.project_management.project.ProjectRepository;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;
    private final UserRepository userRepository;
    private final TaskModifiedHistoryRepository taskModifiedHistoryRepository;

    private final EmailService emailService;

    public TaskResponse createTask(Integer projectId, TaskRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        // Récupérer le projet par ID
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        // Log pour voir si l'utilisateur est bien dans les membres du projet
        System.out.println("Membres du projet : " + project.getMembers());
        System.out.println("Utilisateur connecté : " + user.getFullName());

        // Vérifier si l'utilisateur est membre ou administrateur du projet
        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre ou administrateur pour créer une tâche dans ce projet.");
        }

        // Mapper la requête en une entité Task
        Task task = taskMapper.toTaskRequest(request, project);

        // Sauvegarder la tâche dans la base de données
        taskRepository.save(task);

        // Retourner la tâche sous forme de TaskResponse
        return taskMapper.toTaskResponse(task);
    }


    public TaskResponse updateTask(Integer taskId, TaskRequest taskRequest, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        // Récupérer la tâche existante
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tâche non trouvée avec l'ID : " + taskId));

        // Copier l'ancienne version de la tâche pour comparaison
        Task oldTask = new Task();
        BeanUtils.copyProperties(existingTask, oldTask);

        // Appliquer les modifications à la tâche
        existingTask.setName(taskRequest.getName());
        existingTask.setDescription(taskRequest.getDescription());
        existingTask.setDueDate(taskRequest.getDueDate());
        existingTask.setPriority(taskRequest.getPriority());
        existingTask.setStatus(taskRequest.getStatus());

        // Enregistrer la tâche mise à jour
        taskRepository.save(existingTask);

        // Utiliser le mapper pour générer l'historique avec une description dynamique
        TaskHistoryResponse historyResponse = taskMapper.toTaskHistoryResponse(oldTask, existingTask);

        // Créer une entrée d'historique et la sauvegarder
        TaskModifiedHistory history = new TaskModifiedHistory();
        history.setTask(existingTask);
        history.setUser(user);
        history.setDescription(historyResponse.getModificationDescription());

        taskModifiedHistoryRepository.save(history);

        return taskMapper.toTaskResponse(existingTask);
    }

    public TaskResponse getTaskById(Integer taskId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tâche non trouvée avec l'ID : " + taskId));

        Project project = task.getProject();
        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre, administrateur ou observateur pour voir cette tâche.");
        }

        return taskMapper.toTaskResponse(task);
    }


    public void deleteTask(Integer taskId, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tâche non trouvée avec l'ID : " + taskId));

        Project project = task.getProject();
        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre ou administrateur pour supprimer cette tâche.");
        }
        taskRepository.delete(task);
    }


    public List<TaskResponse> getTasksByStatus(Integer projectId, EStatus status, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre, administrateur ou observateur pour voir les tâches.");
        }

        List<Task> tasks = taskRepository.findByProjectAndStatus(project, status);

        return tasks.stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getTasksByPriority(Integer projectId, EPriority priority, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre, administrateur ou observateur pour voir les tâches.");
        }

        List<Task> tasks = taskRepository.findByProjectAndPriority(project, priority);

        return tasks.stream()
                .map(taskMapper::toTaskResponse)
                .collect(Collectors.toList());
    }


    public List<TaskResponse> getAllTasksByProject(Integer projectId, Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'ID : " + projectId));

        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre, administrateur ou observateur pour voir les tâches.");
        }

        List<Task> tasks = taskRepository.findByProject(project);

        List<TaskResponse> taskResponses = tasks.stream()
                .map(taskMapper::toTaskResponse)
                .toList();

        return taskResponses;
    }

    // Méthode pour récupérer l'historique des tâches modifiées pour les projets de l'utilisateur connecté
// Méthode pour récupérer l'historique des tâches modifiées pour les projets de l'utilisateur connecté avec pagination
    public List<TaskHistoryResponse> getTaskModificationsForUserProjects(Authentication connectedUser, int page, int size) {
        User user = (User) connectedUser.getPrincipal();

        // Définir les paramètres de pagination
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        // Récupérer les projets auxquels l'utilisateur est membre, administrateur ou observateur, avec pagination
        Page<Project> userProjectsPage = projectRepository.findByMembersContaining(user, pageable);

        // Si aucun projet n'est trouvé, retourner une liste vide
        if (userProjectsPage.isEmpty()) {
            return Collections.emptyList();
        }

        // Récupérer toutes les tâches des projets paginés
        List<Task> tasks = userProjectsPage.getContent().stream()
                .flatMap(project -> project.getTasks().stream())
                .collect(Collectors.toList());

        // Si aucune tâche n'est associée aux projets de l'utilisateur, retourner une liste vide
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // Récupérer l'historique des modifications pour ces tâches
        List<TaskModifiedHistory> histories = taskModifiedHistoryRepository.findByTaskIn(tasks);

        // Si aucun historique n'est trouvé, retourner une liste vide
        if (histories.isEmpty()) {
            return Collections.emptyList();
        }

        // Utiliser le mapper pour transformer les historiques en TaskHistoryResponse
        return histories.stream()
                .map(this::mapToTaskHistoryResponse)
                .collect(Collectors.toList());
    }

    // Mapper pour transformer l'entité TaskModifiedHistory en TaskHistoryResponse
    private TaskHistoryResponse mapToTaskHistoryResponse(TaskModifiedHistory history) {
        return TaskHistoryResponse.builder()
                .taskId(history.getTask().getId())
                .taskName(history.getTask().getName())
                .projectName(history.getTask().getProject().getName())
                .lastModifiedById(history.getUser().getId())
                .lastModifiedByName(history.getUser().getFullName())
                .lastModifiedDate(history.getCreatedDate())
                .modificationDescription(history.getDescription())
                .build();
    }


    public TaskResponse assignTaskToMember(Integer taskId, Integer memberId, Authentication connectedUser) throws MessagingException {
        User user = (User) connectedUser.getPrincipal();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Tâche non trouvée avec l'ID : " + taskId));

        // Récupérer le projet auquel appartient la tâche
        Project project = task.getProject();

        // Vérifier si l'utilisateur connecté est membre ou propriétaire du projet
        if (!project.getMembers().contains(user) && !project.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("Vous devez être membre ou administrateur du projet pour assigner une tâche.");
        }

        // Rechercher le membre à qui la tâche va être assignée par ID
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'ID : " + memberId));

        // Vérifier que le membre fait partie du projet
        if (!project.getMembers().contains(member)) {
            throw new IllegalStateException("L'utilisateur n'est pas membre de ce projet.");
        }

        // Assigner la tâche au membre
        task.setAssignedTo(member);
        taskRepository.save(task);

        // Envoyer un e-mail de notification au membre
        emailService.sendTaskAssignmentEmail(member.getEmail(), member.getFullName(), task.getName(), project.getName(), "Affectation de tâche");

        // Retourner la réponse de la tâche mise à jour
        return taskMapper.toTaskResponse(task);
    }
}
