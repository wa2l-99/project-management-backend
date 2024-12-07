package com.pmt.project_management.service;


import com.pmt.project_management.auth.AuthenticationRequest;
import com.pmt.project_management.auth.AuthenticationResponse;
import com.pmt.project_management.exception.AlreadyExistsException;
import com.pmt.project_management.exception.UserNotFoundException;
import com.pmt.project_management.auth.AuthenticationService;
import com.pmt.project_management.auth.RegistrationRequest;
import com.pmt.project_management.security.JwtService;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserMapper;
import com.pmt.project_management.user.UserRepository;
import com.pmt.project_management.user.UserResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest(1, "John", "Doe", "john.doe@example.com", "password");
        User user = User.builder().id(1) // Simulez un ID valide
                .nom(request.getNom()).prenom(request.getPrenom()).email(request.getEmail()).password("encodedPassword").build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        Integer result = authenticationService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(1);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowAlreadyExistsExceptionWhenEmailAlreadyExists() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest(1, "John", "Doe", "john.doe@example.com", "password");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password");
        User user = User.builder().email(request.getEmail()).password("password").build();

        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(user, null));
        when(jwtService.generateToken(anyMap(), eq(user))).thenReturn("jwtToken");
        when(userMapper.fromUser(user)).thenReturn(new UserResponse(user.getId(), user.getNom(), user.getPrenom(), user.getEmail(), null));

        // Act
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwtToken");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void shouldFindAllUsersSuccessfully() {
        // Arrange
        User user = User.builder().id(1).email("john.doe@example.com").build();
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        when(userMapper.fromUser(user)).thenReturn(new UserResponse(user.getId(), user.getNom(), user.getPrenom(), user.getEmail(), null));

        // Act
        List<UserResponse> users = authenticationService.findAllUsers();

        // Assert
        assertThat(users).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    void shouldThrowUserNotFoundExceptionForInvalidUserId() {
        // Arrange
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authenticationService.findById(1));
        verify(userRepository).findById(anyInt());
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        // Arrange
        User user = User.builder().id(1).email("john.doe@example.com").build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act
        authenticationService.deleteUser(1);

        // Assert
        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentUser() {
        // Arrange
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> authenticationService.deleteUser(1));
        verify(userRepository, never()).delete(any(User.class));
    }
}
