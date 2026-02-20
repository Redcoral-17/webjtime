package it.unicam.cs.awmc.webjtime.repository;

import it.unicam.cs.awmc.webjtime.model.Project;
import it.unicam.cs.awmc.webjtime.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDateOrderByStartTimeAsc(LocalDate date);
    List<Task> findByProject(Project project);
    List<Task> findByDateGreaterThanEqual(LocalDate start);
    List<Task> findByDateLessThanEqual(LocalDate end);
    List<Task> findByDateBetween(LocalDate start, LocalDate end);
    @Query("""
            SELECT t FROM Task t
            WHERE t.date = :date
              AND (:excludeId IS NULL OR t.id <> :excludeId)
              AND (t.status = 'ACTIVE' OR t.status = 'COMPLETED')
              AND t.startTime < :end
              AND t.endTime   > :start
            """)
    List<Task> findOverlapping(@Param("date") LocalDate date,
                               @Param("start") java.time.LocalTime start,
                               @Param("end") java.time.LocalTime end,
                               @Param("excludeId") Long excludeId);
}
