package de.logicalis.java_staging_gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProjectDefaultsController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/project-defaults")
    public String showDefaults(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";
        model.addAttribute("project", project);
        return "project-defaults";
    }

    @PostMapping("/project-defaults")
    public String saveDefaults(
            @RequestParam String defaultGatewayIp,
            @RequestParam String localDnsServerIp,
            @RequestParam String ntpServer,
            @RequestParam(required = false) String localPrtgSensorIp,
            @RequestParam(required = false) String localDhcpServerIps,
            @RequestParam String siteCountry,
            @RequestParam String siteId,
            @RequestParam String templateFileName,
            @RequestParam String iosxeVersion,
            @RequestParam String subnetMask,
            HttpSession session,
            Model model) {

        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        // apply first, so the form keeps the entries on an error
        project.setDefaultGatewayIp(defaultGatewayIp.trim());
        project.setLocalDnsServerIp(localDnsServerIp.trim());
        project.setNtpServer(ntpServer.trim());
        if (localPrtgSensorIp != null) project.setLocalPrtgSensorIp(localPrtgSensorIp.trim());
        if (localDhcpServerIps != null) project.setLocalDhcpServerIps(localDhcpServerIps.trim());
        project.setSiteCountry(siteCountry.trim());
        project.setSiteId(siteId.trim());
        project.setTemplateFileName(templateFileName.trim());
        project.setIosxeVersion(iosxeVersion.trim());
        project.setSubnetMask(subnetMask.trim());

        // validate the network fields - a typo here otherwise lands unchecked in the
        // generated switch config. NTP stays free (can also be a hostname).
        String err = null;
        if (!DeviceValidation.isIpv4(project.getDefaultGatewayIp()))
            err = "Default Gateway is not a valid IPv4 address.";
        else if (!DeviceValidation.isIpv4(project.getSubnetMask()))
            err = "Subnet Mask is not a valid IPv4 address.";
        else if (!DeviceValidation.isIpv4(project.getLocalDnsServerIp()))
            err = "Local DNS Server IP is not a valid IPv4 address.";
        else if (project.getLocalPrtgSensorIp() != null && !project.getLocalPrtgSensorIp().isBlank()
                && !DeviceValidation.isIpv4(project.getLocalPrtgSensorIp()))
            err = "Local PRTG / Syslog IP is not a valid IPv4 address.";
        else if (project.getLocalDhcpServerIps() != null && !project.getLocalDhcpServerIps().isBlank()) {
            for (String part : project.getLocalDhcpServerIps().split(",")) {
                if (!part.isBlank() && !DeviceValidation.isIpv4(part.trim())) {
                    err = "Local DHCP Server IP invalid: " + part.trim();
                    break;
                }
            }
        }

        if (err != null) {
            model.addAttribute("project", project);
            model.addAttribute("error", err);
            return "project-defaults";
        }

        projectService.save(project);

        return "redirect:/device";
    }
}
