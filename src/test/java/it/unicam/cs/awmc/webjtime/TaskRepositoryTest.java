package it.unicam.cs.awmc.webjtime;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test di integrazione per TaskRepository.
 * Usa H2 in-memory tramite il profilo "dev".
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private ProjectRepository projectRepo;

    private final LocalDate TODAY = LocalDate.of(2024, 6, 1);

    @BeforeEach
    void setup() {
        taskRepo.deleteAll();
        projectRepo.deleteAll();
    }

    @Test
    void findByDateOrderByStartTimeAsc_returnsTasksForDate() {
        taskRepo.save(new Task("Task A", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null));
        taskRepo.save(new Task("Task B", TODAY, LocalTime.of(8, 0), LocalTime.of(9, 0), null));
        taskRepo.save(new Task("Task C", TODAY.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        List<Task> result = taskRepo.findByDateOrderByStartTimeAsc(TODAY);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getName()).isEqualTo("Task B");
        assertThat(result.get(1).getName()).isEqualTo("Task A");
    }

    @Test
    void findByProject_returnsOnlyTasksForGivenProject() {
        Project p1 = projectRepo.save(new Project("Project 1"));
        Project p2 = projectRepo.save(new Project("Project 2"));
        taskRepo.save(new Task("T1", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), p1));
        taskRepo.save(new Task("T2", TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0), p2));
        taskRepo.save(new Task("T3", TODAY, LocalTime.of(11, 0), LocalTime.of(12, 0), p1));

        List<Task> result = taskRepo.findByProject(p1);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getProject().getId().equals(p1.getId()));
    }

    @Test
    void findOverlapping_detectsOverlap() {
        taskRepo.save(new Task("Existing", TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null));

        List<Task> overlap = taskRepo.findOverlapping(TODAY, LocalTime.of(10, 0), LocalTime.of(12, 0), null);
        assertThat(overlap).isNotEmpty();
    }

    @Test
    void findOverlapping_noOverlapWhenAdjacent() {
        taskRepo.save(new Task("Existing", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        List<Task> overlap = taskRepo.findOverlapping(TODAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        assertThat(overlap).isEmpty();
    }

    @Test
    void findOverlapping_excludesCurrentTask() {
        Task saved = taskRepo.save(new Task("Self", TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null));

        List<Task> overlap = taskRepo.findOverlapping(TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0), saved.getId());
        assertThat(overlap).isEmpty();
    }

    @Test
    void findByDateBetween_returnsTasksInRange() {
        taskRepo.save(new Task("T1", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null));
        taskRepo.save(new Task("T2", TODAY.plusDays(3), LocalTime.of(9, 0), LocalTime.of(10, 0), null));
        taskRepo.save(new Task("T3", TODAY.plusDays(10), LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        List<Task> result = taskRepo.findByDateBetween(TODAY, TODAY.plusDays(5));

        assertThat(result).hasSize(2);
    }

    @Test
    void task_statusIsActiveAfterCreation() {
        Task t = taskRepo.save(new Task("T", TODAY, LocalTime.of(9, 0), LocalTime.of(10, 0), null));

        assertThat(t.getStatus()).isEqualTo(Status.ACTIVE);
    }
}
