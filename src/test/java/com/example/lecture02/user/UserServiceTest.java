package com.example.lecture02.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService; // Adjust class name if different

    @Test
    void findOrCreateUser_WhenUserExists_ShouldReturnExistingUserWithoutSaving() {
        // Arrange
        String username = "existingUser";
        User existingUser = new User();
        existingUser.setUsername(username);
        existingUser.setRole("ROLE_READER");

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(existingUser));

        // Act
        User result = userService.findOrCreateUser(username);

        // Assert: Kills 'replaced return value with null'
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getRole()).isEqualTo("ROLE_READER");

        // Assert: Ensure no new user is created or saved when user exists
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void findOrCreateUser_WhenUserDoesNotExist_ShouldCreateSaveAndReturnNewUser() {
        // Arrange
        String username = "newUser";
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));

        // Act
        User result = userService.findOrCreateUser(username);

        // Assert 1: Verify the returned object (Kills return-null mutants)
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getRole()).isEqualTo("ROLE_REPORTER");
        assertThat(result.getPassword()).startsWith("encoded_");

        // Assert 2: Verify side effects with ArgumentCaptor (Kills 'removed call to save')
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getRole()).isEqualTo("ROLE_REPORTER");
        assertThat(savedUser.getPassword()).startsWith("encoded_");

        // Assert 3: Kills 'removed call to PasswordEncoder::encode'
        verify(passwordEncoder, times(1)).encode(anyString());
    }
}