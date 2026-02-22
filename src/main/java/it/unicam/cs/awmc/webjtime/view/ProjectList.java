package it.unicam.cs.awmc.webjtime.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.service.ProjectService;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;
import static java.time.format.DateTimeFormatter.ofPattern;

@Route(value = "projects", layout = MainLayout.class)
@PageTitle("Lista dei Progetti")
@RolesAllowed("USER")
public class ProjectList extends VerticalLayout {
    private static final DateTimeFormatter DATE_FMT = ofPattern("dd/MM/yyyy");
    private final ProjectService pService;
    private final Grid<Project> grid = new Grid<>(Project.class, false);
    private Map<Long, ProjectService.ProjectStats> statsCache = Map.of();

    public ProjectList(ProjectService pService) {
        this.pService = pService;
        setSizeFull();
        configureGrid();
        Button createBtn = new Button("Aggiungi", e -> openCreateProjectDialog());
        Button completeBtn = new Button("Completa", e -> completeProject());
        Button deleteBtn = new Button("Elimina", e -> deleteProject());
        add(new HorizontalLayout(createBtn, completeBtn, deleteBtn), grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Project::getName).setHeader("Nome");
        grid.addColumn(p -> { ProjectService.ProjectStats s = statsCache.get(p.getId());
            return s != null && s.start() != null ? s.start().format(DATE_FMT) : "-Not available-";
        }).setHeader("Ora d'inizio");
        grid.addColumn(p -> { ProjectService.ProjectStats s = statsCache.get(p.getId());
            return s != null && s.end() != null ? s.end().format(DATE_FMT) : "-Not available-";
        }).setHeader("Ora di fine");
        grid.addColumn(p -> { ProjectService.ProjectStats s = statsCache.get(p.getId());
            return s != null && !s.duration().isZero() ? s.duration().toHours() + " h " + s.duration().toMinutesPart() + " m" : "-Not available-";
        }).setHeader("Durata");
        grid.addColumn(Project::getStatus).setHeader("Status").setSortable(true);
        grid.setSizeFull();
    }

    private void refresh() {
        List<Project> projects = pService.getAllProjects();
        statsCache = projects.stream().collect(Collectors.toMap(Project::getId, pService::statsOf));
        grid.setItems(projects);
    }

    private void openCreateProjectDialog() {
        TextField n = new TextField("Nome");
        DialogBuilder.build("Crea un nuovo progetto", dialog -> {
            if (n.isEmpty()) { Notification.show("Riempire tutti i campi"); return; }
            try {
                pService.createProject(n.getValue());
                dialog.close();
                refresh();
                showSuccess("Progetto creato con successo!");
            } catch (IllegalArgumentException ex) { Notification.show(ex.getMessage()); }
        }, n);
    }

    private void completeProject() {
        grid.asSingleSelect().getOptionalValue().ifPresentOrElse(project ->
                        DialogBuilder.build("Completare " + project.getName() + "?", "Complete", dialog -> {
                            try {
                                pService.completeProject(project);
                                dialog.close();
                                refresh();
                                showSuccess("Progetto completato con successo!");
                            } catch (IllegalStateException ex) { showError(ex.getMessage()); }
                        }), () -> showError("Seleziona un progetto da completare"));
    }

    private void deleteProject() {
        grid.asSingleSelect().getOptionalValue().ifPresentOrElse(project ->
                        DialogBuilder.build("Eliminare " + project.getName() + "?", "Delete", dialog -> {
                            try {
                                pService.deleteProject(project);
                                dialog.close();
                                refresh();
                                showSuccess("Progetto eliminato con successo!");
                            } catch (IllegalStateException ex) { showError(ex.getMessage()); }
                        }), () -> showError("Seleziona un progetto da eliminare"));
    }
}