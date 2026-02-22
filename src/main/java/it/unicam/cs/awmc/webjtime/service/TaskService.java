package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository tRepo;
    private final ProjectRepository pRepo;

    public TaskService(TaskRepository tRepo, ProjectRepository pRepo) {
        this.tRepo = tRepo;
        this.pRepo = pRepo;
    }

    public List<Task> getTasksByDate(LocalDate d) {
        return tRepo.findByDateOrderByStartTimeAsc(d);
    }

    @Transactional
    public void createTask(String n, LocalDate d, LocalTime s, LocalTime e, Project p) {
        if (n == null || n.isBlank()) throw new IllegalArgumentException("Name is required");
        if (d == null || s == null || e == null) throw new IllegalArgumentException("Date, start and end are required");
        if (s.isAfter(e)) throw new IllegalArgumentException("Start cannot be after end");
        if (hasOverlap(d, s, e, Status.ACTIVE)) throw new IllegalStateException("Overlaps with another active task");
        tRepo.save(new Task(n, d, s, e, p));
        if (p != null) refreshProjectDates(p);
    }

    @Transactional
    public void completeTask(@NonNull Task t, LocalTime s, LocalTime e) {
        if (t.getStatus() != Status.ACTIVE) throw new IllegalStateException("Task is already completed");
        if (s == null || e == null) throw new IllegalArgumentException("Start and end are required");
        if (s.isAfter(e)) throw new IllegalArgumentException("Start cannot be after end");
        if (hasOverlap(t.getDate(), s, e, Status.COMPLETED)) throw new IllegalStateException("Overlaps with another completed task");
        t.setOldDuration(t.getDuration());
        t.setStartTime(s);
        t.setEndTime(e);
        t.setDuration(Duration.between(s, e));
        t.setStatus(Status.COMPLETED);
        tRepo.save(t);
        if (t.getProject() != null) refreshProjectDates(t.getProject());
    }

    @Transactional
    public void deleteTask(@NonNull Task t) {
        if (t.getStatus() != Status.ACTIVE) throw new IllegalStateException("Only active tasks can be deleted");
        Project project = t.getProject();
        tRepo.delete(t);
        if (project != null) refreshProjectDates(project);
    }

    private boolean hasOverlap(LocalDate d, LocalTime s, LocalTime e, Status st) {
        List<Task> tasks = tRepo.findByDateAndStatus(d, st);
        for (Task task : tasks) {
            if (task.getStartTime().isBefore(e) && task.getEndTime().isAfter(s)) return true;
        }
        return false;
    }

    private void refreshProjectDates(@NonNull Project p) {
        List<Task> tasks = tRepo.findByProject(p);
        p.setStartDate(tasks.stream().map(Task::getDate).min(LocalDate::compareTo).orElse(LocalDate.now()));
        p.setEndDate(tasks.stream().map(Task::getDate).max(LocalDate::compareTo).orElse(LocalDate.now()));
        pRepo.save(p);
    }
}
