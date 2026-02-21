package it.unicam.cs.awmc.webjtime.service;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Report;
import it.unicam.cs.awmc.webjtime.model.Task;
import it.unicam.cs.awmc.webjtime.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service per la gestione dei Report.
 * Centralizza la logica di business separandola dalla View.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepo;
    private final TaskService taskService;

    public ReportService(ReportRepository reportRepo, TaskService taskService) {
        this.reportRepo = reportRepo;
        this.taskService = taskService;
    }

    public List<Report> getAllReports() {
        return reportRepo.findAll();
    }

    /**
     * Recupera le task filtrate per il report usando query mirate.
     * Gestisce date parziali: solo start, solo end, o entrambe.
     */
    public List<Task> getTasksOf(Report report) {
        LocalDate start = report.getStartDate();
        LocalDate end   = report.getEndDate();
        List<Task> byDate = taskService.getTasksByDateRange(start, end);
        if (report.getProject() != null) {
            Long projectId = report.getProject().getId();
            return byDate.stream()
                    .filter(t -> t.getProject() != null
                              && t.getProject().getId().equals(projectId))
                    .toList();
        }
        return byDate;
    }

    @Transactional
    public void createReport(String name, LocalDate startDate, LocalDate endDate, Project project) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        if (startDate != null && endDate != null && startDate.isAfter(endDate))
            throw new IllegalArgumentException("Start date cannot be after end date");
        reportRepo.save(new Report(name, startDate, endDate, project));
    }

    @Transactional
    public void deleteReport(Report report) {
        reportRepo.delete(report);
    }
}

