package de.logicalis.java_staging_gui;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
// DB-side uniqueness per project: hostname, ip and switch+port each only once.
// the application checks (existsInProject/portExistsInProject) stay for the
// friendly message, these constraints are the real safeguard against the
// race between the check and the insert during concurrent stagings.
@Table(name = "devices", uniqueConstraints = {
        @UniqueConstraint(name = "uq_dev_proj_hostname", columnNames = {"project_title", "hostname"}),
        @UniqueConstraint(name = "uq_dev_proj_ip", columnNames = {"project_title", "ip"}),
        @UniqueConstraint(name = "uq_dev_proj_port", columnNames = {"project_title", "upstream_switch", "upstream_port"})
})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String upstreamSwitch;
    private int upstreamPort;
    private String locationName;
    private String projectTitle;
    private String hostname;
    private String ip;
    private String serialNumber;

    private int retryCount;
    private String vendor;
    private String model;
    private String configProfile;
    private String configMode;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String customConfig;

    private String defaultGatewayIp;
    private String localDnsServerIp;
    private String localPrtgSensorIp;
    private String ntpServer;
    private String subnetMask;
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

    private String currentStep;
    private String errorMessage;
    private long createdAt;
    private long startedAt;
    private long finishedAt;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String generatedConfig;

    protected Device() {
        // for JPA
    }

    public Device(String upstreamSwitch, int upstreamPort,
                  String hostname, String ip, String serialNumber,
                  String locationName, String projectTitle,
                  String vendor, String model,
                  String configProfile,
                  String configMode, String customConfig,
                  String defaultGatewayIp, String localDnsServerIp,
                  String localPrtgSensorIp, String ntpServer,
                  String subnetMask, String assignedTo) {

        this.upstreamSwitch = upstreamSwitch;
        this.upstreamPort = upstreamPort;
        this.hostname = hostname;
        this.ip = ip;
        this.serialNumber = serialNumber;
        this.locationName = locationName;
        this.projectTitle = projectTitle;
        this.vendor = vendor;
        this.model = model;
        this.configProfile = configProfile;
        this.configMode = configMode;
        this.customConfig = customConfig;
        this.defaultGatewayIp = defaultGatewayIp;
        this.localDnsServerIp = localDnsServerIp;
        this.localPrtgSensorIp = localPrtgSensorIp;
        this.ntpServer = ntpServer;
        this.subnetMask = subnetMask;
        this.assignedTo = assignedTo;
        this.status = DeviceStatus.OPEN;
        this.currentStep = "Recorded";
        this.createdAt = System.currentTimeMillis();
    }

    public void incrementRetry() { this.retryCount++; }

    public void resetForRetry() {
        this.status = DeviceStatus.OPEN;
        this.currentStep = "Retry #" + this.retryCount;
        this.errorMessage = null;
        this.startedAt = 0;
        this.finishedAt = 0;
    }

    public Long getId() { return id; }
    public String getProjectTitle() { return projectTitle; }
    public String getHostname() { return hostname; }
    public String getIp() { return ip; }
    public String getSerialNumber() { return serialNumber; }
    public String getUpstreamSwitch() { return upstreamSwitch; }
    public int getUpstreamPort() { return upstreamPort; }
    public String getLocationName() { return locationName; }
    public String getVendor() { return vendor; }
    public String getModel() { return model; }
    public String getConfigProfile() { return configProfile; }
    public String getConfigMode() { return configMode; }
    public String getCustomConfig() { return customConfig; }
    public String getDefaultGatewayIp() { return defaultGatewayIp; }
    public String getLocalDnsServerIp() { return localDnsServerIp; }
    public String getLocalPrtgSensorIp() { return localPrtgSensorIp; }
    public String getNtpServer() { return ntpServer; }
    public String getSubnetMask() { return subnetMask; }
    public String getAssignedTo() { return assignedTo; }
    public DeviceStatus getStatus() { return status; }
    public String getCurrentStep() { return currentStep; }
    public String getErrorMessage() { return errorMessage; }
    public long getCreatedAt() { return createdAt; }
    public long getStartedAt() { return startedAt; }
    public long getFinishedAt() { return finishedAt; }
    public String getGeneratedConfig() { return generatedConfig; }
    public int getRetryCount() { return retryCount; }

    public void setStatus(DeviceStatus status) { this.status = status; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setGeneratedConfig(String generatedConfig) { this.generatedConfig = generatedConfig; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public void setModel(String model) { this.model = model; }
    public void setConfigProfile(String configProfile) { this.configProfile = configProfile; }
    public void setConfigMode(String configMode) { this.configMode = configMode; }
    public void setCustomConfig(String customConfig) { this.customConfig = customConfig; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void markStarted() { this.startedAt = System.currentTimeMillis(); }
    public void markFinished() { this.finishedAt = System.currentTimeMillis(); }
}
