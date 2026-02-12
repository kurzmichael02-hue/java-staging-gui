package de.logicalis.java_staging_gui;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectTitle(String projectTitle);

    boolean existsByProjectTitle(String projectTitle);

    List<Project> findAllByOrderByCreatedAtDesc();
}
