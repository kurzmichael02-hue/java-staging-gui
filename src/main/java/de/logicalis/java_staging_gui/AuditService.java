package de.logicalis.java_staging_gui;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditEventRepository repo;

    public void logDevice(Project project, Device device, String action) {
        logDevice(project, device, action, null);
    }

    public void logDevice(Project project, Device device, String action, String details) {
        if (project == null || device == null) return;
        repo.save(new AuditEvent(
                project.getProjectTitle(),
                action,
                actorFor(project, device),
                device.getHostname(),
                details));
    }

    public void logBulk(Project project, String action, String target, String details) {
        if (project == null) return;
        repo.save(new AuditEvent(
                project.getProjectTitle(),
                action,
                project.getStagerName(),
                target,
                details));
    }

    public List<AuditEvent> getEventsForProject(String projectTitle) {
        return repo.findByProjectTitleOrderByTimestampDesc(projectTitle);
    }

    // actor = the stager handling the device, falling back to the stager who created the project
    private static String actorFor(Project project, Device device) {
        if (device.getAssignedTo() != null && !device.getAssignedTo().isBlank()) {
            return device.getAssignedTo();
        }
        return project.getStagerName();
    }
}
