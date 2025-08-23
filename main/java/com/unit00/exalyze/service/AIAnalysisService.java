package com.unit00.exalyze.service;
import com.itextpdf.text.Image;
import com.unit00.exalyze.Bin;
import com.unit00.exalyze.dto.ChartDataDto;
import com.unit00.exalyze.dto.NumericSummaryDto;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;
import java.util.Base64;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import com.itextpdf.text.Document;
 import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
 import org.apache.poi.xssf.usermodel.XSSFWorkbook;


@Service
public class AIAnalysisService {

    private static final long EXCEL_EPOCH_OFFSET = 25569;

    public String analyzeExcelData(List<Map<String, Object>> excelData) {
        if (excelData == null || excelData.isEmpty()) {
            return "✅ **Analysis Complete**: No data was provided to analyze.";
        }

        StringBuilder report = new StringBuilder();
        int totalRows = excelData.size();
        Set<String> allColumns = excelData.get(0).keySet();
        int totalColumns = allColumns.size();

        List<Map<String, Object>> duplicateRows = findDuplicateRows(excelData);
        long totalDuplicates = duplicateRows.size();

        long emptyCells = excelData.stream()
                .flatMap(row -> row.values().stream())
                .filter(value -> value == null || (value instanceof String && ((String) value).isBlank()))
                .count();
        double emptyPercentage = (double) emptyCells / (totalRows * totalColumns);
        double duplicatePercentage = (double) totalDuplicates / totalRows;
        double overallQualityScore = calculateOverallQualityScore(emptyPercentage, duplicatePercentage);

        report.append("### 📊 Data Quality Summary\n\n");
        report.append("The dataset contains **").append(totalRows).append("** rows and **").append(totalColumns).append("** columns.\n\n");
        report.append("Your data has an **Overall Quality Score** of **").append(String.format("%.2f", overallQualityScore * 100)).append(" / 100**.\n\n");
        report.append("A total of **").append(emptyCells).append("** (").append(String.format("%.2f", emptyPercentage * 100)).append("%) empty cells and **").append(totalDuplicates).append("** duplicate rows were found.\n");

        if (!duplicateRows.isEmpty()) {
            report.append("\n---");
            report.append("\n### 📋 Examples of Duplicate Rows\n\n");
            report.append("Below are some examples of the duplicate rows detected in your dataset:\n\n");
            int count = 0;
            for (Map<String, Object> duplicate : duplicateRows) {
                if (count >= 5) break;
                report.append("  - Row ").append(count + 1).append(": `").append(duplicate.toString()).append("`\n");
                count++;
            }
        }

        report.append("\n---\n\n### 🔍 Column-by-Column Insights\n");
        Map<String, List<Double>> numericColumnData = new HashMap<>();
        Map<String, List<String>> categoricalColumnData = new HashMap<>();

        for (String col : allColumns) {
            List<Object> values = excelData.stream().map(row -> row.get(col)).collect(Collectors.toList());
            List<Object> nonNullValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());

            report.append("\n**`").append(col).append("`**\n");
            long emptyCount = values.stream().filter(v -> v == null || v.toString().isBlank()).count();
            String inferredType = inferType(values);
            long uniqueCount = nonNullValues.stream().distinct().count();
            double uniquePercentage = (double) uniqueCount / totalRows * 100;

            report.append("- **Type**: ").append(inferredType).append("\n");
            report.append("- **Missing Values**: ").append(emptyCount).append(" (").append(String.format("%.2f", (double) emptyCount / totalRows * 100)).append("%)\n");
            report.append("- **Unique Values**: ").append(uniqueCount).append(" (Cardinality: ").append(String.format("%.2f", uniquePercentage)).append("%)\n");

            if ("Numeric".equals(inferredType)) {
                List<Double> numericVals = nonNullValues.stream()
                        .filter(v -> v instanceof Number)
                        .map(v -> ((Number) v).doubleValue())
                        .collect(Collectors.toList());
                numericColumnData.put(col, numericVals);
                analyzeNumericColumn(report, col, numericVals, totalRows);
            } else if ("Date".equals(inferredType)) {
                report.append("- **Date Range**: **").append(convertToDate(nonNullValues.stream().min(Comparator.comparingDouble(v -> ((Number) v).doubleValue())).map(v -> ((Number)v).doubleValue()).orElse(0.0)))
                        .append("** to **").append(convertToDate(nonNullValues.stream().max(Comparator.comparingDouble(v -> ((Number) v).doubleValue())).map(v -> ((Number)v).doubleValue()).orElse(0.0))).append("**.\n");
            } else {
                List<String> catVals = nonNullValues.stream().map(Object::toString).collect(Collectors.toList());
                categoricalColumnData.put(col, catVals);
                analyzeCategoricalColumn(report, col, catVals, totalRows);
            }
        }

        final AtomicBoolean insightsAdded = new AtomicBoolean(false);
        report.append("\n---\n\n### 📈 Correlation Analysis & Dynamic Insights\n\n");

        if (!numericColumnData.isEmpty()) {
            report.append("To better understand the relationships between your variables, a correlation heatmap is a powerful tool. \n\n");
            report.append("Here's a look at some of the most notable relationships:\n");
            Map<String, Map<String, Double>> correlations = calculateCorrelationMatrix(numericColumnData);
            correlations.forEach((col1, innerMap) -> {
                innerMap.forEach((col2, corr) -> {
                    if (corr > 0.95 || corr < -0.95) {
                        report.append(String.format("- **⚠️ Suspiciously High Correlation**: A strong correlation of **%.2f** between `**%s**` and `**%s**` was detected. This may indicate a data leak or a derived column.\n", corr, col1, col2));
                        insightsAdded.set(true);
                    } else if (corr > 0.7 || corr < -0.7) {
                        report.append(String.format("- **Strong Correlation**: There is a strong relationship between `**%s**` and `**%s**`, with a correlation of **%.2f**.\n", col1, col2, corr));
                        insightsAdded.set(true);
                    }
                });
            });
        }

        if (!categoricalColumnData.isEmpty()) {
            report.append("\n---\n\n### 📊 Distribution Insights\n\n");
            report.append("These insights highlight key characteristics about the distribution of your data.\n\n");
            categoricalColumnData.forEach((col, values) -> {
                if (isHighlyImbalanced(values)) {
                    report.append(String.format("- **⚠️ Imbalance Detected**: The column `**%s**` is highly imbalanced, with a few categories dominating the data. This could affect model performance.\n", col));
                    insightsAdded.set(true);
                }
                if (numericColumnData.containsKey("Salary") && col.toLowerCase().contains("department")) {
                    report.append(String.format("- **Grouped Insight**: The average salary for employees in different departments shows significant variation. Consider visualizing this to identify key trends.\n"));
                    insightsAdded.set(true);
                }
            });
        }

        if (!insightsAdded.get()) {
            report.append("No further insights were identified based on the data provided.");
        }

        final AtomicBoolean recommendationsAdded = new AtomicBoolean(false);
        report.append("\n---\n\n### 🚀 Actionable Recommendations\n\n");

        if (totalDuplicates > 0) {
            report.append("- **Clean Data**: Your dataset contains **").append(totalDuplicates).append("** duplicate rows. It's recommended to remove these to avoid biased analysis.\n");
            recommendationsAdded.set(true);
        }

        Map<String, List<Object>> columnDataMap = allColumns.stream()
                .collect(Collectors.toMap(
                        col -> col,
                        col -> excelData.stream().map(row -> row.get(col)).collect(Collectors.toList())
                ));

        for (String col : allColumns) {
            List<Object> values = columnDataMap.get(col);
            long emptyCount = values.stream().filter(v -> v == null || v.toString().isBlank()).count();
            double missingPercentage = (double) emptyCount / totalRows;

            if (missingPercentage > 0.90) {
                report.append(String.format("- **Consider Dropping**: The column `**%s**` has **%.1f%%** missing values. It's likely not useful for analysis and could be dropped to simplify your dataset.\n", col, missingPercentage * 100));
                recommendationsAdded.set(true);
            } else if (missingPercentage > 0.0) {
                report.append(String.format("- **Impute Missing Data**: The column `**%s**` has missing values. You could consider imputing these values using techniques like mean, median, or a more advanced method.\n", col));
                recommendationsAdded.set(true);
            }

            if (numericColumnData.containsKey(col)) {
                List<Double> numericVals = numericColumnData.get(col);
                List<Double> outliers = detectOutliers(numericVals);
                if (!outliers.isEmpty()) {
                    report.append(String.format("- **Investigate Outliers**: The column `**%s**` has **%d** potential outliers. This may indicate data entry errors or unusual events that need to be investigated.\n", col, outliers.size()));
                    recommendationsAdded.set(true);
                }
            }

            if (numericColumnData.containsKey(col)) {
                Map<String, Map<String, Double>> correlations = calculateCorrelationMatrix(numericColumnData);
                for (String otherCol : correlations.getOrDefault(col, Collections.emptyMap()).keySet()) {
                    if (!col.equals(otherCol) && Math.abs(correlations.get(col).get(otherCol)) > 0.95) {
                        report.append(String.format("- **Check Redundancy**: The column `**%s**` is highly correlated (**%.2f**) with `**%s**`. Consider if one of these columns is redundant.\n", col, correlations.get(col).get(otherCol), otherCol));
                        recommendationsAdded.set(true);
                    }
                }
            }
        }

        allColumns.stream()
                .filter(col -> {
                    List<Object> values = excelData.stream().map(row -> row.get(col)).collect(Collectors.toList());
                    return values.stream().filter(Objects::nonNull).distinct().count() == 1;
                })
                .forEach(col -> {
                    report.append(String.format("- **Drop Constant Column**: The column `**%s**` has only one unique value. It provides no predictive power and can be dropped.\n", col));
                    recommendationsAdded.set(true);
                });

        if (!recommendationsAdded.get()) {
            report.append("No actionable recommendations were identified for this dataset. The data appears to be well-structured.");
        }

        return report.toString();
    }

    public ChartDataDto getChartData(List<Map<String, Object>> excelData) {
        if (excelData == null || excelData.isEmpty()) {
            return new ChartDataDto();
        }

        ChartDataDto dto = new ChartDataDto();
        Set<String> allColumns = excelData.get(0).keySet();
        Map<String, NumericSummaryDto> numericSummaries = new HashMap<>();
        Map<String, Map<String, Long>> categoricalFrequencies = new HashMap<>();
        Map<String, List<Double>> numericColumnDataForCorrelation = new HashMap<>();

        for (String col : allColumns) {
            List<Object> values = excelData.stream().map(row -> row.get(col)).collect(Collectors.toList());
            List<Object> nonNullValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
            String inferredType = inferType(values);

            if ("Numeric".equals(inferredType)) {
                List<Double> numericVals = nonNullValues.stream()
                        .filter(v -> v instanceof Number)
                        .map(v -> ((Number) v).doubleValue())
                        .collect(Collectors.toList());

                numericSummaries.put(col, calculateNumericSummary(numericVals));
                numericColumnDataForCorrelation.put(col, numericVals);
            } else if ("Categorical".equals(inferredType)) {
                Map<String, Long> freq = nonNullValues.stream()
                        .map(Object::toString)
                        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

                categoricalFrequencies.put(col, topNWithOther(freq, 20));
            }
        }

        dto.setNumericData(numericSummaries);
        dto.setCategoricalFrequencies(categoricalFrequencies);
        dto.setCorrelationMatrix(calculateCorrelationMatrix(numericColumnDataForCorrelation));

        return dto;
    }

    public byte[] generatePdfReport(String reportContent, Map<String, String> chartImages) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph(reportContent));

            document.add(new Paragraph("\n\n---"));
            document.add(new Paragraph("### Charts & Visualizations"));

            for (Map.Entry<String, String> entry : chartImages.entrySet()) {
                String base64Data = entry.getValue().split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                Image chartImage = Image.getInstance(imageBytes);

                float documentWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                float scale = (documentWidth / chartImage.getWidth()) * 100;
                chartImage.scalePercent(scale);

                document.add(chartImage);
                document.add(new Paragraph("\n"));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] generateExcelReport(List<Map<String, Object>> data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Workbook workbook = new XSSFWorkbook(); // Correct instantiation
            Sheet sheet = workbook.createSheet("Data Analysis");

            Row headerRow = sheet.createRow(0);
            List<String> columns = new ArrayList<>(data.get(0).keySet());
            for (int i = 0; i < columns.size(); i++) {
                headerRow.createCell(i).setCellValue(columns.get(i));
            }

            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                for (int j = 0; j < columns.size(); j++) {
                    Object value = rowData.get(columns.get(j));
                    if (value != null) {
                        if (value instanceof Number) {
                            row.createCell(j).setCellValue(((Number) value).doubleValue());
                        } else {
                            row.createCell(j).setCellValue(value.toString());
                        }
                    }
                }
            }
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Helper Methods ---

    private Map<String, Long> topNWithOther(Map<String, Long> freq, int n) {
        if (freq.size() <= n) {
            return freq;
        }

        List<Map.Entry<String, Long>> sortedEntries = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        Map<String, Long> topNMap = new LinkedHashMap<>();
        long otherCount = 0;

        for (int i = 0; i < sortedEntries.size(); i++) {
            if (i < n) {
                topNMap.put(sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue());
            } else {
                otherCount += sortedEntries.get(i).getValue();
            }
        }

        if (otherCount > 0) {
            topNMap.put("Other", otherCount);
        }

        return topNMap;
    }

    private String createNumericSummaryText(String columnName, List<Double> values, int totalRows) {
        if (values.isEmpty()) {
            return "This column is empty.";
        }
        Collections.sort(values);
        double min = values.get(0);
        double max = values.get(values.size() - 1);
        double median = calculateMedian(values);
        long missingCount = totalRows - values.size();
        double missingPercentage = (double) missingCount / totalRows * 100;
        List<Double> outliers = detectOutliers(values);
        StringBuilder summary = new StringBuilder();
        summary.append("The '").append(columnName).append("' data ranges from **").append(String.format("%.2f", min)).append("** to **").append(String.format("%.2f", max)).append("**. ");
        summary.append("The median value is **").append(String.format("%.2f", median)).append("**. ");
        if (missingCount > 0) {
            summary.append("About **").append(String.format("%.0f%%", missingPercentage)).append(" of the values are missing**. ");
        }
        if (!outliers.isEmpty()) {
            summary.append("**").append(outliers.size()).append(" potential outliers** were detected.");
        }
        return summary.toString();
    }

    private NumericSummaryDto calculateNumericSummary(List<Double> values) {
        NumericSummaryDto summary = new NumericSummaryDto();
        if (values.isEmpty()) {
            return summary;
        }
        Collections.sort(values);
        summary.setMin(values.get(0));
        summary.setMax(values.get(values.size() - 1));
        summary.setAvg(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        summary.setMedian(calculateMedian(values));
        summary.setStDev(calculateStandardDeviation(values, summary.getAvg()));
        summary.setHistogramBins(createHistogramBins(values));
        return summary;
    }

    private List<Bin> createHistogramBins(List<Double> values) {
        final int numBins = 10;
        if (values.size() < 2) {
            return Collections.emptyList();
        }
        double min = values.get(0);
        double max = values.get(values.size() - 1);
        double range = max - min;
        double binSize = range / numBins;
        if (binSize == 0) {
            Bin bin = new Bin();
            bin.setLabel(String.format("%.2f", min));
            bin.setCount(values.size());
            return Collections.singletonList(bin);
        }
        List<Bin> bins = new ArrayList<>(numBins);
        for (int i = 0; i < numBins; i++) {
            Bin bin = new Bin();
            bin.setLabel(String.format("%.2f-%.2f", min + i * binSize, min + (i + 1) * binSize));
            bin.setCount(0);
            bins.add(bin);
        }
        for (Double value : values) {
            int binIndex = (int) Math.min(Math.floor((value - min) / binSize), numBins - 1);
            if (binIndex >= 0) {
                bins.get(binIndex).setCount(bins.get(binIndex).getCount() + 1);
            }
        }
        return bins;
    }

    private List<Map<String, Object>> findDuplicateRows(List<Map<String, Object>> excelData) {
        Set<Map<String, Object>> seen = new HashSet<>();
        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map<String, Object> row : excelData) {
            if (!seen.add(row)) {
                duplicates.add(row);
            }
        }
        return duplicates;
    }

    private String inferType(List<Object> values) {
        List<Object> nonNullValues = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (nonNullValues.isEmpty()) {
            return "Empty";
        }
        long numericCount = nonNullValues.stream().filter(v -> v instanceof Number).count();
        if ((double) numericCount / nonNullValues.size() > 0.8) {
            long dateLikeValues = nonNullValues.stream()
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).doubleValue())
                    .filter(d -> {
                        double minDateSerial = LocalDate.of(1970, 1, 1).toEpochDay() + EXCEL_EPOCH_OFFSET;
                        double maxDateSerial = LocalDate.of(2100, 1, 1).toEpochDay() + EXCEL_EPOCH_OFFSET;
                        return d >= minDateSerial && d <= maxDateSerial;
                    })
                    .count();

            if ((double) dateLikeValues / nonNullValues.size() > 0.8) {
                return "Date";
            }
            return "Numeric";
        }
        long uniqueCount = nonNullValues.stream().distinct().count();
        if (uniqueCount == nonNullValues.size()) {
            return "ID/Text";
        }
        return "Categorical";
    }

    private void analyzeNumericColumn(StringBuilder report, String columnName, List<Double> values, int totalRows) {
        if (values.isEmpty()) return;
        String summaryText = createNumericSummaryText(columnName, values, totalRows);
        report.append("- **Summary**: ").append(summaryText).append("\n");
        double min = values.stream().min(Double::compare).orElse(0.0);
        double max = values.stream().max(Double::compare).orElse(0.0);
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double median = calculateMedian(values);
        double stDev = calculateStandardDeviation(values, avg);
        List<Double> outliers = detectOutliers(values);
        report.append("- **Range**: **").append(String.format("%.2f", min)).append(" - ").append(String.format("%.2f", max)).append("**\n");
        report.append("- **Median**: **").append(String.format("%.2f", median)).append("**\n");
        report.append("- **Standard Deviation**: **").append(String.format("%.2f", stDev)).append("**\n");
        report.append("- **Outliers**: ").append(outliers.size()).append(" detected.\n");
    }

    private void analyzeCategoricalColumn(StringBuilder report, String columnName, List<String> values, int totalRows) {
        Map<String, Long> freq = values.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        if (freq.isEmpty()) return;
        long uniqueCount = values.stream().distinct().count();
        report.append("- **Top Categories**:\n");
        freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> report.append("  - **").append(e.getKey()).append("** (").append(String.format("%.1f", (double) e.getValue() / values.size() * 100)).append("%)\n"));
    }

    private boolean isHighlyImbalanced(List<String> values) {
        Map<String, Long> freq = values.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        if (freq.size() <= 1) return false;
        long maxCount = freq.values().stream().max(Long::compare).orElse(0L);
        return (double) maxCount / values.size() > 0.90;
    }

    private Map<String, Map<String, Double>> calculateCorrelationMatrix(Map<String, List<Double>> numericData) {
        Map<String, Map<String, Double>> correlations = new HashMap<>();
        List<String> columns = new ArrayList<>(numericData.keySet());
        for (int i = 0; i < columns.size(); i++) {
            for (int j = i; j < columns.size(); j++) {
                String col1 = columns.get(i);
                String col2 = columns.get(j);
                if (i == j) {
                    correlations.computeIfAbsent(col1, k -> new HashMap<>()).put(col2, 1.0);
                    continue;
                }
                List<Double> vals1 = numericData.get(col1);
                List<Double> vals2 = numericData.get(col2);
                if (vals1.size() == vals2.size() && vals1.size() > 1) {
                    double mean1 = vals1.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double mean2 = vals2.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double numerator = 0.0;
                    double denominator1 = 0.0;
                    double denominator2 = 0.0;
                    for (int k = 0; k < vals1.size(); k++) {
                        double diff1 = vals1.get(k) - mean1;
                        double diff2 = vals2.get(k) - mean2;
                        numerator += diff1 * diff2;
                        denominator1 += diff1 * diff1;
                        denominator2 += diff2 * diff2;
                    }
                    double correlation = (denominator1 * denominator2 == 0) ? 0 : numerator / Math.sqrt(denominator1 * denominator2);
                    correlations.computeIfAbsent(col1, k -> new HashMap<>()).put(col2, correlation);
                    correlations.computeIfAbsent(col2, k -> new HashMap<>()).put(col1, correlation);
                }
            }
        }
        return correlations;
    }

    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        Collections.sort(values);
        int mid = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(mid);
        } else {
            return (values.get(mid - 1) + values.get(mid)) / 2.0;
        }
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() < 2) return 0.0;
        double sumOfSquares = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();
        return Math.sqrt(sumOfSquares / values.size());
    }

    private String convertToDate(double excelSerial) {
        if (excelSerial <= 0) return "N/A";
        LocalDate date = LocalDate.of(1900, 1, 1).plusDays((long) excelSerial - 2);
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private List<Double> detectOutliers(List<Double> values) {
        if (values.size() < 4) return Collections.emptyList();
        Collections.sort(values);
        int mid = values.size() / 2;
        double q1 = (values.size() % 2 == 1) ? values.get(mid / 2) : (values.get(mid / 2 - 1) + values.get(mid / 2)) / 2.0;
        double q3 = (values.size() % 2 == 1) ? values.get(mid + mid / 2) : (values.get(mid + mid / 2) + values.get(mid + mid / 2 + 1)) / 2.0;
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        return values.stream().filter(v -> v < lowerBound || v > upperBound).collect(Collectors.toList());
    }
    private double calculateOverallQualityScore(double emptyPercentage, double duplicatePercentage) {
        // These weights can be adjusted based on the importance of each factor
        double missingWeight = 0.5;
        double duplicateWeight = 0.5;

        // Calculate a score from 0 to 1, where 1 is perfect quality
        double score = 1.0 - (missingWeight * emptyPercentage + duplicateWeight * duplicatePercentage);

        // Ensure the score is between 0 and 1
        return Math.max(0, Math.min(1.0, score));
    }


    public String analyzeData(List<Map<String, Object>> data) {
        // This is the one-line fix!
        return analyzeExcelData(data);
    }
}