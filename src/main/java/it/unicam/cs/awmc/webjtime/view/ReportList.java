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
import jakarta.annotation.security.PermitAll;
import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Report;
import it.unicam.cs.awmc.webjtime.model.Status;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.service.ProjectService;
import it.unicam.cs.awmc.webjtime.service.ReportService;

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
@PermitAll
public class ReportList extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NA = "-Not available-";

    private final ReportService reportService;
    private final ProjectService projectService;

    private final ComboBox<Report> reportCombo  = new ComboBox<>("Report");
    private final Span projectLabel             = new Span(NA);
    private final Span startLabel               = new Span(NA);
    private final Span endLabel                 = new Span(NA);
    private final Span tasksStatusLabel         = new Span(NA);
    private final Grid<Task> grid               = new Grid<>(Task.class, false);

    public ReportList(ReportService reportService, ProjectService projectService) {
        this.reportService  = reportService;
        this.projectService = projectService;
        configureGrid();
        configureCombo();
        Button addBtn    = new Button("Add Report",    e -> openAddReportDialog());
        Button deleteBtn = new Button("Delete Report", e -> deleteReport());
        HorizontalLayout toolbar = new HorizontalLayout(reportCombo, addBtn, deleteBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        HorizontalLayout info = new HorizontalLayout(
                labeledSpan("Project: ",            projectLabel),
                labeledSpan("From: ",               startLabel),
                labeledSpan("To: ",                 endLabel),
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
        reportCombo.setItemLabelGenerator(Report::getName);
        reportCombo.addValueChangeListener(e -> {
            resetInfo();
            if (e.getValue() != null) populateInfo(e.getValue());
        });
    }

    private void refresh() {
        Report current = reportCombo.getValue();
        List<Report> reports = reportService.getAllReports();
        reportCombo.setItems(reports);
        if (current != null) {
            reports.stream()
                   .filter(r -> r.getId().equals(current.getId()))
                   .findFirst()
                   .ifPresent(reportCombo::setValue);
        }
    }

    private void populateInfo(Report report) {
        List<Task> tasks = reportService.getTasksOf(report);
        grid.setItems(tasks);
        projectLabel.setText(report.getProject() != null
                ? report.getProject().getName() : NA);
        startLabel.setText(report.getStartDate() != null
                ? report.getStartDate().format(DATE_FMT) : NA);
        endLabel.setText(report.getEndDate() != null
                ? report.getEndDate().format(DATE_FMT) : NA);
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
        ComboBox<Project> project = new ComboBox<>("Project");
        project.setItems(projectService.getAllProjects());
        project.setItemLabelGenerator(Project::getName);
        DatePicker start = new DatePicker("Start date");
        DatePicker end   = new DatePicker("End date");
        start.setValue(LocalDate.now());
        end.setValue(LocalDate.now().plusDays(7));
        DialogBuilder.build("New Report", dialog -> {
            if (name.isEmpty()) { Notification.show("Name is required"); return; }
            try {
                reportService.createReport(name.getValue(), start.getValue(), end.getValue(), project.getValue());
                dialog.close();
                refresh();
                showSuccess("Report created");
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage());
            }
        }, name, project, start, end);
    }

    private void deleteReport() {
        Report selected = reportCombo.getValue();
        if (selected == null) { showError("Select a report first"); return; }
        reportService.deleteReport(selected);
        resetInfo();
        refresh();
        showSuccess("Report deleted");
    }

    private HorizontalLayout labeledSpan(String label, Span value) {
        return new HorizontalLayout(new Span(label), value);
    }
}