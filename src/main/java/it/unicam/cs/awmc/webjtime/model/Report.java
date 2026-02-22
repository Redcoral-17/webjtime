package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Column(nullable = false)
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    public Report(String n, LocalDate s, LocalDate e, Project p) {
        this.name = n;
        this.startDate = s;
        this.endDate = e;
        this.project = p;
    }
}