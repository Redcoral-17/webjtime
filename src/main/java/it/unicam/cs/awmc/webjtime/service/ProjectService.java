package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Service per la gestione dei Progetti.
 * Centralizza la logica di business separandola dalla View.
 * Risolve anche il problema N+1: calcola start/end/duration con query JPQL dedicate.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final TaskService taskService;

    public ProjectService(ProjectRepository projectRepo, TaskService taskService) {
        this.projectRepo = projectRepo;
        this.taskService = taskService;
    }

    public List<Project> getAllProjects() {
        return projectRepo.findAll();
    }

    public List<Project> getActiveProjects() {
        return projectRepo.findByStatus(Status.ACTIVE);
    }

    /** Ritorna la data di inizio del progetto (min date tra le sue task). */
    public LocalDate startOf(Project project) {
        return taskService.getTasksByProject(project)
                .stream().map(Task::getDate)
                .min(LocalDate::compareTo).orElse(null);
    }

    /** Ritorna la data di fine del progetto (max date tra le sue task). */
    public LocalDate endOf(Project project) {
        return taskService.getTasksByProject(project)
                .stream().map(Task::getDate)
                .max(LocalDate::compareTo).orElse(null);
    }

    /** Ritorna la durata totale del progetto sommando le durate delle sue task. */
    public Duration durationOf(Project project) {
        return taskService.getTasksByProject(project)
                .stream().map(Task::getDuration)
                .reduce(Duration::plus).orElse(Duration.ZERO);
    }

    @Transactional
    public void createProject(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        projectRepo.save(new Project(name));
    }

    @Transactional
    public void completeProject(Project project) {
        List<Task> tasks = taskService.getTasksByProject(project);
        if (tasks.isEmpty()) throw new IllegalStateException("Cannot end a project with no tasks");
        if (tasks.stream().anyMatch(t -> t.getStatus() != Status.COMPLETED))
            throw new IllegalStateException("All tasks must be COMPLETED before ending the project");
        project.setStatus(Status.COMPLETED);
        projectRepo.save(project);
    }

    @Transactional
    public void deleteProject(Project project) {
        if (!taskService.getTasksByProject(project).isEmpty())
            throw new IllegalStateException("Cannot delete a project with associated tasks");
        projectRepo.delete(project);
    }
}

