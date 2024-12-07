package com.pmt.project_management.controller;

import com.pmt.project_management.auth.*;
import com.pmt.project_management.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService service;

    @InjectMocks
    private AuthenticationController controller;

    private RegistrationRequest registrationRequest;
    private AuthenticationRequest authenticationRequest;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        registrationRequest = new RegistrationRequest(1, "john.doe@example.com", "password123", "John", "Doe");

        authenticationRequest = new AuthenticationRequest("john.doe@example.com", "password123");

        mockUserResponse = new UserResponse(1, "john.doe@example.com", "John", "Doe", "ADMIN");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(service.register(registrationRequest)).thenReturn(1);

        // Act
        ResponseEntity<Integer> response = controller.register(registrationRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody());
        verify(service).register(registrationRequest);
    }

    @Test
    void testAuthenticate_Success() {
        // Arrange
        AuthenticationResponse mockResponse = new AuthenticationResponse("token123", mockUserResponse);
        when(service.authenticate(authenticationRequest)).thenReturn(mockResponse);

        // Act
        ResponseEntity<AuthenticationResponse> response = controller.authenticate(authenticationRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("token123", response.getBody().getToken());
        assertEquals(mockUserResponse, response.getBody().getUser());
        verify(service).authenticate(authenticationRequest);
    }

    @Test
    void testFindAll_Success() {
        // Arrange
        List<UserResponse> mockUsers = Arrays.asList(new UserResponse(1, "john@example.com", "John", "Doe", "ADMIN"), new UserResponse(2, "jane@example.com", "Jane", "Smith", "ADMIN"));
        when(service.findAllUsers()).thenReturn(mockUsers);

        // Act
        ResponseEntity<List<UserResponse>> response = controller.findAll();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(service).findAllUsers();
    }

    @Test
    void testFindById_Success() {
        // Arrange
        UserResponse mockUser = new UserResponse(1, "john@example.com", "John", "Doe", "ADMIN");
        when(service.findById(1)).thenReturn(mockUser);

        // Act
        ResponseEntity<UserResponse> response = controller.findById(1);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getId());
        verify(service).findById(1);
    }

    @Test
    void testDelete_Success() {
        // Act
        ResponseEntity<Void> response = controller.delete(1);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(service).deleteUser(1);
    }

    @Test
    void testGetAllUserEmails_Success() {
        // Arrange
        List<String> mockEmails = Arrays.asList("john@example.com", "jane@example.com");
        when(service.getAllUserEmails()).thenReturn(mockEmails);

        // Act
        ResponseEntity<List<String>> response = controller.getAllUserEmails();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(service).getAllUserEmails();
    }
}