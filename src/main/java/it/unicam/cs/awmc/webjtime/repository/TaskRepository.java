package it.unicam.cs.awmc.webjtime.repository;

import it.unicam.cs.awmc.webjtime.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
