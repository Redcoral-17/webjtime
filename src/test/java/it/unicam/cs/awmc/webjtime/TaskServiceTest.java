package it.unicam.cs.awmc.webjtime;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import it.unicam.cs.awmc.webjtime.repository.UserRepository;
import it.unicam.cs.awmc.webjtime.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TaskServiceTest {
    @Autowired TaskService taskService;
    @Autowired TaskRepository taskRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    private User user;
    private final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User("tester", "password"));
    }

    @Test
    void createTask_shouldPersistTask() {
        taskService.createTask("Task1", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        List<Task> tasks = taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getName()).isEqualTo("Task1");
    }

    @Test
    void createTask_blankName_shouldThrow() {
        assertThatThrownBy(() -> taskService.createTask("", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTask_startAfterEnd_shouldThrow() {
        assertThatThrownBy(() -> taskService.createTask("T", TODAY, LocalTime.of(11, 0), LocalTime.of(9, 0), null, user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTask_overlappingActive_shouldThrow() {
        taskService.createTask("Task1", TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null, user);
        assertThatThrownBy(() -> taskService.createTask("Task2", TODAY, LocalTime.of(10, 0), LocalTime.of(12, 0), null, user))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createTask_adjacentTimes_shouldNotOverlap() {
        taskService.createTask("Task1", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        assertThatNoException().isThrownBy(() ->
                taskService.createTask("Task2", TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null, user));
    }

    @Test
    void completeTask_shouldSetStatusAndNewTimes() {
        taskService.createTask("Task", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        Task task = taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY).getFirst();
        taskService.completeTask(task, LocalTime.of(9, 15), LocalTime.of(10, 15));
        Task updated = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(updated.getStartTime()).isEqualTo(LocalTime.of(9, 15));
        assertThat(updated.getEndTime()).isEqualTo(LocalTime.of(10, 15));
    }

    @Test
    void completeTask_alreadyCompleted_shouldThrow() {
        taskService.createTask("Task", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        Task task = taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY).getFirst();
        taskService.completeTask(task, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThatThrownBy(() -> taskService.completeTask(task, LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteTask_active_shouldRemove() {
        taskService.createTask("Task", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        Task task = taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY).getFirst();
        taskService.deleteTask(task);
        assertThat(taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY)).isEmpty();
    }

    @Test
    void deleteTask_completed_shouldThrow() {
        taskService.createTask("Task", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null, user);
        Task task = taskRepository.findByUserAndDateOrderByStartTimeAsc(user, TODAY).getFirst();
        taskService.completeTask(task, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThatThrownBy(() -> taskService.deleteTask(task)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createTask_withProject_shouldUpdateProjectDates() {
        Project project = projectRepository.save(new Project("Progetto", user));
        taskService.createTask("T", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), project, user);
        Project updated = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(updated.getStartDate()).isEqualTo(TODAY);
        assertThat(updated.getEndDate()).isEqualTo(TODAY);
    }
}


