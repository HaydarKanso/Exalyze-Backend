package com.unit00.exalyze.controller;

import com.unit00.exalyze.dto.ChartDataDto;
import com.unit00.exalyze.service.AIAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class ExcelAnalysisController {

    private final AIAnalysisService analysisService;

    public ExcelAnalysisController(AIAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // ... constructor and other methods

    @PostMapping("/chart-data")
    public ResponseEntity<ChartDataDto> getChartData(@RequestBody List<Map<String, Object>> excelData) {
        ChartDataDto chartData = analysisService.getChartData(excelData);
        return ResponseEntity.ok(chartData);
    }
}