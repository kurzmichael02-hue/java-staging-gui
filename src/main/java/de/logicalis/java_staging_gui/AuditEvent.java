package de.logicalis.java_staging_gui;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_project", columnList = "projectTitle"),
        @Index(name = "idx_audit_ts", columnList = "timestamp")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;
    private String projectTitle;
    private String action;
    private String actor;
    // hostname as a label (not an FK), so the audit entry survives even when the device is deleted
    private String target;
    private String details;

    protected AuditEvent() {
        // for JPA
    }

    public AuditEvent(String projectTitle, String action, String actor, String target, String details) {
        this.timestamp = System.currentTimeMillis();
        this.projectTitle = projectTitle;
        this.action = action;
        this.actor = actor;
        this.target = target;
        this.details = details;
    }

    public Long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getProjectTitle() { return projectTitle; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getTarget() { return target; }
    public String getDetails() { return details; }

    // formatting helper so the Thymeleaf template does not need 'new Date(...)' in the
    // expression engine (the default OGNL/SpEL does not allow that out of the box)
    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }

    // CSS class for the action pill, instead of the ternary directly in the template
    public String getActionCssClass() {
        if (action == null) return "a-DEFAULT";
        if (action.startsWith("BULK")) return "a-BULK";
        return "a-" + action.replace("DEVICE_", "");
    }

    // human-readable short label for the pill ("CREATED", "FINISHED", "BULK STARTED")
    public String getActionLabel() {
        if (action == null) return "?";
        return action.replace("DEVICE_", "").replace("_", " ");
    }
}
