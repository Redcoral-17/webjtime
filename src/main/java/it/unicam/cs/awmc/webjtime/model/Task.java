package it.unicam.cs.awmc.webjtime.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@Setter
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private LocalTime startTime;
    @Column(nullable = false)
    private LocalTime endTime;
    private Duration duration;
    private Duration oldDuration = Duration.ZERO;
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Task(String n, LocalDate d, LocalTime s, LocalTime e, Project p, User u) {
        this.name = n;
        this.date = d;
        this.startTime = s;
        this.endTime = e;
        this.duration = Duration.between(s, e);
        this.project = p;
        this.user = u;
    }
}
