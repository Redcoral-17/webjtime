package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Rappresenta un report (Report).
 * Questa entità è mappata su una tabella del database tramite JPA.
 * Utilizza Lombok per generare getter, setter e il costruttore senza argomenti.
 *
 * @author Filippo Corallini (125587), filippo.corallini@studenti.unicam.it
 */
@Entity
@Getter
@NoArgsConstructor
@Setter
@Table
public class Report {
    /**
     * Identificatore univoco del report generato automaticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Nome del report.
     */
    private String name;
    /**
     * Data di inizio del report.
     */
    private LocalDate startDate;
    /**
     * Data di fine del report.
     */
    private LocalDate endDate;
    /**
     * Progetto associato al report (opzionale).
     * FK reale verso {@link Project} per garantire integrità referenziale.
     */
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Costruisce un nuovo Report con i parametri forniti.
     *
     * @param name      nome
     * @param startDate data di inizio
     * @param endDate   data di fine
     * @param project   progetto (può essere null)
     */
    public Report(String name, LocalDate startDate, LocalDate endDate, Project project) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.project = project;
    }
}