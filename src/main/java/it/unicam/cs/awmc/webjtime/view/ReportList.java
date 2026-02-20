package it.unicam.cs.awmc.webjtime.view;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Report;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ProjectRepository;
import it.unicam.cs.awmc.webjtime.repository.ReportRepository;
import it.unicam.cs.awmc.webjtime.repository.TaskRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showError;
import static it.unicam.cs.awmc.webjtime.view.MainLayout.showSuccess;
/**
 * Vista Report: mostra i report salvati e le task filtrate da ciascuno.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports")
public class ReportList extends VerticalLayout {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NA = "-Not available-";
    private final ReportRepository reportRepo;
    private final TaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final ComboBox<String> reportCombo = new ComboBox<>("Report");
    private final Span projectLabel     = new Span(NA);
    private final Span startLabel       = new Span(NA);
    private final Span endLabel         = new Span(NA);
    private final Span tasksStatusLabel = new Span(NA);
    private final Grid<Task> grid = new Grid<>(Task.class, false);

    public ReportList(ReportRepository reportRepo, TaskRepository taskRepo,
                      ProjectRepository projectRepo) {
        this.reportRepo  = reportRepo;
        this.taskRepo    = taskRepo;
        this.projectRepo = projectRepo;
        configureGrid();
        configureCombo();
        Button addBtn    = new Button("Add Report",    e -> openAddReportDialog());
        Button deleteBtn = new Button("Delete Report", e -> deleteReport());
        HorizontalLayout toolbar = new HorizontalLayout(reportCombo, addBtn, deleteBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        HorizontalLayout info = new HorizontalLayout(
                labeledSpan("Project: ",          projectLabel),
                labeledSpan("From: ",             startLabel),
                labeledSpan("To: ",               endLabel),
                labeledSpan("Active / Completed: ", tasksStatusLabel)
        );
        add(toolbar, info, grid);
        setSizeFull();
        refresh();
    }
    private void configureGrid() {
        grid.addColumn(Task::getName).setHeader("Name").setSortable(true);
        grid.addColumn(t -> t.getDate().format(DATE_FMT)).setHeader("Date");
        grid.addColumn(t -> {
            if (!t.getOldDuration().isZero())
                return t.getDuration().minus(t.getOldDuration()).toMinutes() + " m";
            return NA;
        }).setHeader("Difference");
        grid.addColumn(Task::getStatus).setHeader("Status");
        grid.setSizeFull();
    }
    private void configureCombo() {
        reportCombo.addValueChangeListener(e -> {
            resetInfo();
            if (e.getValue() != null) populateInfo(e.getValue());
        });
    }
    private void refresh() {
        String current = reportCombo.getValue();
        List<String> names = reportRepo.findAll().stream().map(Report::getName).toList();
        reportCombo.setItems(names);
        if (current != null && names.contains(current)) {
            reportCombo.setValue(current);
        }
    }
    /**
     * Recupera le task filtrate per il report usando query mirate.
     * Gestisce date parziali: solo start, solo end, o entrambe.
     */
    private List<Task> getTasksOf(Report report) {
        LocalDate start = report.getStartDate();
        LocalDate end   = report.getEndDate();
        List<Task> byDate;
        if (start != null && end != null) {
            byDate = taskRepo.findByDateBetween(start, end);
        } else if (start != null) {
            byDate = taskRepo.findByDateGreaterThanEqual(start);
        } else if (end != null) {
            byDate = taskRepo.findByDateLessThanEqual(end);
        } else {
            byDate = taskRepo.findAll();
        }
        if (report.getProject() != null) {
            return byDate.stream()
                    .filter(t -> t.getProject() != null
                            && t.getProject().getName().equals(report.getProject()))
                    .toList();
        }
        return byDate;
    }
    private void populateInfo(String reportName) {
        Report report = reportRepo.findByName(reportName).orElse(null);
        if (report == null) return;
        List<Task> tasks = getTasksOf(report);
        grid.setItems(tasks);
        projectLabel.setText(report.getProject() != null ? report.getProject() : NA);
        startLabel.setText(report.getStartDate() != null ? report.getStartDate().format(DATE_FMT) : NA);
        endLabel.setText(report.getEndDate()   != null ? report.getEndDate().format(DATE_FMT)   : NA);
        long active    = tasks.stream().filter(t -> t.getStatus() == Status.ACTIVE).count();
        long completed = tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
        tasksStatusLabel.setText(active + " / " + completed);
    }
    private void resetInfo() {
        grid.setItems(List.of());
        projectLabel.setText(NA);
        startLabel.setText(NA);
        endLabel.setText(NA);
        tasksStatusLabel.setText(NA);
    }
    private void openAddReportDialog() {
        TextField name = new TextField("Name");
        ComboBox<String> project = new ComboBox<>("Project");
        project.setItems(projectRepo.findAll().stream().map(Project::getName).toList());
        DatePicker start = new DatePicker("Start date");
        DatePicker end   = new DatePicker("End date");
        start.setValue(LocalDate.now());
        end.setValue(LocalDate.now().plusDays(7));
        DialogBuilder.build("New Report", dialog -> {
            if (name.isEmpty()) { Notification.show("Name is required"); return; }
            if (start.getValue() != null && end.getValue() != null
                    && start.getValue().isAfter(end.getValue())) {
                Notification.show("Start date cannot be after end date"); return;
            }
            reportRepo.save(new Report(name.getValue(), start.getValue(), end.getValue(), project.getValue()));
            dialog.close();
            refresh();
            showSuccess("Report created");
        }, name, project, start, end);
    }
    private void deleteReport() {
        String selected = reportCombo.getValue();
        if (selected == null) { showError("Select a report first"); return; }
        reportRepo.findByName(selected).ifPresent(r -> {
            reportRepo.delete(r);
            resetInfo();
            refresh();
            showSuccess("Report deleted");
        });
    }
    private HorizontalLayout labeledSpan(String label, Span value) {
        return new HorizontalLayout(new Span(label), value);
    }
}