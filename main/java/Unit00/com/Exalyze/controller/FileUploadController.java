package Unit00.com.Exalyze.controller;


import Unit00.com.Exalyze.service.AIAnalysisService;
import Unit00.com.Exalyze.service.ExcelService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final ExcelService excelService;
    private final AIAnalysisService aiAnalysisService;

    public FileUploadController(ExcelService excelService, AIAnalysisService aiAnalysisService) {
        this.excelService = excelService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file uploaded!");
        }

        try {
            List<Map<String, Object>> data;
            String filename = file.getOriginalFilename();

            if (filename != null && filename.toLowerCase().endsWith(".csv")) {
                data = excelService.readCsvData(file);
            } else {
                data = excelService.readExcelData(file);
            }

            String analysis = aiAnalysisService.analyzeData(data);

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "analysis", analysis
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to process file: " + e.getMessage());
        }
    }

    @PostMapping("/download/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdf(@RequestBody Map<String, Object> payload) {
        try {
            String reportContent = (String) payload.get("reportContent");
            Map<String, String> chartImages = (Map<String, String>) payload.get("chartImages");

            byte[] pdfBytes = aiAnalysisService.generatePdfReport(reportContent, chartImages);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analysis_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/download/excel")
    public ResponseEntity<ByteArrayResource> downloadExcel(@RequestBody List<Map<String, Object>> data) {
        try {
            byte[] excelBytes = excelService.generateExcelFile(data);
            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=raw_data.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(excelBytes.length)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}