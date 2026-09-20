package com.emresahin.utils;

import com.emresahin.model.SapReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ExcelHelper {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicExcelResult {
        private List<String> headers;
        private List<Map<String, Object>> rows;
        private List<SapReport> sapReports;
    }

    public static List<SapReport> excelToSapReports(String filePath) {
        try (InputStream is = new FileInputStream(new File(filePath))) {
            return parseInputStream(is).getSapReports();
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse Excel file: " + e.getMessage(), e);
        }
    }

    public static DynamicExcelResult parseInputStream(InputStream is) {
        List<String> headers = new ArrayList<>();
        List<Map<String, Object>> rowsList = new ArrayList<>();
        List<SapReport> sapReports = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return DynamicExcelResult.builder()
                        .headers(headers)
                        .rows(rowsList)
                        .sapReports(sapReports)
                        .build();
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                int lastCellNum = headerRow.getLastCellNum();
                for (int c = 0; c < lastCellNum; c++) {
                    Cell cell = headerRow.getCell(c);
                    String h = getCellValueAsString(cell);
                    if (h == null || h.trim().isEmpty()) {
                        h = "Column " + (c + 1);
                    }
                    headers.add(h);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row currentRow = sheet.getRow(r);
                if (currentRow == null || isRowEmptyOrInvalid(currentRow)) {
                    continue;
                }

                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = currentRow.getCell(c);
                    Object val = getCellValueGeneric(cell);
                    rowMap.put(headers.get(c), val);
                }
                rowsList.add(rowMap);

                try {
                    SapReport report = mapToSapReport(currentRow);
                    if (report != null && report.getYear() != null && report.getMaterialNumber() != null) {
                        sapReports.add(report);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse Excel input stream: " + e.getMessage(), e);
        }

        return DynamicExcelResult.builder()
                .headers(headers)
                .rows(rowsList)
                .sapReports(sapReports)
                .build();
    }

    private static SapReport mapToSapReport(Row currentRow) {
        SapReport report = new SapReport();
        report.setYear(getCellValueAsInteger(currentRow.getCell(0)));
        report.setMonth(getCellValueAsInteger(currentRow.getCell(1)));
        report.setMaterialNumber(getCellValueAsLong(currentRow.getCell(2)));
        report.setMaterialDescription(getCellValueAsString(currentRow.getCell(3)));
        report.setPeriod(getCellValueAsInteger(currentRow.getCell(4)));
        report.setBrand(getCellValueAsString(currentRow.getCell(5)));
        report.setSize(getCellValueAsString(currentRow.getCell(6)));
        report.setPack(getCellValueAsString(currentRow.getCell(7)));
        report.setClient(getCellValueAsString(currentRow.getCell(8)));
        report.setClientType(getCellValueAsString(currentRow.getCell(9)));
        report.setVolume(getCellValueAsDouble(currentRow.getCell(10)));
        report.setGrossSales(getCellValueAsDouble(currentRow.getCell(11)));
        report.setDiscounts(getCellValueAsDouble(currentRow.getCell(12)));
        report.setNetSales(getCellValueAsDouble(currentRow.getCell(13)));
        report.setCostOfGoodsSold(getCellValueAsDouble(currentRow.getCell(14)));
        report.setDistribution(getCellValueAsDouble(currentRow.getCell(15)));
        report.setWarehousing(getCellValueAsDouble(currentRow.getCell(16)));
        return report;
    }

    private static boolean isRowEmptyOrInvalid(Row row) {
        if (row == null) return true;
        
        Cell firstCell = row.getCell(0);
        if (firstCell == null || firstCell.getCellType() == CellType.BLANK) {
            return true;
        }
        
        CellType type = firstCell.getCellType();
        if (type == CellType.FORMULA) {
            type = firstCell.getCachedFormulaResultType();
        }
        
        if (type == CellType.STRING) {
            String val = firstCell.getStringCellValue().trim();
            if (val.equalsIgnoreCase("x") || val.isEmpty()) {
                return true;
            }
        }
        
        return false;
    }

    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numericVal = cell.getNumericCellValue();
                if (numericVal == (long) numericVal) {
                    return String.valueOf((long) numericVal);
                }
                return String.valueOf(numericVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private static Object getCellValueGeneric(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    return (long) num;
                }
                return num;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }

    private static Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        
        switch (type) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String str = cell.getStringCellValue().trim().replace(",", ".");
                    return Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private static Integer getCellValueAsInteger(Cell cell) {
        Double val = getCellValueAsDouble(cell);
        return val == null ? null : val.intValue();
    }

    private static Long getCellValueAsLong(Cell cell) {
        Double val = getCellValueAsDouble(cell);
        return val == null ? null : val.longValue();
    }
}

