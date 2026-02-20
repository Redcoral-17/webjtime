package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rappresenta un progetto (Project).
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
public class Project {
    /**
     * Identificatore univoco del progetto generato automaticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Nome del progetto.
     */
    private String name;
    /**
     * Stato del progetto.
     */
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Costruisce una nuovo Project con i parametri forniti.
     * Lo {@code status} è impostato su ACTIVE.
     *
     * @param name nome
     */
    public Project(String name) {
        this.name = name;
        this.status = Status.ACTIVE;
    }
}

