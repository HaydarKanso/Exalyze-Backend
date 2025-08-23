package unit00.com.exalyze.dto;


import java.util.Map;

public class ChartDataDto {
    // This is the updated line: it no longer holds a List<Double>
    private Map<String, unit00.com.exalyze.dto.NumericSummaryDto> numericData;
    private Map<String, Map<String, Long>> categoricalFrequencies;
    private Map<String, Map<String, Double>> correlationMatrix;

    // Getters and Setters for the new structure
    public Map<String, unit00.com.exalyze.dto.NumericSummaryDto> getNumericData() {
        return numericData;
    }

    public void setNumericData(Map<String, unit00.com.exalyze.dto.NumericSummaryDto> numericData) {
        this.numericData = numericData;
    }

    public Map<String, Map<String, Long>> getCategoricalFrequencies() {
        return categoricalFrequencies;
    }

    public void setCategoricalFrequencies(Map<String, Map<String, Long>> categoricalFrequencies) {
        this.categoricalFrequencies = categoricalFrequencies;
    }

    public Map<String, Map<String, Double>> getCorrelationMatrix() {
        return correlationMatrix;
    }

    public void setCorrelationMatrix(Map<String, Map<String, Double>> correlationMatrix) {
        this.correlationMatrix = correlationMatrix;
    }
}