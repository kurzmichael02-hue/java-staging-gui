package de.logicalis.java_staging_gui;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByProjectTitleOrderByTimestampDesc(String projectTitle);
}
