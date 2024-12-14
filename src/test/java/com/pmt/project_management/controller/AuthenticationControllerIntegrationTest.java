package com.pmt.project_management.controller;

import com.pmt.project_management.auth.AuthenticationRequest;
import com.pmt.project_management.auth.AuthenticationResponse;
import com.pmt.project_management.auth.AuthenticationService;
import com.pmt.project_management.auth.RegistrationRequest;
import com.pmt.project_management.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void testRegister() throws Exception {
        RegistrationRequest registrationRequest = new RegistrationRequest(
                1,
                "John",
                "Doe",
                "john.doe@example.com",
                "password123"

        );

        when(authenticationService.register(any(RegistrationRequest.class))).thenReturn(1);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void testAuthenticate() throws Exception {
        AuthenticationRequest authRequest = new AuthenticationRequest(
                "john.doe@example.com",
                "password123"
        );

        UserResponse userResponse = new UserResponse(1, "Doe", "John", "john.doe@example.com","ADMIN");
        AuthenticationResponse authResponse = new AuthenticationResponse("token123", userResponse);

        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("john.doe@example.com"));
    }

    @Test
    void testFindAll() throws Exception {
        List<UserResponse> users = Arrays.asList(
                new UserResponse(1, "Doe", "John", "john@example.com","ADMIN"),
                new UserResponse(2, "Smith", "Jane", "jane@example.com","MEMBER")
        );

        when(authenticationService.findAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/auth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void testFindById() throws Exception {
        UserResponse user = new UserResponse(1, "Doe", "John", "john@example.com","ADMIN");

        when(authenticationService.findById(1)).thenReturn(user);

        mockMvc.perform(get("/api/auth/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void testGetAllUserEmails() throws Exception {
        List<String> emails = Arrays.asList(
                "john@example.com",
                "jane@example.com"
        );

        when(authenticationService.getAllUserEmails()).thenReturn(emails);

        mockMvc.perform(get("/api/auth/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("john@example.com"))
                .andExpect(jsonPath("$[1]").value("jane@example.com"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/auth/1"))
                .andExpect(status().isAccepted());
    }
}