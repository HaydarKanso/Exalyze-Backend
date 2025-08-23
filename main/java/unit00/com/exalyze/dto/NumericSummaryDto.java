package unit00.com.exalyze.dto;
import unit00.com.exalyze.Bin;
import java.util.List;

public class NumericSummaryDto {
    private Double min;
    private Double max;
    private Double avg;
    private Double median;
    private Double stDev;
    private List<Bin> histogramBins;

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getAvg() {
        return avg;
    }

    public Double getMedian() {
        return median;
    }

    public Double getStDev() {
        return stDev;
    }

    public List<Bin> getHistogramBins() {
        return histogramBins;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public void setMedian(Double median) {
        this.median = median;
    }

    public void setStDev(Double stDev) {
        this.stDev = stDev;
    }

    public void setHistogramBins(List<Bin> histogramBins) {
        this.histogramBins = histogramBins;
    }
    // Getters and setters
    // ... (You'll need to create a simple 'Bin' class too)
}

