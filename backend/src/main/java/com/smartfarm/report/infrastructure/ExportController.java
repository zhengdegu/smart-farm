package com.smartfarm.report.infrastructure;

import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/export")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "CSV/Excel 导出")
public class ExportController {

    private final ReportService reportService;

    @GetMapping("/irrigation-daily")
    @Operation(summary = "导出灌溉统计", description = "支持 xlsx/csv 格式")
    public void exportIrrigationDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String greenhouseNo,
            @RequestParam(defaultValue = "xlsx") String format,
            HttpServletResponse response) throws IOException {

        Long tenantId = SecurityUtils.currentTenantId();
        List<IrrigationDailySummary> data = reportService.getDailyStats(tenantId, start, end, greenhouseNo);

        if ("csv".equalsIgnoreCase(format)) {
            exportCsv(response, data, start, end);
        } else {
            exportExcel(response, data, start, end);
        }
    }

    private void exportCsv(HttpServletResponse response, List<IrrigationDailySummary> data,
                           LocalDate start, LocalDate end) throws IOException {
        String filename = String.format("irrigation_%s_%s.csv", start, end);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // BOM

        var out = response.getWriter();
        out.println("日期,棚号,灌溉次数,总时长(分钟),估算用水量(升),平均土壤湿度(%),自动触发,手动触发");
        for (var row : data) {
            out.printf("%s,%s,%d,%d,%.1f,%.1f,%d,%d%n",
                    row.getSummaryDate(), row.getGreenhouseNo(),
                    row.getIrrigationCount(), row.getTotalDurationMin(),
                    row.getEstimatedWaterLiters(), row.getAvgSoilMoisture() != null ? row.getAvgSoilMoisture() : 0.0,
                    row.getAutoTriggerCount(), row.getManualTriggerCount());
        }
        out.flush();
    }

    private void exportExcel(HttpServletResponse response, List<IrrigationDailySummary> data,
                             LocalDate start, LocalDate end) throws IOException {
        String filename = String.format("irrigation_%s_%s.xlsx", start, end);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("灌溉统计");

            // 表头样式
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"日期", "棚号", "灌溉次数", "总时长(分钟)", "估算用水量(升)", "平均土壤湿度(%)", "自动触发", "手动触发"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIdx = 1;
            for (var row : data) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(row.getSummaryDate().format(DateTimeFormatter.ISO_DATE));
                r.createCell(1).setCellValue(row.getGreenhouseNo());
                r.createCell(2).setCellValue(row.getIrrigationCount());
                r.createCell(3).setCellValue(row.getTotalDurationMin());
                r.createCell(4).setCellValue(row.getEstimatedWaterLiters());
                r.createCell(5).setCellValue(row.getAvgSoilMoisture() != null ? row.getAvgSoilMoisture().doubleValue() : 0);
                r.createCell(6).setCellValue(row.getAutoTriggerCount());
                r.createCell(7).setCellValue(row.getManualTriggerCount());
            }

            // 自动列宽
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(response.getOutputStream());
        }
    }
}
