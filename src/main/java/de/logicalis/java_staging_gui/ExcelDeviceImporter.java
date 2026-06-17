package de.logicalis.java_staging_gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExcelDeviceImporter {

    static final int MAX_ROWS = 500;

    static final String[] HEADERS = {
        "upstreamSwitch", "upstreamPort", "hostname", "ip",
        "serialNumber", "locationName", "assignedTo"
    };

    @Autowired
    private DeviceService deviceService;

    public ImportResult parse(InputStream in, Project project) throws IOException {
        List<String> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            // row 0 = header, data rows start at row 1.
            // check the header against the template: the mapping is based purely on the
            // column position, swapped columns would otherwise be imported wrong silently.
            Row headerRow = sheet.getRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                String got = headerRow == null ? "" : fmt.formatCellValue(headerRow.getCell(i)).trim();
                if (!HEADERS[i].equalsIgnoreCase(got)) {
                    errors.add("Column header in column " + (i + 1) + " is '" + got
                            + "', expected '" + HEADERS[i]
                            + "'. Please use the Excel template, import aborted.");
                    return new ImportResult(imported, errors);
                }
            }

            int lastDataRow = Math.min(sheet.getLastRowNum(), MAX_ROWS);

            for (int rowIdx = 1; rowIdx <= lastDataRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row, fmt)) continue;

                int humanRow = rowIdx + 1;

                String upstreamSwitch = cell(row, 0, fmt);
                String portStr = cell(row, 1, fmt);
                String hostname = cell(row, 2, fmt);
                String ip = cell(row, 3, fmt);
                String serialNumber = cell(row, 4, fmt);
                String locationName = cell(row, 5, fmt);
                String assignedTo = cell(row, 6, fmt);

                Integer port;
                try {
                    // depending on the cell format, excel also returns integers as "3.0"
                    String ps = portStr.endsWith(".0") ? portStr.substring(0, portStr.length() - 2) : portStr;
                    port = ps.isEmpty() ? null : Integer.parseInt(ps);
                } catch (NumberFormatException e) {
                    errors.add("Row " + humanRow + ": Port is not a number ('" + portStr + "').");
                    continue;
                }

                String validationErr = DeviceValidation.validate(
                        upstreamSwitch, port, hostname, ip, locationName);
                if (validationErr != null) {
                    errors.add("Row " + humanRow + ": " + validationErr);
                    continue;
                }

                if (deviceService.existsInProject(project.getProjectTitle(), hostname, ip)) {
                    errors.add("Row " + humanRow + ": Hostname or IP already exists.");
                    continue;
                }

                if (deviceService.portExistsInProject(project.getProjectTitle(), upstreamSwitch, port)) {
                    errors.add("Row " + humanRow + ": Port " + upstreamSwitch + ":" + port + " already in use.");
                    continue;
                }

                try {
                    deviceService.createAndSave(
                            upstreamSwitch, port, hostname, ip,
                            serialNumber.isEmpty() ? null : serialNumber,
                            locationName, project.getProjectTitle(),
                            project.getVendor(), project.getModel(),
                            project.getConfigProfile(),
                            "AUTO", null,
                            project.getDefaultGatewayIp(), project.getLocalDnsServerIp(),
                            project.getLocalPrtgSensorIp(), project.getNtpServer(),
                            project.getSubnetMask(),
                            assignedTo.isEmpty() ? null : assignedTo);
                    imported.add(hostname);
                } catch (Exception e) {
                    // a DB/save error in one row must not abort the whole import
                    errors.add("Row " + humanRow + ": Save failed (" + e.getMessage() + ").");
                }
            }

            if (sheet.getLastRowNum() > MAX_ROWS) {
                errors.add("The Excel file had more than " + MAX_ROWS
                        + " rows. The remaining rows were ignored. Please upload across multiple files.");
            }
        }

        return new ImportResult(imported, errors);
    }

    public byte[] buildTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Devices");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("sw-fra-01");
            example.createCell(1).setCellValue(3);
            example.createCell(2).setCellValue("sw-fra-stage-01");
            example.createCell(3).setCellValue("10.10.10.1");
            example.createCell(4).setCellValue("FCW2144L0GH");
            example.createCell(5).setCellValue("Frankfurt RZ A");
            example.createCell(6).setCellValue("Michael Kurz");

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static String cell(Row row, int idx, DataFormatter fmt) {
        Cell c = row.getCell(idx);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    private static boolean isRowEmpty(Row row, DataFormatter fmt) {
        for (Cell c : row) {
            if (!fmt.formatCellValue(c).trim().isEmpty()) return false;
        }
        return true;
    }
}
