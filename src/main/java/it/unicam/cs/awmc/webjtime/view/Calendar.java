package it.unicam.cs.awmc.webjtime.view;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;
/**
 * Vista Calendario: mostra le task del giorno selezionato.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Route(value = "calendar", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Calendar")
public class Calendar extends VerticalLayout {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<LocalTime> TIME_SLOTS = Stream
            .iterate(LocalTime.MIDNIGHT, t -> t.plusMinutes(15))
            .limit(24 * 4)
            .toList();
    private final TaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final DatePicker datePicker = new DatePicker("Date");
    private final Grid<Task> grid = new Grid<>(Task.class, false);
    public Calendar(TaskRepository taskRepo, ProjectRepository projectRepo) {
        this.taskRepo = taskRepo;
        this.projectRepo = projectRepo;
        configureGrid();
        configureDatePicker();
        Button addBtn    = new Button("Add Task",    e -> openAddTaskDialog());
        Button endBtn    = new Button("End Task",    e -> openEndTaskDialog());
        Button deleteBtn = new Button("Delete Task", e -> deleteTask());
        HorizontalLayout toolbar = new HorizontalLayout(datePicker, addBtn, endBtn, deleteBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        add(toolbar, grid);
        setSizeFull();
        refresh();
    }
    private void configureGrid() {
        grid.addColumn(Task::getName).setHeader("Name").setSortable(true);
        grid.addColumn(task -> task.getProject() != null ? task.getProject().getName() : "-No Project-")
                .setHeader("Project");
        grid.addColumn(task -> task.getStartTime() != null ? task.getStartTime().format(TIME_FMT) : "-")
                .setHeader("Start").setSortable(true);
        grid.addColumn(task -> task.getEndTime() != null ? task.getEndTime().format(TIME_FMT) : "-")
                .setHeader("End");
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
        grid.setItems(taskRepo.findByDateOrderByStartTimeAsc(selected));
    }
    private void openAddTaskDialog() {
        TextField name    = new TextField("Name");
        ComboBox<Project> project = new ComboBox<>("Project");
        project.setItems(projectRepo.findByStatus(Status.ACTIVE));
        project.setItemLabelGenerator(Project::getName);
        DatePicker date   = new DatePicker("Date");
        date.setValue(datePicker.getValue());
        ComboBox<LocalTime> start = timeComboBox("Start");
        ComboBox<LocalTime> end   = timeComboBox("End");
        DialogBuilder.build("Add Task", dialog -> {
            if (name.isEmpty()) { Notification.show("Name is required"); return; }
            if (date.isEmpty() || start.isEmpty() || end.isEmpty()) { Notification.show("Fill in all fields"); return; }
            if (start.getValue().isAfter(end.getValue())) { Notification.show("Start cannot be after End"); return; }
            if (hasOverlap(date.getValue(), start.getValue(), end.getValue(), null)) { Notification.show("A task already exists in this time slot"); return; }
            taskRepo.save(new Task(name.getValue(), date.getValue(), start.getValue(), end.getValue(), project.getValue()));
            dialog.close();
            refresh();
            showSuccess("Task added");
        }, name, project, date, start, end);
    }
    private void openEndTaskDialog() {
        grid.asSingleSelect().getOptionalValue().ifPresentOrElse(task -> {
            if (task.getStatus() != Status.ACTIVE) { Notification.show("Task is already completed"); return; }
            ComboBox<LocalTime> start = timeComboBox("Actual Start");
            ComboBox<LocalTime> end   = timeComboBox("Actual End");
            start.setValue(task.getStartTime());
            end.setValue(task.getEndTime());
            DialogBuilder.build("End Task: " + task.getName(), "Confirm", dialog -> {
                if (start.isEmpty() || end.isEmpty()) { Notification.show("Fill in both times"); return; }
                if (start.getValue().isAfter(end.getValue())) { Notification.show("Start cannot be after End"); return; }
                if (hasOverlap(task.getDate(), start.getValue(), end.getValue(), task.getId())) { Notification.show("Overlaps with another task"); return; }
                task.setOldDuration(task.getDuration());
                task.setStartTime(start.getValue());
                task.setEndTime(end.getValue());
                task.setDuration(Duration.between(start.getValue(), end.getValue()));
                task.setStatus(Status.COMPLETED);
                taskRepo.save(task);
                dialog.close();
                refresh();
                showSuccess("Task completed");
            }, start, end);
        }, () -> Notification.show("Select a task first"));
    }
    private void deleteTask() {
        Task selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getStatus() != Status.ACTIVE) {
            showError("Select an ACTIVE task first");
            return;
        }
        taskRepo.delete(selected);
        refresh();
        showSuccess("Task deleted");
    }
    private ComboBox<LocalTime> timeComboBox(String label) {
        ComboBox<LocalTime> cb = new ComboBox<>(label);
        cb.setItems(TIME_SLOTS);
        cb.setItemLabelGenerator(t -> t.format(TIME_FMT));
        return cb;
    }
    /** Verifica sovrapposizioni delegando la query al DB tramite JPQL. */
    private boolean hasOverlap(LocalDate date, LocalTime start, LocalTime end, Long excludeId) {
        return !taskRepo.findOverlapping(date, start, end, excludeId).isEmpty();
    }
}