package it.unicam.cs.awmc.webjtime.view;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;
/**
 * Vista Progetti: mostra tutti i progetti con date e durata calcolate dalle task.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Projects")
@PermitAll
public class ProjectList extends VerticalLayout {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ProjectRepository projectRepo;
    private final TaskRepository taskRepo;
    private final Grid<Project> grid = new Grid<>(Project.class, false);
    public ProjectList(ProjectRepository projectRepo, TaskRepository taskRepo) {
        this.projectRepo = projectRepo;
        this.taskRepo = taskRepo;
        configureGrid();
        Button addBtn    = new Button("Add Project",    e -> openAddProjectDialog());
        Button endBtn    = new Button("End Project",    e -> endProject());
        Button deleteBtn = new Button("Delete Project", e -> deleteProject());
        add(new HorizontalLayout(addBtn, endBtn, deleteBtn), grid);
        setSizeFull();
        refresh();
    }
    private void configureGrid() {
        grid.addColumn(Project::getName).setHeader("Name").setSortable(true);
        grid.addColumn(p -> {
            LocalDate d = startOf(p);
            return d != null ? d.format(DATE_FMT) : "-Not available-";
        }).setHeader("Start");
        grid.addColumn(p -> {
            LocalDate d = endOf(p);
            return d != null ? d.format(DATE_FMT) : "-Not available-";
        }).setHeader("End");
        grid.addColumn(p -> {
            Duration d = durationOf(p);
            return !d.isZero() ? d.toHours() + " h " + d.toMinutesPart() + " m" : "-Not available-";
        }).setHeader("Duration");
        grid.addColumn(Project::getStatus).setHeader("Status");
        grid.setSizeFull();
    }
    private void refresh() {
        grid.setItems(projectRepo.findAll());
    }
    /** Query mirata: nessun findAll() + filter in memoria. */
    private List<Task> getTasksOf(Project project) {
        return taskRepo.findByProject(project);
    }
    private LocalDate startOf(Project p) {
        return getTasksOf(p).stream().map(Task::getDate).min(LocalDate::compareTo).orElse(null);
    }
    private LocalDate endOf(Project p) {
        return getTasksOf(p).stream().map(Task::getDate).max(LocalDate::compareTo).orElse(null);
    }
    private Duration durationOf(Project p) {
        return getTasksOf(p).stream().map(Task::getDuration).reduce(Duration::plus).orElse(Duration.ZERO);
    }
    private void openAddProjectDialog() {
        TextField name = new TextField("Project name");
        DialogBuilder.build("New Project", dialog -> {
            if (name.isEmpty()) { Notification.show("Name is required"); return; }
            projectRepo.save(new Project(name.getValue()));
            dialog.close();
            refresh();
            showSuccess("Project created");
        }, name);
    }
    private void endProject() {
        Project selected = grid.asSingleSelect().getValue();
        if (selected == null) { showError("Select a project first"); return; }
        List<Task> tasks = getTasksOf(selected);
        if (tasks.isEmpty()) {
            showError("Cannot end a project with no tasks");
            return;
        }
        if (tasks.stream().allMatch(t -> t.getStatus() == Status.COMPLETED)) {
            selected.setStatus(Status.COMPLETED);
            projectRepo.save(selected);
            refresh();
            showSuccess("Project completed");
        } else {
            showError("All tasks must be COMPLETED before ending the project");
        }
    }
    private void deleteProject() {
        Project selected = grid.asSingleSelect().getValue();
        if (selected == null) { showError("Select a project first"); return; }
        if (!getTasksOf(selected).isEmpty()) {
            showError("Cannot delete a project with associated tasks");
            return;
        }
        DialogBuilder.build("Delete Project",
                "Delete",
                dialog -> {
                    projectRepo.delete(selected);
                    dialog.close();
                    refresh();
                    showSuccess("Project deleted");
                });
    }
}