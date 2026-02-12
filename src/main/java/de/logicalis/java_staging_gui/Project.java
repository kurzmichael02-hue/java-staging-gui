package de.logicalis.java_staging_gui;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stagerName;
    private String customer;

    // projectTitle is the business identifier (natural key) - unique + indexed.
    // in the wizard the stager types it in, and the system finds the project by it.
    @Column(unique = true, nullable = false)
    private String projectTitle;

    private String orderNumber;

    private String defaultGatewayIp;
    private String localDnsServerIp;
    private String localPrtgSensorIp;
    private String localDhcpServerIps;
    private String ntpServer;
    private String subnetMask;
    private String siteCountry;
    private String siteId;
    // jinja2 template on the ztp server, the name must exist exactly like this under /templates
    private String templateFileName;
    private String iosxeVersion;

    private long createdAt;

    private String vendor;
    private String model;
    private String configProfile;

    protected Project() {
        // for JPA
    }

    public Project(String stagerName, String customer, String projectTitle, String orderNumber) {
        this.stagerName = stagerName;
        this.customer = customer;
        this.projectTitle = projectTitle;
        this.orderNumber = orderNumber;
        this.createdAt = System.currentTimeMillis();

        this.vendor = "Cisco";
        this.model = "";
        this.configProfile = "S";
    }

    public Long getId() { return id; }
    public String getStagerName() { return stagerName; }
    public String getCustomer() { return customer; }
    public String getProjectTitle() { return projectTitle; }
    public String getOrderNumber() { return orderNumber; }
    public long getCreatedAt() { return createdAt; }

    public void setStagerName(String v) { this.stagerName = v; }
    public void setCustomer(String v) { this.customer = v; }
    public void setOrderNumber(String v) { this.orderNumber = v; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getConfigProfile() { return configProfile; }
    public void setConfigProfile(String configProfile) { this.configProfile = configProfile; }

    public String getDefaultGatewayIp() { return defaultGatewayIp; }
    public void setDefaultGatewayIp(String v) { this.defaultGatewayIp = v; }
    public String getLocalDnsServerIp() { return localDnsServerIp; }
    public void setLocalDnsServerIp(String v) { this.localDnsServerIp = v; }
    public String getLocalPrtgSensorIp() { return localPrtgSensorIp; }
    public void setLocalPrtgSensorIp(String v) { this.localPrtgSensorIp = v; }
    public String getLocalDhcpServerIps() { return localDhcpServerIps; }
    public void setLocalDhcpServerIps(String v) { this.localDhcpServerIps = v; }
    public String getNtpServer() { return ntpServer; }
    public void setNtpServer(String v) { this.ntpServer = v; }
    public String getSiteCountry() { return siteCountry; }
    public void setSiteCountry(String v) { this.siteCountry = v; }
    public String getSiteId() { return siteId; }
    public void setSiteId(String v) { this.siteId = v; }
    public String getTemplateFileName() { return templateFileName; }
    public void setTemplateFileName(String v) { this.templateFileName = v; }
    public String getIosxeVersion() { return iosxeVersion; }
    public void setIosxeVersion(String v) { this.iosxeVersion = v; }

    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String v) { this.subnetMask = v; }
}
