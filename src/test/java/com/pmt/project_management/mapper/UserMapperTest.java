package com.pmt.project_management.mapper;

import com.pmt.project_management.auth.RegistrationRequest;
import com.pmt.project_management.role.ERole;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.user.User;
import com.pmt.project_management.user.UserMapper;
import com.pmt.project_management.user.UserResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUser_WhenRequestIsValid_ShouldMapCorrectly() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setId(1);
        request.setNom("Doe");
        request.setPrenom("John");
        request.setEmail("john.doe@example.com");

        // Act
        User user = userMapper.toUser(request);

        // Assert
        assertNotNull(user);
        assertEquals(1, user.getId());
        assertEquals("Doe", user.getNom());
        assertEquals("John", user.getPrenom());
        assertEquals("john.doe@example.com", user.getEmail());
    }

    @Test
    void toUser_WhenRequestIsNull_ShouldReturnNull() {
        // Act
        User user = userMapper.toUser(null);

        // Assert
        assertNull(user);
    }

    // Test for fromUser
    @Test
    void fromUser_WhenUserIsValid_ShouldMapCorrectly() {
        // Arrange
        Role role = new Role();
        role.setNom(ERole.ADMIN);

        User user = User.builder()
                .id(1)
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .role(role)
                .build();

        // Act
        UserResponse response = userMapper.fromUser(user);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Doe", response.getNom());
        assertEquals("John", response.getPrenom());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void fromUser_WhenUserIsNull_ShouldReturnNull() {
        // Act
        UserResponse response = userMapper.fromUser(null);

        // Assert
        assertNull(response);
    }

    @Test
    void fromUser_WhenUserHasNoRole_ShouldAssignNoRole() {
        // Arrange
        User user = User.builder()
                .id(1)
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .role(null) // No role assigned
                .build();

        // Act
        UserResponse response = userMapper.fromUser(user);

        // Assert
        assertNotNull(response);
        assertEquals("No role", response.getRole());
    }
}