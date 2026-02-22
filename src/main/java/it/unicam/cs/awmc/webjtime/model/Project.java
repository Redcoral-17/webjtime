package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Column(nullable = false)
    private String name;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now();
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    public Project(String n) {
        this.name = n;
    }
}
