package unit00.com.exalyze.service;



import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    public List<Map<String, Object>> readExcelData(MultipartFile file) throws IOException {
        System.out.println("Parsing Excel file: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize() + " bytes");
        List<Map<String, Object>> data = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return data;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            System.out.println("Headers found: " + headers);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow == null) {
                    continue;
                }
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = currentRow.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    switch (cell.getCellType()) {
                        case STRING:
                            rowMap.put(headers.get(j), cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowMap.put(headers.get(j), cell.getDateCellValue());
                            } else {
                                rowMap.put(headers.get(j), cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            rowMap.put(headers.get(j), cell.getBooleanCellValue());
                            break;
                        case FORMULA:
                            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                            CellValue cellValue = evaluator.evaluate(cell);
                            switch (cellValue.getCellType()) {
                                case STRING:
                                    rowMap.put(headers.get(j), cellValue.getStringValue());
                                    break;
                                case NUMERIC:
                                    rowMap.put(headers.get(j), cellValue.getNumberValue());
                                    break;
                                case BOOLEAN:
                                    rowMap.put(headers.get(j), cellValue.getBooleanValue());
                                    break;
                                default:
                                    rowMap.put(headers.get(j), null);
                            }
                            break;
                        case BLANK:
                        case ERROR:
                        default:
                            rowMap.put(headers.get(j), null);
                    }
                }
                data.add(rowMap);
            }
            System.out.println("Parsed " + data.size() + " rows successfully.");
        } catch (Exception e) {
            System.err.println("Excel parsing error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return data;
    }

    public List<Map<String, Object>> readCsvData(MultipartFile file) throws IOException {
        System.out.println("Parsing CSV file: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize() + " bytes");
        List<Map<String, Object>> data = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CSVParser csvParser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(false)
                    .build()
                    .parse(reader);

            List<String> headers = csvParser.getHeaderNames();
            System.out.println("Headers found: " + headers);

            for (CSVRecord csvRecord : csvParser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, csvRecord.get(header));
                }
                data.add(row);
            }
            System.out.println("Parsed " + data.size() + " rows successfully.");
        } catch (Exception e) {
            System.err.println("CSV parsing error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return data;
    }

    public byte[] generateExcelFile(List<Map<String, Object>> data) throws IOException {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Raw Data");

            Row headerRow = sheet.createRow(0);
            List<String> headers = new ArrayList<>(data.get(0).keySet());
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String header : headers) {
                    Cell cell = row.createCell(colNum++);
                    Object value = rowData.get(header);
                    if (value != null) {
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}