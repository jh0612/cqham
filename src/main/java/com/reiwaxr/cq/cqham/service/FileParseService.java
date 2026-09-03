package com.reiwaxr.cq.cqham.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文件解析服务 - 支持 Excel、Word、TXT、MD 文件内容提取
 */
public class FileParseService {

    /**
     * 根据文件类型解析文件内容
     *
     * @param file      文件
     * @param sheetName Excel的sheet名称（仅Excel有效，为null时读取第一个sheet）
     * @return 提取的文本内容
     */
    public String parseFile(File file, String sheetName) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return parseExcel(file, sheetName);
        } else if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
            return parseWord(file);
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return parseText(file);
        } else {
            throw new IOException("不支持的文件类型：" + fileName);
        }
    }

    /**
     * 解析Excel文件
     * @param file      Excel文件
     * @param sheetName Excel的sheet名称（为null时读取第一个sheet）
     * @return 提取的文本内容
     */
    private String parseExcel(File file, String sheetName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet;
            if (sheetName != null && !sheetName.isBlank()) {
                sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new IOException("未找到Sheet: " + sheetName);
                }
            } else {
                sheet = workbook.getSheetAt(0);
            }

            sb.append("【文件名: ").append(file.getName()).append(", Sheet: ").append(sheet.getSheetName()).append("】\n\n");

            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(getCellValue(cell)).append("\t");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 解析Word文件（仅提取纯文本）
     * @param file Word文件
     * @return 提取的文本内容
     */
    private String parseWord(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            sb.append("【文件名: ").append(file.getName()).append("】\n\n");
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 解析TXT/MD文件
     * @param file TXT/MD文件
     * @return 提取的文本内容
     */
    private String parseText(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return "【文件名: " + file.getName() + "】\n\n" + content;
    }

    /**
     * 获取Excel单元格值
     * @param cell 单元格
     * @return 单元格的字符串表示
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    yield String.valueOf((long) numVal);
                }
                yield String.valueOf(numVal);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            case BLANK -> "";
            default -> "";
        };
    }
}