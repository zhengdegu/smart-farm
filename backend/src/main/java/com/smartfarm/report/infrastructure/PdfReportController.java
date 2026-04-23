package com.smartfarm.report.infrastructure;

import com.smartfarm.report.application.ReportService;
import com.smartfarm.report.domain.IrrigationDailySummary;
import com.smartfarm.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class PdfReportController {

    private final ReportService reportService;

    @PostMapping("/monthly-pdf")
    public void generateMonthlyPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month,
            HttpServletResponse response) throws IOException {

        Long tenantId = SecurityUtils.currentTenantId();
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        // 收集数据
        Map<String, Object> monthlyStats = reportService.getMonthlyStats(tenantId, month);
        Map<String, Object> waterSaving = reportService.getWaterSavingComparison(tenantId, start, end, 500.0);
        Map<String, Object> alertStats = reportService.getAlertStats(tenantId, start, end);
        List<IrrigationDailySummary> dailyData = reportService.getDailyStats(tenantId, start, end, null);

        String filename = String.format("monthly_report_%s.pdf", start.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        generatePdf(response.getOutputStream(), start, monthlyStats, waterSaving, alertStats, dailyData);
    }

    private void generatePdf(OutputStream out, LocalDate month,
                             Map<String, Object> monthlyStats,
                             Map<String, Object> waterSaving,
                             Map<String, Object> alertStats,
                             List<IrrigationDailySummary> dailyData) throws IOException {

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        // 标题
        String title = String.format("智慧农业平台 — %s月度运营报告", month.format(DateTimeFormatter.ofPattern("yyyy年MM")));
        doc.add(new Paragraph(title)
                .setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        doc.add(new Paragraph(String.format("报告周期: %s ~ %s", month, month.plusMonths(1).minusDays(1)))
                .setFontSize(10).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // 1. 灌溉概览
        doc.add(new Paragraph("一、灌溉概览").setFontSize(14).setBold().setMarginTop(10));
        Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        overviewTable.addHeaderCell(createHeaderCell("总灌溉次数"));
        overviewTable.addHeaderCell(createHeaderCell("总时长(分钟)"));
        overviewTable.addHeaderCell(createHeaderCell("总用水量(升)"));
        overviewTable.addHeaderCell(createHeaderCell("自动化率"));
        overviewTable.addCell(createDataCell(String.valueOf(monthlyStats.getOrDefault("totalIrrigationCount", 0))));
        overviewTable.addCell(createDataCell(String.valueOf(monthlyStats.getOrDefault("totalDurationMin", 0))));
        overviewTable.addCell(createDataCell(String.format("%.1f", toDouble(monthlyStats.get("totalWaterLiters")))));
        overviewTable.addCell(createDataCell(String.format("%.1f%%", toDouble(monthlyStats.get("autoRate")))));
        doc.add(overviewTable.setMarginBottom(15));

        // 2. 节水效果
        doc.add(new Paragraph("二、节水效果").setFontSize(14).setBold().setMarginTop(10));
        Table waterTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        waterTable.addHeaderCell(createHeaderCell("系统用水量(升)"));
        waterTable.addHeaderCell(createHeaderCell("人工估算用水量(升)"));
        waterTable.addHeaderCell(createHeaderCell("节水率"));
        waterTable.addCell(createDataCell(String.format("%.1f", toDouble(waterSaving.get("systemWaterLiters")))));
        waterTable.addCell(createDataCell(String.format("%.1f", toDouble(waterSaving.get("manualEstimateLiters")))));
        waterTable.addCell(createDataCell(String.format("%.1f%%", toDouble(waterSaving.get("waterSavedPercent")))));
        doc.add(waterTable.setMarginBottom(15));

        // 3. 告警统计
        doc.add(new Paragraph("三、告警统计").setFontSize(14).setBold().setMarginTop(10));
        doc.add(new Paragraph(String.format("本月告警总数: %s", alertStats.getOrDefault("total", 0)))
                .setFontSize(11).setMarginBottom(10));

        // 4. 每日灌溉明细
        doc.add(new Paragraph("四、每日灌溉明细").setFontSize(14).setBold().setMarginTop(10));
        Table dailyTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1, 1, 1, 1.2f, 1, 1})).useAllAvailableWidth();
        dailyTable.addHeaderCell(createHeaderCell("日期"));
        dailyTable.addHeaderCell(createHeaderCell("棚号"));
        dailyTable.addHeaderCell(createHeaderCell("次数"));
        dailyTable.addHeaderCell(createHeaderCell("时长(分)"));
        dailyTable.addHeaderCell(createHeaderCell("用水量(升)"));
        dailyTable.addHeaderCell(createHeaderCell("自动"));
        dailyTable.addHeaderCell(createHeaderCell("手动"));

        for (var row : dailyData) {
            dailyTable.addCell(new Cell().add(new Paragraph(row.getSummaryDate().toString()).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(row.getGreenhouseNo()).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(String.valueOf(row.getIrrigationCount())).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(String.valueOf(row.getTotalDurationMin())).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(String.format("%.1f", row.getEstimatedWaterLiters())).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(String.valueOf(row.getAutoTriggerCount())).setFontSize(9)));
            dailyTable.addCell(new Cell().add(new Paragraph(String.valueOf(row.getManualTriggerCount())).setFontSize(9)));
        }
        if (dailyData.isEmpty()) {
            dailyTable.addCell(new Cell(1, 7).add(new Paragraph("本月暂无灌溉记录").setFontSize(9).setTextAlignment(TextAlignment.CENTER)));
        }
        doc.add(dailyTable.setMarginBottom(15));

        // 页脚
        doc.add(new Paragraph("— 报告由智慧农业平台自动生成 —")
                .setFontSize(9).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30));

        doc.close();
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(10).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }

    private Cell createDataCell(String text) {
        return new Cell().add(new Paragraph(text).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }
}
