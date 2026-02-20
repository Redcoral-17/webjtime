package it.unicam.cs.awmc.webjtime.repository;

import it.unicam.cs.awmc.webjtime.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
