package it.unicam.cs.awmc.webjtime.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.service.ProjectService;
import it.unicam.cs.awmc.webjtime.service.TaskService;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;
import static java.time.format.DateTimeFormatter.ofPattern;

@Route(value = "tasks", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Calendar")
@RolesAllowed("USER")
public class Calendar extends VerticalLayout {
    private static final DateTimeFormatter TIME_FMT = ofPattern("HH:mm");
    private final TaskService tService;
    private final ProjectService pService;
    private final DatePicker datePicker = new DatePicker("Seleziona data");
    private final Grid<Task> grid = new Grid<>(Task.class, false);

    public Calendar(TaskService tService, ProjectService pService) {
        this.tService = tService;
        this.pService = pService;
        setSizeFull();
        configureGrid();
        configureDatePicker();
        Button createBtn = new Button("Aggiungi", e -> openCreateTaskDialog());
        Button completeBtn = new Button("Completa", e -> openCompleteTaskDialog());
        Button deleteBtn = new Button("Rimuovi", e -> deleteTask());
        add(new HorizontalLayout(datePicker, createBtn, completeBtn, deleteBtn), grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Task::getName).setHeader("Nome");
        grid.addColumn(task -> task.getProject() != null ? task.getProject().getName() : "-No Project-").setHeader("Progetto");
        grid.addColumn(task -> task.getStartTime().format(TIME_FMT)).setHeader("Ora d'inizio").setSortable(true);
        grid.addColumn(task -> task.getEndTime().format(TIME_FMT)).setHeader("Ora di fine");
        grid.addColumn(Task::getStatus).setHeader("Status");
        grid.setSizeFull();
    }

    private void configureDatePicker() {
        datePicker.setValue(LocalDate.now());
        datePicker.addValueChangeListener(e -> refresh());
    }

    private void refresh() {
        LocalDate selected = datePicker.getValue();
        if (selected == null) return;
        grid.setItems(tService.getTasksByDate(selected));
    }

    private void openCreateTaskDialog() {
        TextField n = new TextField("Nome");
        ComboBox<Project> p = new ComboBox<>("Progetto");
        p.setItems(pService.getActiveProjects());
        p.setItemLabelGenerator(Project::getName);
        DatePicker d = new DatePicker("Data");
        d.setValue(datePicker.getValue());
        ComboBox<LocalTime> s = timeComboBox("Ora d'inizio");
        ComboBox<LocalTime> e = timeComboBox("Ora di fine");
        DialogBuilder.build("Aggiungi una nuova task", dialog -> {
            if (n.isEmpty() || d.isEmpty() || s.isEmpty() || e.isEmpty()) { showError("Riempire tutti i campi"); return; }
            try {
                tService.createTask(n.getValue(), d.getValue(), s.getValue(), e.getValue(), p.getValue());
                dialog.close();
                refresh();
                showSuccess("Task aggiunta con successo!");
            } catch (IllegalArgumentException | IllegalStateException ex) { showError(ex.getMessage()); }
        }, n, p, d, s, e);
    }

    private void openCompleteTaskDialog() {
        grid.asSingleSelect().getOptionalValue().ifPresentOrElse(task -> {
            ComboBox<LocalTime> s = timeComboBox("Ora d'inizio");
            ComboBox<LocalTime> e = timeComboBox("Ora di fine");
            s.setValue(task.getStartTime());
            e.setValue(task.getEndTime());
            DialogBuilder.build("Completa Task: " + task.getName(), "Confirm", dialog -> {
                if (s.isEmpty() || e.isEmpty()) { showError("Riempire tutti i campi"); return; }
                try {
                    tService.completeTask(task, s.getValue(), e.getValue());
                    dialog.close();
                    refresh();
                    showSuccess("Task completata con successo!");
                } catch (IllegalArgumentException | IllegalStateException ex) { showError(ex.getMessage()); }
            }, s, e); }, () -> showError("Seleziona una task da completare"));
    }

    private void deleteTask() {
        grid.asSingleSelect().getOptionalValue().ifPresentOrElse(task ->
            DialogBuilder.build("Eliminare " + task.getName() + "?", "Delete", dialog -> {
                try {
                    tService.deleteTask(task);
                    dialog.close();
                    refresh();
                    showSuccess("Task eliminata con successo!");
                } catch (IllegalStateException ex) { showError(ex.getMessage()); }
            }), () -> showError("Seleziona una task da eliminare"));
    }

    private @NonNull ComboBox<LocalTime> timeComboBox(String label) {
        ComboBox<LocalTime> cb = new ComboBox<>(label);
        cb.setItems(Stream.iterate(LocalTime.MIDNIGHT, t -> t.plusMinutes(15)).limit(24 * 4).toList());
        cb.setItemLabelGenerator(t -> t.format(TIME_FMT));
        return cb;
    }
}
