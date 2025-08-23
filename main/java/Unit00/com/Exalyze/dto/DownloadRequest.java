package Unit00.com.Exalyze.dto;


import java.util.Map;

public class DownloadRequest {
    private String reportContent;
    private Map<String, String> chartImages;

    // Getters and Setters
    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    public Map<String, String> getChartImages() {
        return chartImages;
    }

    public void setChartImages(Map<String, String> chartImages) {
        this.chartImages = chartImages;
    }
}