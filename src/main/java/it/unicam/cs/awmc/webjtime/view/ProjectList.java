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
import it.unicam.cs.awmc.webjtime.service.ProjectService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    private final ProjectService projectService;
    private final Grid<Project> grid = new Grid<>(Project.class, false);

    public ProjectList(ProjectService projectService) {
        this.projectService = projectService;
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
            LocalDate d = projectService.startOf(p);
            return d != null ? d.format(DATE_FMT) : "-Not available-";
        }).setHeader("Start");
        grid.addColumn(p -> {
            LocalDate d = projectService.endOf(p);
            return d != null ? d.format(DATE_FMT) : "-Not available-";
        }).setHeader("End");
        grid.addColumn(p -> {
            Duration d = projectService.durationOf(p);
            return !d.isZero() ? d.toHours() + " h " + d.toMinutesPart() + " m" : "-Not available-";
        }).setHeader("Duration");
        grid.addColumn(Project::getStatus).setHeader("Status");
        grid.setSizeFull();
    }

    private void refresh() {
        grid.setItems(projectService.getAllProjects());
    }

    private void openAddProjectDialog() {
        TextField name = new TextField("Project name");
        DialogBuilder.build("New Project", dialog -> {
            if (name.isEmpty()) { Notification.show("Name is required"); return; }
            try {
                projectService.createProject(name.getValue());
                dialog.close();
                refresh();
                showSuccess("Project created");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        }, name);
    }

    private void endProject() {
        Project selected = grid.asSingleSelect().getValue();
        if (selected == null) { showError("Select a project first"); return; }
        try {
            projectService.completeProject(selected);
            refresh();
            showSuccess("Project completed");
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteProject() {
        Project selected = grid.asSingleSelect().getValue();
        if (selected == null) { showError("Select a project first"); return; }
        DialogBuilder.build("Delete Project",
                "Delete",
                dialog -> {
                    try {
                        projectService.deleteProject(selected);
                        dialog.close();
                        refresh();
                        showSuccess("Project deleted");
                    } catch (IllegalStateException ex) {
                        Notification.show(ex.getMessage());
                    }
                });
    }
}