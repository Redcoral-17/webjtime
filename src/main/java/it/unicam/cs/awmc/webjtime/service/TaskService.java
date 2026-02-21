package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service per la gestione delle Task.
 * Centralizza la logica di business separandola dalla View.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepo;

    public TaskService(TaskRepository taskRepo) {
        this.taskRepo = taskRepo;
    }

    public List<Task> getTasksByDate(LocalDate date) {
        return taskRepo.findByDateOrderByStartTimeAsc(date);
    }

    public List<Task> getTasksByProject(Project project) {
        return taskRepo.findByProject(project);
    }

    public List<Task> getTasksByDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null) return taskRepo.findByDateBetween(start, end);
        if (start != null)               return taskRepo.findByDateGreaterThanEqual(start);
        if (end != null)                 return taskRepo.findByDateLessThanEqual(end);
        return taskRepo.findAll();
    }

    public boolean hasOverlap(LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        return !taskRepo.findOverlapping(date, start, end, excludeId).isEmpty();
    }

    @Transactional
    public void addTask(String name, LocalDate date, LocalTime start, LocalTime end, Project project) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (date == null || start == null || end == null) throw new IllegalArgumentException("Date, start and end are required");
        if (start.isAfter(end)) throw new IllegalArgumentException("Start cannot be after End");
        if (hasOverlap(date, start, end, null)) throw new IllegalStateException("A task already exists in this time slot");
        taskRepo.save(new Task(name, date, start, end, project));
    }

    @Transactional
    public void completeTask(Task task, LocalTime actualStart, LocalTime actualEnd) {
        if (task.getStatus() != Status.ACTIVE) throw new IllegalStateException("Task is already completed");
        if (actualStart == null || actualEnd == null) throw new IllegalArgumentException("Both times are required");
        if (actualStart.isAfter(actualEnd)) throw new IllegalArgumentException("Start cannot be after End");
        if (hasOverlap(task.getDate(), actualStart, actualEnd, task.getId())) throw new IllegalStateException("Overlaps with another task");
        task.setOldDuration(task.getDuration());
        task.setStartTime(actualStart);
        task.setEndTime(actualEnd);
        task.setDuration(Duration.between(actualStart, actualEnd));
        task.setStatus(Status.COMPLETED);
        taskRepo.save(task);
    }

    @Transactional
    public void deleteTask(Task task) {
        if (task.getStatus() != Status.ACTIVE) throw new IllegalStateException("Only ACTIVE tasks can be deleted");
        taskRepo.delete(task);
    }
}

