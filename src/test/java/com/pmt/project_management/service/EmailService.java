package com.pmt.project_management.service;

import com.pmt.project_management.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private MimeMessageHelper mimeMessageHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendTaskAssignmentEmail_ShouldSendEmailSuccessfully() throws MessagingException {
        // Arrange
        String to = "test@example.com";
        String username = "John";
        String taskName = "Test Task";
        String projectName = "Test Project";
        String subject = "Task Assignment";

        // Simulation du moteur de template pour retourner le contenu HTML attendu
        String expectedHtmlContent = "<html><body>Task Assignment Email</body></html>";
        when(templateEngine.process(eq("task-assignment"), any(Context.class))).thenReturn(expectedHtmlContent);

        // Act
        emailService.sendTaskAssignmentEmail(to, username, taskName, projectName, subject);

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);

        // Capturer le contexte passé au moteur de template
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("task-assignment"), contextCaptor.capture());
        Context context = contextCaptor.getValue();

        assertEquals(username, context.getVariable("username"));
        assertEquals(taskName, context.getVariable("taskName"));
        assertEquals(projectName, context.getVariable("projectName"));
    }

}
