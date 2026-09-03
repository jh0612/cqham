package com.reiwaxr.cq.cqham.utils.converter;

import java.nio.file.Path;

/**
 * 转换器工厂
 */
public class ConverterFactory {
    public static DocumentToMarkdownConverter getConverter(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".docx")) {
            return new WordConverter();
        } else if (fileName.endsWith(".xlsx")) {
            return new ExcelConverter();
        } else if (fileName.endsWith(".pdf")) {
            return new PdfConverter();
        } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
            return new PowerPointConverter();
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + fileName);
        }
    }
}
