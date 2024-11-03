package com.pmt.project_management.task;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Task")
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/projectId={projectId}/tasks")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER')")
    public ResponseEntity<TaskResponse> createTask(@PathVariable Integer projectId, @Valid @RequestBody TaskRequest request, Authentication authentication) {
        TaskResponse taskResponse = taskService.createTask(projectId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskResponse);
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Integer taskId, @Valid @RequestBody TaskRequest request, Authentication authentication) {
        TaskResponse taskResponse = taskService.updateTask(taskId, request, authentication);
        return ResponseEntity.ok(taskResponse);
    }

    @GetMapping("/projectId={projectId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER') or hasAuthority('OBSERVER')")
    public ResponseEntity<List<TaskResponse>> findAllTasksByProject(@PathVariable Integer projectId, Authentication authentication) {

        List<TaskResponse> taskResponses = taskService.getAllTasksByProject(projectId, authentication);
        return ResponseEntity.ok(taskResponses);
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER') or hasAuthority('OBSERVER')")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Integer taskId, Authentication authentication) {
        TaskResponse taskResponse = taskService.getTaskById(taskId, authentication);
        return ResponseEntity.ok(taskResponse);
    }


    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER')")
    public ResponseEntity<String> deleteTask(@PathVariable Integer taskId, Authentication authentication) {
        taskService.deleteTask(taskId, authentication);
        return ResponseEntity.ok("La tâche a été supprimé avec succès.");
    }

    @GetMapping("/projectId={projectId}/tasksByStatus")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER') or hasAuthority('OBSERVER')")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(@PathVariable Integer projectId, @RequestParam EStatus status, Authentication authentication) {
        List<TaskResponse> taskResponses = taskService.getTasksByStatus(projectId, status, authentication);
        return ResponseEntity.ok(taskResponses);
    }


    @GetMapping("/projectId={projectId}/tasksByPriority")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER') or hasAuthority('OBSERVER')")
    public ResponseEntity<List<TaskResponse>> getTasksByPriority(@PathVariable Integer projectId, @RequestParam EPriority priority, Authentication authentication) {
        List<TaskResponse> taskResponses = taskService.getTasksByPriority(projectId, priority, authentication);
        return ResponseEntity.ok(taskResponses);
    }

    @GetMapping("/my-projects/history")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER') or hasAuthority('OBSERVER')")
    public ResponseEntity<List<TaskHistoryResponse>> getTaskModificationsForUserProjects(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size) {

        // Appeler le service pour obtenir l'historique avec pagination
        List<TaskHistoryResponse> history = taskService.getTaskModificationsForUserProjects(authentication, page, size);

        // Retourner la liste des historiques
        return ResponseEntity.ok(history);
    }


    // Endpoint pour assigner une tâche à un membre
    @PostMapping("/{taskId}/assign")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MEMBER')")
    public ResponseEntity<TaskResponse> assignTaskToMember(
            @PathVariable Integer taskId,
            @RequestParam Integer memberId,
            Authentication authentication) throws MessagingException {
        TaskResponse updatedTask = taskService.assignTaskToMember(taskId, memberId, authentication);
        return ResponseEntity.ok(updatedTask);
    }
}
