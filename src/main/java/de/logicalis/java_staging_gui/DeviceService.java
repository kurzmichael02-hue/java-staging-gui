package de.logicalis.java_staging_gui;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository repo;

    @Transactional
    public Device createAndSave(
            String upstreamSwitch, int upstreamPort,
            String hostname, String ip, String serialNumber,
            String locationName, String projectTitle,
            String vendor, String model,
            String configProfile,
            String configMode, String customConfig,
            String defaultGatewayIp, String localDnsServerIp,
            String localPrtgSensorIp, String ntpServer, String subnetMask,
            String assignedTo) {

        Device device = new Device(
                upstreamSwitch, upstreamPort, hostname, ip, serialNumber,
                locationName, projectTitle, vendor, model,
                configProfile, configMode, customConfig,
                defaultGatewayIp, localDnsServerIp, localPrtgSensorIp,
                ntpServer, subnetMask, assignedTo);
        return repo.save(device);
    }

    public List<Device> getByProjectTitle(String projectTitle) {
        return repo.findByProjectTitle(projectTitle);
    }

    public Device findById(long id) {
        return repo.findById(id).orElse(null);
    }

    public boolean existsInProject(String projectTitle, String hostname, String ip) {
        return repo.existsByProjectTitleAndHostname(projectTitle, hostname)
                || repo.existsByProjectTitleAndIp(projectTitle, ip);
    }

    public boolean portExistsInProject(String projectTitle, String upstreamSwitch, int upstreamPort) {
        return repo.existsByProjectTitleAndUpstreamSwitchAndUpstreamPort(
                projectTitle, upstreamSwitch, upstreamPort);
    }

    @Transactional
    public void setStatus(long id, DeviceStatus status, String step, String errorMessage) {
        Device d = repo.findById(id).orElse(null);
        if (d == null) return;
        d.setStatus(status);
        if (step != null) d.setCurrentStep(step);
        // set errorMessage explicitly (even null), so a status change overwrites an old error message
        d.setErrorMessage(errorMessage);
        if (status == DeviceStatus.IN_PROGRESS) d.markStarted();
        if (status == DeviceStatus.DONE || status == DeviceStatus.ERROR) d.markFinished();
    }

    // atomic claim OPEN->IN_PROGRESS via a conditional UPDATE. exactly one of
    // several concurrent start requests gets rowcount==1 and may proceed,
    // the rest get 0 and abort - prevents duplicate provisioning.
    @Transactional
    public boolean claimForStart(long id) {
        int updated = repo.updateStatusIfCurrent(
                id, DeviceStatus.OPEN, DeviceStatus.IN_PROGRESS,
                "Generating config...", System.currentTimeMillis());
        return updated == 1;
    }

    @Transactional
    public void setGeneratedConfig(long id, String config) {
        repo.findById(id).ifPresent(d -> d.setGeneratedConfig(config));
    }

    // retry: bump the counter + reset state to OPEN (clears error + timing).
    // resetForRetry() sets currentStep to "Retry #N", which lands in the step column.
    // only allowed from ERROR: a retry on a running or finished device would
    // bypass the claim protection or discard the measured duration.
    @Transactional
    public boolean retryDevice(long id) {
        Device d = repo.findById(id).orElse(null);
        if (d == null || d.getStatus() != DeviceStatus.ERROR) return false;
        d.incrementRetry();
        d.resetForRetry();
        return true;
    }

    @Transactional
    public boolean deleteById(long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }
}
