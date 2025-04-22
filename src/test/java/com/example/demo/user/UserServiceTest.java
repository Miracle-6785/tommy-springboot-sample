package com.example.demo.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private UserDto.CreateUserRequest createRequest;
    private UserDto.UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        user1 = new User(1L, "John Doe", "john@example.com");
        user2 = new User(2L, "Jane Doe", "jane@example.com");
        createRequest = new UserDto.CreateUserRequest("New User", "new@example.com");
        updateRequest = new UserDto.UpdateUserRequest("Updated User", "updated@example.com");
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<UserDto.UserResponse> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        assertEquals(user1.getId(), result.get(0).getId());
        assertEquals(user1.getName(), result.get(0).getName());
        assertEquals(user1.getEmail(), result.get(0).getEmail());
        assertEquals(user2.getId(), result.get(1).getId());
        assertEquals(user2.getName(), result.get(1).getName());
        assertEquals(user2.getEmail(), result.get(1).getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // When
        UserDto.UserResponse result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(user1.getId(), result.getId());
        assertEquals(user1.getName(), result.getName());
        assertEquals(user1.getEmail(), result.getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldThrowException() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(99L));
        verify(userRepository).findById(99L);
    }

    @Test
    void createUser_whenEmailNotInUse_shouldCreateUser() {
        // Given
        User newUser = new User();
        newUser.setName(createRequest.getName());
        newUser.setEmail(createRequest.getEmail());

        User savedUser = new User(3L, createRequest.getName(), createRequest.getEmail());

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserDto.UserResponse result = userService.createUser(createRequest);

        // Then
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(savedUser.getName(), result.getName());
        assertEquals(savedUser.getEmail(), result.getEmail());
        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_whenEmailInUse_shouldThrowException() {
        // Given
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> userService.createUser(createRequest));
        assertTrue(exception.getMessage().contains("Email already in use"));
        verify(userRepository).existsByEmail(createRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenUserExistsAndEmailNotInUse_shouldUpdateUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(
                new User(1L, updateRequest.getName(), updateRequest.getEmail()));

        // When
        UserDto.UserResponse result = userService.updateUser(1L, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(updateRequest.getName(), result.getName());
        assertEquals(updateRequest.getEmail(), result.getEmail());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_whenUserDoesNotExist_shouldThrowException() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> userService.updateUser(99L, updateRequest));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_whenUserExists_shouldDeleteUser() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_whenUserDoesNotExist_shouldThrowException() {
        // Given
        when(userRepository.existsById(99L)).thenReturn(false);

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository).existsById(99L);
        verify(userRepository, never()).deleteById(any());
    }
} 