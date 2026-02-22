package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.model.User;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    public record ProjectStats(LocalDate start, LocalDate end, Duration duration) {}

    private final ProjectRepository pRepo;
    private final TaskRepository tRepo;

    public ProjectService(ProjectRepository pRepo, TaskRepository tRepo) {
        this.pRepo = pRepo;
        this.tRepo = tRepo;
    }

    public List<Project> getAllProjects(User u) { return pRepo.findByUser(u); }

    public List<Project> getActiveProjects(User u) { return pRepo.findByUserAndStatus(u, Status.ACTIVE); }

    public ProjectStats statsOf(@NonNull Project project) {
        LocalDate start = project.getStartDate();
        LocalDate end = project.getEndDate();
        Duration duration = getTasksByProject(project).stream().map(Task::getDuration).reduce(Duration::plus).orElse(Duration.ZERO);
        return new ProjectStats(start, end, duration);
    }

    @Transactional
    public void createProject(String n, User u) {
        if (n == null || n.isBlank()) throw new IllegalArgumentException("Name is required");
        pRepo.save(new Project(n, u));
    }

    @Transactional
    public void completeProject(Project p) {
        List<Task> tasks = getTasksByProject(p);
        if (tasks.isEmpty()) throw new IllegalStateException("Cannot end a project with no tasks");
        if (tasks.stream().anyMatch(t -> t.getStatus() != Status.COMPLETED))
            throw new IllegalStateException("All tasks must be completed before ending the project");
        p.setStatus(Status.COMPLETED);
        pRepo.save(p);
    }

    @Transactional
    public void deleteProject(@NonNull Project p) {
        if (p.getStatus() != Status.ACTIVE) throw new IllegalStateException("Only active projects can be deleted");
        if (!getTasksByProject(p).isEmpty()) throw new IllegalStateException("Cannot delete a project with associated tasks");
        pRepo.delete(p);
    }

    private List<Task> getTasksByProject(Project p) {
        return tRepo.findByProject(p);
    }
}

