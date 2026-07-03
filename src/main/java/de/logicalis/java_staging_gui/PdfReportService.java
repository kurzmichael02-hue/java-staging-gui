package de.logicalis.java_staging_gui;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;

@Service
public class PdfReportService {

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color HEADER_BG = new Color(241, 245, 249);
    private static final Color MUTED = new Color(100, 116, 139);

    public byte[] buildProjectReport(Project project, List<Device> devices) {
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();
        try {

        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
        Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

        Paragraph title = new Paragraph("Staging Report", h1);
        doc.add(title);
        Paragraph sub = new Paragraph(project.getProjectTitle(),
                FontFactory.getFont(FontFactory.HELVETICA, 12, MUTED));
        sub.setSpacingAfter(16);
        doc.add(sub);

        // project meta
        doc.add(section("Project", h2));
        PdfPTable meta = kvTable();
        kvRow(meta, "Customer", nz(project.getCustomer()), normal);
        kvRow(meta, "Stager", nz(project.getStagerName()), normal);
        kvRow(meta, "Order number", nz(project.getOrderNumber()), normal);
        kvRow(meta, "Vendor / Model", nz(project.getVendor()) + " " + nz(project.getModel()), normal);
        kvRow(meta, "Profile", profileLabel(project.getConfigProfile()), normal);
        kvRow(meta, "IOS-XE Version", nz(project.getIosxeVersion()), normal);
        doc.add(meta);

        // network parameters
        doc.add(section("Network parameters", h2));
        PdfPTable net = kvTable();
        kvRow(net, "Default Gateway", nz(project.getDefaultGatewayIp()), normal);
        kvRow(net, "Subnet Mask", nz(project.getSubnetMask()), normal);
        kvRow(net, "DNS-Server", nz(project.getLocalDnsServerIp()), normal);
        kvRow(net, "NTP-Server", nz(project.getNtpServer()), normal);
        kvRow(net, "Site", nz(project.getSiteCountry()) + " / " + nz(project.getSiteId()), normal);
        kvRow(net, "Template", nz(project.getTemplateFileName()), normal);
        doc.add(net);

        // device table
        doc.add(section("Devices (" + devices.size() + ")", h2));
        PdfPTable tbl = new PdfPTable(new float[]{1.2f, 2.2f, 1.8f, 2.0f, 2.0f, 1.5f, 1.2f});
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(4);
        for (String h : new String[]{"ID", "Hostname", "IP", "Serial", "Site", "Status", "Duration"}) {
            tbl.addCell(headerCell(h, small));
        }
        for (Device d : devices) {
            tbl.addCell(bodyCell(String.valueOf(d.getId()), normal));
            tbl.addCell(bodyCell(nz(d.getHostname()), normal));
            tbl.addCell(bodyCell(nz(d.getIp()), normal));
            tbl.addCell(bodyCell(d.getSerialNumber() != null && !d.getSerialNumber().isEmpty() ? d.getSerialNumber() : "-", normal));
            tbl.addCell(bodyCell(nz(d.getLocationName()), normal));
            tbl.addCell(bodyCell(d.getStatus() != null ? d.getStatus().name() : "-", normal));
            tbl.addCell(bodyCell(duration(d), normal));
        }
        doc.add(tbl);

        Paragraph footer = new Paragraph(
                "Generated on " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())
                        + " | Staging GUI | " + devices.size() + " Devices", small);
        footer.setSpacingBefore(18);
        doc.add(footer);

        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private static Paragraph section(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        return p;
    }

    private static PdfPTable kvTable() {
        PdfPTable t = new PdfPTable(new float[]{1.2f, 3f});
        t.setWidthPercentage(100);
        return t;
    }

    private static void kvRow(PdfPTable t, String key, String value, Font font) {
        PdfPCell k = new PdfPCell(new Phrase(key, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED)));
        k.setBorder(0);
        k.setPaddingBottom(3);
        PdfPCell v = new PdfPCell(new Phrase(value, font));
        v.setBorder(0);
        v.setPaddingBottom(3);
        t.addCell(k);
        t.addCell(v);
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text.toUpperCase(), font));
        c.setBackgroundColor(HEADER_BG);
        c.setPadding(5);
        c.setBorderColor(new Color(226, 232, 240));
        return c;
    }

    private static PdfPCell bodyCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(5);
        c.setBorderColor(new Color(241, 245, 249));
        return c;
    }

    private static String duration(Device d) {
        if (d.getStartedAt() > 0 && d.getFinishedAt() > d.getStartedAt()) {
            long sec = (d.getFinishedAt() - d.getStartedAt()) / 1000;
            return sec >= 60 ? (sec / 60) + "m " + (sec % 60) + "s" : sec + "s";
        }
        return "-";
    }

    private static String profileLabel(String p) {
        if (p == null) return "-";
        return switch (p) {
            case "S" -> "S - Lite";
            case "M" -> "M - Standard";
            case "L" -> "L - Extended";
            default -> p;
        };
    }

    private static String nz(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }
}
