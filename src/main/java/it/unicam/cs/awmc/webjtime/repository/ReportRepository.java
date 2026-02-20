package it.unicam.cs.awmc.webjtime.repository;

import it.unicam.cs.awmc.webjtime.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByName(String name);
}
