package it.unicam.cs.awmc.webjtime;

import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import it.unicam.cs.awmc.webjtime.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserServiceTest {
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_shouldPersistUser() {
        userService.register("Mario", "password123");
        assertThat(userRepository.findByUsername("Mario")).isPresent();
    }

    @Test
    void register_passwordShouldBeEncoded() {
        userService.register("Mario", "password123");
        String encoded = userRepository.findByUsername("Mario").orElseThrow().getPassword();
        assertThat(encoded).isNotEqualTo("password123");
        assertThat(encoded).startsWith("$2a$");
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        userService.register("Mario", "password123");
        assertThatThrownBy(() -> userService.register("Mario", "password321"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void register_blankUsername_shouldThrow() {
        assertThatThrownBy(() -> userService.register("  ", "password123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_blankPassword_shouldThrow() {
        assertThatThrownBy(() -> userService.register("Mario", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


