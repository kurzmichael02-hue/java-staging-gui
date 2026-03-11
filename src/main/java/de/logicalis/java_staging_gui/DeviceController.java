package de.logicalis.java_staging_gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@Controller
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ZtpClient ztpClient;

    @Autowired
    private ConfigGenerator configGenerator;

    @Autowired
    private ExcelDeviceImporter excelImporter;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private PdfReportService pdfReportService;

    @Value("${app.stagers:}")
    private String stagersConfig;

    private List<String> stagers() {
        if (stagersConfig == null || stagersConfig.isBlank()) return List.of();
        return Arrays.stream(stagersConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    @GetMapping("/device")
    public String device(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        if (project.getDefaultGatewayIp() == null || project.getDefaultGatewayIp().trim().isEmpty())
            return "redirect:/project-defaults";
        model.addAttribute("project", project);
        model.addAttribute("stagers", stagers());
        return "device";
    }

    @GetMapping("/devices")
    public String showDevices(Model model, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        List<Device> devices = deviceService.getByProjectTitle(project.getProjectTitle());

        // auto-refresh only while provisioning is actually running (IN_PROGRESS).
        // OPEN does not count: nothing changes on its own there, and the refresh
        // would only throw away the search field and checkbox selection.
        boolean hasActive = devices.stream().anyMatch(d ->
                d.getStatus() == DeviceStatus.IN_PROGRESS);

        model.addAttribute("project", project);
        model.addAttribute("devices", devices);
        model.addAttribute("hasActive", hasActive);
        return "devices";
    }

    @PostMapping("/device")
    public String saveDevice(
            @RequestParam(required = false) String upstreamSwitch,
            @RequestParam(required = false) Integer upstreamPort,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) String configMode,
            @RequestParam(required = false) String customConfig,
            @RequestParam(required = false) String assignedTo,
            HttpSession session,
            Model model) {

        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        model.addAttribute("project", project);
        // also needed for the error paths, otherwise the Stager dropdown
        // is empty after a validation error
        model.addAttribute("stagers", stagers());

        upstreamSwitch = trim(upstreamSwitch);
        hostname = trim(hostname);
        ip = trim(ip);
        locationName = trim(locationName);
        serialNumber = trim(serialNumber);
        customConfig = trim(customConfig);
        assignedTo = trim(assignedTo);

        String validationError = DeviceValidation.validate(upstreamSwitch, upstreamPort, hostname, ip, locationName);
        if (validationError != null) {
            model.addAttribute("error", validationError);
            return "device";
        }

        // in CUSTOM mode the free text is sent 1:1 to the switch. an empty
        // custom config would go out as an empty config and still mark the device
        // as "provisioning sent" - misleading. so block it here.
        if ("CUSTOM".equals(configMode) && (customConfig == null || customConfig.isBlank())) {
            model.addAttribute("error", "Custom config is empty. Please enter a config or switch to Auto.");
            return "device";
        }

        if (deviceService.existsInProject(project.getProjectTitle(), hostname, ip)) {
            model.addAttribute("error", "Hostname or IP already exists in the project!");
            return "device";
        }

        if (deviceService.portExistsInProject(project.getProjectTitle(), upstreamSwitch, upstreamPort)) {
            model.addAttribute("error", "This port is already in use!");
            return "device";
        }

        Device created;
        try {
            created = deviceService.createAndSave(
                    upstreamSwitch, upstreamPort, hostname, ip, serialNumber,
                    locationName, project.getProjectTitle(),
                    project.getVendor(), project.getModel(),
                    project.getConfigProfile(),
                    configMode, customConfig,
                    project.getDefaultGatewayIp(), project.getLocalDnsServerIp(),
                    project.getLocalPrtgSensorIp(), project.getNtpServer(),
                    project.getSubnetMask(), assignedTo
            );
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // the DB unique constraint caught a duplicate created in parallel
            // (race between the exists check above and the insert)
            model.addAttribute("error", "Hostname, IP or port was just created in parallel. Please check your input.");
            return "device";
        }
        auditService.logDevice(project, created, "DEVICE_CREATED");

        return "redirect:/devices";
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    @GetMapping("/devices/bulk-import")
    public String bulkImportForm(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        if (project.getDefaultGatewayIp() == null || project.getDefaultGatewayIp().trim().isEmpty())
            return "redirect:/project-defaults";
        model.addAttribute("project", project);
        return "bulk-import";
    }

    @PostMapping("/devices/bulk-import")
    public String bulkImportUpload(@RequestParam("file") MultipartFile file,
                                   HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        model.addAttribute("project", project);

        if (file == null || file.isEmpty()) {
            model.addAttribute("error", "No file selected.");
            return "bulk-import";
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            model.addAttribute("error", "Only .xlsx is supported.");
            return "bulk-import";
        }

        try {
            ImportResult result = excelImporter.parse(file.getInputStream(), project);
            auditService.logBulk(project, "BULK_IMPORTED",
                    result.imported().size() + " Devices",
                    "from " + file.getOriginalFilename() + " (" + result.errors().size() + " errors)");
            model.addAttribute("result", result);
            return "bulk-import-result";
        } catch (IOException e) {
            model.addAttribute("error", "File could not be read: " + e.getMessage());
            return "bulk-import";
        }
    }

    @GetMapping("/devices/bulk-import/template")
    public ResponseEntity<byte[]> bulkImportTemplate() throws IOException {
        byte[] xlsx = excelImporter.buildTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "staging-gui-template.xlsx");
        return new ResponseEntity<>(xlsx, headers, HttpStatus.OK);
    }

    @GetMapping("/devices/export.csv")
    public ResponseEntity<byte[]> exportCsv(HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) {
            HttpHeaders redirect = new HttpHeaders();
            redirect.add(HttpHeaders.LOCATION, "/project");
            return new ResponseEntity<>(redirect, HttpStatus.FOUND);
        }

        List<Device> devices = deviceService.getByProjectTitle(project.getProjectTitle());
        byte[] csv = buildDeviceCsv(devices).getBytes(StandardCharsets.UTF_8);

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String filename = sanitizeFilename(project.getProjectTitle()) + "-devices-" + stamp + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @GetMapping("/devices/report.pdf")
    public ResponseEntity<byte[]> exportPdf(HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) {
            HttpHeaders redirect = new HttpHeaders();
            redirect.add(HttpHeaders.LOCATION, "/project");
            return new ResponseEntity<>(redirect, HttpStatus.FOUND);
        }

        List<Device> devices = deviceService.getByProjectTitle(project.getProjectTitle());
        byte[] pdf = pdfReportService.buildProjectReport(project, devices);

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String filename = sanitizeFilename(project.getProjectTitle()) + "-report-" + stamp + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private static String buildDeviceCsv(List<Device> devices) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,hostname,ip,serialNumber,upstreamSwitch,upstreamPort,profile,mode,stager,")
          .append("status,step,error,created,started,done,duration_sec\n");

        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Device d : devices) {
            sb.append(d.getId()).append(',');
            sb.append(csvEscape(d.getHostname())).append(',');
            sb.append(csvEscape(d.getIp())).append(',');
            sb.append(csvEscape(d.getSerialNumber())).append(',');
            sb.append(csvEscape(d.getUpstreamSwitch())).append(',');
            sb.append(d.getUpstreamPort()).append(',');
            sb.append(csvEscape(d.getConfigProfile())).append(',');
            sb.append(csvEscape(d.getConfigMode())).append(',');
            sb.append(csvEscape(d.getAssignedTo())).append(',');
            sb.append(d.getStatus() != null ? d.getStatus().name() : "").append(',');
            sb.append(csvEscape(d.getCurrentStep())).append(',');
            sb.append(csvEscape(d.getErrorMessage())).append(',');
            sb.append(d.getCreatedAt() > 0 ? dt.format(new Date(d.getCreatedAt())) : "").append(',');
            sb.append(d.getStartedAt() > 0 ? dt.format(new Date(d.getStartedAt())) : "").append(',');
            sb.append(d.getFinishedAt() > 0 ? dt.format(new Date(d.getFinishedAt())) : "").append(',');
            long dur = (d.getStartedAt() > 0 && d.getFinishedAt() > d.getStartedAt())
                    ? (d.getFinishedAt() - d.getStartedAt()) / 1000 : 0;
            sb.append(dur).append('\n');
        }
        return sb.toString();
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        // CSV formula injection: a field like =cmd|... would be executed as a formula
        // when opened in excel/calc. neutralize a leading = + - @ with '.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String sanitizeFilename(String name) {
        return name == null ? "export" : name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    @PostMapping("/device/start")
    public String startDevice(@RequestParam long id, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        startSingleDevice(id, project);
        return "redirect:/devices";
    }

    @PostMapping("/devices/bulk-start")
    public String bulkStartDevices(@RequestParam(value = "ids", required = false) List<Long> ids,
                                   HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        if (ids == null || ids.isEmpty()) return "redirect:/devices";

        // sequential, not parallel - the ZTP server flask often cannot handle
        // concurrent posts because it writes the data.json synchronously
        int started = 0;
        for (Long id : ids) {
            if (startSingleDevice(id, project)) started++;
        }
        auditService.logBulk(project, "BULK_STARTED",
                started + " Devices", "of " + ids.size() + " selected");
        return "redirect:/devices";
    }

    private boolean startSingleDevice(long id, Project project) {
        Device device = deviceService.findById(id);
        if (device == null) return false;
        // does the device belong to the currently active project? otherwise someone could
        // trigger devices of another project via the session using a foreign id (IDOR)
        if (!project.getProjectTitle().equals(device.getProjectTitle())) return false;
        // atomic claim OPEN->IN_PROGRESS: only one concurrent request wins, otherwise
        // the same switch could be provisioned twice (race across separate tx).
        if (!deviceService.claimForStart(id)) return false;
        device = deviceService.findById(id);
        if (device == null) return false;

        String config;
        if ("CUSTOM".equals(device.getConfigMode())) {
            config = device.getCustomConfig() != null ? device.getCustomConfig() : "";
            // same check as when recording: an empty custom config must not
            // pass as "provisioning sent" (affects e.g. the bulk import path)
            if (config.isBlank()) {
                deviceService.setStatus(id, DeviceStatus.ERROR, "Error during send", "Custom config is empty");
                auditService.logDevice(project, device, "DEVICE_FAILED", "Custom config is empty");
                return false;
            }
        } else {
            config = configGenerator.generate(device);
        }

        deviceService.setGeneratedConfig(id, config);
        deviceService.setStatus(id, DeviceStatus.IN_PROGRESS, "Config created", null);
        deviceService.setStatus(id, DeviceStatus.IN_PROGRESS, "Sending data to ZTP...", null);

        boolean ok = ztpClient.sendProvisioningData(project, device);

        if (ok) {
            deviceService.setStatus(id, DeviceStatus.IN_PROGRESS, "Provisioning data sent", null);
            auditService.logDevice(project, device, "DEVICE_STARTED", "ZTP request sent");
        } else {
            deviceService.setStatus(id, DeviceStatus.ERROR, "Error during send", "ZTP server not reachable");
            auditService.logDevice(project, device, "DEVICE_FAILED", "ZTP server not reachable");
        }
        return ok;
    }

    @PostMapping("/device/retry")
    public String retryDevice(@RequestParam long id, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        Device device = deviceService.findById(id);
        if (device == null || !project.getProjectTitle().equals(device.getProjectTitle()))
            return "redirect:/devices";

        // retryDevice() increments the counter and resets to OPEN -
        // but only from the status ERROR, otherwise nothing happens (no log entry).
        if (deviceService.retryDevice(id)) {
            auditService.logDevice(project, device, "DEVICE_RETRIED", "Attempt #" + (device.getRetryCount() + 1));
        }

        return "redirect:/devices";
    }

    @GetMapping("/device/config")
    public String showConfig(@RequestParam long id, HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        Device device = deviceService.findById(id);
        if (device == null || !project.getProjectTitle().equals(device.getProjectTitle()))
            return "redirect:/devices";

        String config;
        if (device.getGeneratedConfig() != null && !device.getGeneratedConfig().trim().isEmpty()) {
            config = device.getGeneratedConfig();
        } else {
            if ("CUSTOM".equals(device.getConfigMode())) {
                config = device.getCustomConfig() != null ? device.getCustomConfig() : "";
            } else {
                config = configGenerator.generate(device);
            }
        }

        model.addAttribute("device", device);
        model.addAttribute("config", config);
        return "config";
    }

    @PostMapping("/device/finish")
    public String finishDevice(@RequestParam long id, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        Device device = deviceService.findById(id);
        if (device == null || !project.getProjectTitle().equals(device.getProjectTitle()))
            return "redirect:/devices";
        deviceService.setStatus(id, DeviceStatus.DONE, "Done", null);
        auditService.logDevice(project, device, "DEVICE_FINISHED");
        return "redirect:/devices";
    }

    @PostMapping("/device/error")
    public String errorDevice(@RequestParam long id, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        Device device = deviceService.findById(id);
        if (device == null || !project.getProjectTitle().equals(device.getProjectTitle()))
            return "redirect:/devices";
        deviceService.setStatus(id, DeviceStatus.ERROR, "Error", "Provisioning failed");
        auditService.logDevice(project, device, "DEVICE_MARKED_ERROR", "Manually marked as error");
        return "redirect:/devices";
    }

    @PostMapping("/device/delete")
    public String deleteDevice(@RequestParam long id, HttpSession session) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        Device device = deviceService.findById(id);
        if (device == null || !project.getProjectTitle().equals(device.getProjectTitle()))
            return "redirect:/devices";
        auditService.logDevice(project, device, "DEVICE_DELETED");
        deviceService.deleteById(id);
        return "redirect:/devices";
    }

    @PostMapping("/project/reset")
    public String resetProject(HttpSession session) {
        session.invalidate();
        return "redirect:/project";
    }
}