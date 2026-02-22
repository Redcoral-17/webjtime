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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Project(String n, User u) {
        this.name = n;
        this.user = u;
    }
}
