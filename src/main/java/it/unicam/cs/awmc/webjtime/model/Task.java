package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Rappresenta un'attività (Task).
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
public class Task {
    /**
     * Identificatore univoco della task generato automaticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Nome della task.
     */
    private String name;
    /**
     * Data associata alla task.
     */
    private LocalDate date;
    /**
     * Orario di inizio della task.
     */
    private LocalTime startTime;
    /**
     * Orario di fine della task.
     */
    private LocalTime endTime;
    /**
     * Durata precedente della task (viene utilizza per salvare la durata precedente).
     */
    private Duration oldDuration;
    /**
     * Durata corrente calcolata tra startTime ed endTime.
     */
    private Duration duration;
    /**
     * Stato della task.
     */
    @Enumerated(EnumType.STRING)
    private Status status;
    /**
     * Progetto al quale la task può appartenere.
     */
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Costruisce una nuova Task con i parametri forniti.
     * La {@code duration} viene calcolata come la differenza tra {@code startTime} e {@code endTime}.
     * {@code oldDuration} viene inizializzata a {@link Duration#ZERO} e lo {@code status} è impostato su ACTIVE.
     *
     * @param name nome
     * @param date data
     * @param startTime orario di inizio
     * @param endTime orario di fine
     * @param project progetto associato alla task
     */
    public Task(String name, LocalDate date, LocalTime startTime, LocalTime endTime, Project project) {
        this.name = name;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.oldDuration = Duration.ZERO;
        this.duration = Duration.between(startTime, endTime);
        this.status = Status.ACTIVE;
        this.project = project;
    }
}
