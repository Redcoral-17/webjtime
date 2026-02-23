package it.unicam.cs.awmc.webjtime;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import it.unicam.cs.awmc.webjtime.service.ProjectService;
import it.unicam.cs.awmc.webjtime.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ProjectServiceTest {
    @Autowired ProjectService projectService;
    @Autowired TaskService taskService;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired UserRepository userRepository;
    private User user;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User("tester", "password"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("tester", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void createProject_shouldPersistProject() {
        projectService.createProject("Progetto Test");
        assertThat(projectRepository.findByUser(user)).hasSize(1);
        assertThat(projectRepository.findByUser(user).getFirst().getName()).isEqualTo("Progetto Test");
    }

    @Test
    void createProject_blankName_shouldThrow() {
        assertThatThrownBy(() -> projectService.createProject("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteProject_withNoTasks_shouldRemoveProject() {
        projectService.createProject("Da Eliminare");
        Project project = projectRepository.findByUser(user).getFirst();
        projectService.deleteProject(project);
        assertThat(projectRepository.findByUser(user)).isEmpty();
    }

    @Test
    void deleteProject_withTasks_shouldThrow() {
        projectService.createProject("Con Task");
        Project project = projectRepository.findByUser(user).getFirst();
        taskRepository.save(new Task("task", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), project, user));
        assertThatThrownBy(() -> projectService.deleteProject(project)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeProject_withAllCompletedTasks_shouldSetStatusCompleted() {
        projectService.createProject("Completabile");
        Project project = projectRepository.findByUser(user).getFirst();
        Task task = taskRepository.save(new Task("t", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), project, user));
        taskService.completeTask(task, LocalTime.of(9, 0), LocalTime.of(10, 0));
        projectService.completeProject(project);
        Project saved = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void completeProject_withActiveTasks_shouldThrow() {
        projectService.createProject("Non Completabile");
        Project project = projectRepository.findByUser(user).getFirst();
        taskRepository.save(new Task("t", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), project, user));
        assertThatThrownBy(() -> projectService.completeProject(project)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeProject_withNoTasks_shouldThrow() {
        projectService.createProject("Vuoto");
        Project project = projectRepository.findByUser(user).getFirst();
        assertThatThrownBy(() -> projectService.completeProject(project)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statsOf_shouldReturnCorrectDuration() {
        projectService.createProject("Stats");
        Project project = projectRepository.findByUser(user).getFirst();
        taskRepository.save(new Task("t1", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), project, user));
        taskRepository.save(new Task("t2", LocalDate.now(), LocalTime.of(11, 0), LocalTime.of(12, 30), project, user));
        ProjectService.ProjectStats stats = projectService.statsOf(project);
        assertThat(stats.duration().toMinutes()).isEqualTo(150); // 60 + 90 min
    }
}

