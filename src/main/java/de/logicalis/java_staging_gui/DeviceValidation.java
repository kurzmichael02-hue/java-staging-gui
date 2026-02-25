package de.logicalis.java_staging_gui;

import java.util.regex.Pattern;

public final class DeviceValidation {

    private static final Pattern HOSTNAME = Pattern.compile("^[a-zA-Z0-9-]{3,30}$");
    // upstreamSwitch ends up as the interface description in the config and as a json key
    // in the ZTP payload - only allow switch-name characters (no line break -> no injection)
    private static final Pattern UPSTREAM = Pattern.compile("^[a-zA-Z0-9.\\-]{1,50}$");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    private DeviceValidation() {}

    public static String validate(String upstreamSwitch, Integer upstreamPort,
                                  String hostname, String ip, String locationName) {
        if (upstreamSwitch == null || upstreamSwitch.isEmpty())
            return "Upstream Switch is missing.";
        if (!UPSTREAM.matcher(upstreamSwitch).matches())
            return "Upstream Switch: only letters, numbers, dot, hyphen (max. 50 characters).";
        // "general" is the reserved key in the ZTP JSON for the project-wide values -
        // a switch with this name would overwrite that block in the payload
        if ("general".equalsIgnoreCase(upstreamSwitch))
            return "Upstream Switch: 'general' is a reserved name.";
        if (upstreamPort == null || upstreamPort < 1 || upstreamPort > 48)
            return "Upstream Port must be between 1 and 48.";
        if (hostname == null || !HOSTNAME.matcher(hostname).matches())
            return "Hostname: only letters, numbers, hyphen (3-30 characters).";
        if (ip == null || !IPV4.matcher(ip).matches())
            return "IP address invalid (e.g. 10.10.10.1).";
        if (locationName == null || locationName.isEmpty())
            return "Location name is missing.";
        if (locationName.length() > 200 || hasControlChars(locationName))
            return "Location name contains invalid characters (no line breaks/control characters).";
        return null;
    }

    // location ends up raw in the cisco config (snmp-server location, banner) -
    // line breaks/control characters could otherwise smuggle in additional commands
    static boolean hasControlChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || (c < 0x20 && c != '\t')) return true;
        }
        return false;
    }

    // for the project-wide network fields (gateway, subnet mask, dns, prtg, dhcp)
    // in step 3 - these otherwise end up unchecked in the switch config
    public static boolean isIpv4(String s) {
        return s != null && IPV4.matcher(s).matches();
    }
}
