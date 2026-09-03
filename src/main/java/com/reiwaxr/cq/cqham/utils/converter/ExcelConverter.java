package com.reiwaxr.cq.cqham.utils.converter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Excel 转换器（支持 .xlsx）
 */
public class ExcelConverter implements DocumentToMarkdownConverter {

    @Override
    public String convert(Path filePath) throws IOException {
        StringBuilder md = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(filePath))) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                // 添加 Sheet 标题（二级标题）
                md.append("## ").append(sheetName).append("\n\n");

                int rowCount = sheet.getPhysicalNumberOfRows();
                if (rowCount == 0) {
                    md.append("*（此 Sheet 为空）*\n\n");
                    continue;
                }

                // 处理表头（第一行）
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    md.append("*（无数据行）*\n\n");
                    continue;
                }
                int colCount = headerRow.getLastCellNum();
                if (colCount <= 0) {
                    md.append("*（无列）*\n\n");
                    continue;
                }

                // 生成表头行
                md.append("|");
                for (int c = 0; c < colCount; c++) {
                    Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    md.append(" ").append(getCellString(cell)).append(" |");
                }
                md.append("\n|");
                for (int c = 0; c < colCount; c++) {
                    md.append(" --- |");
                }
                md.append("\n");

                // 数据行（从第2行开始）
                for (int r = 1; r < rowCount; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    md.append("|");
                    for (int c = 0; c < colCount; c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        md.append(" ").append(getCellString(cell)).append(" |");
                    }
                    md.append("\n");
                }

                if (sheet instanceof XSSFSheet xssfSheet) {
                    List<ShapeNode> shapeNodes = extractShapeNodes(xssfSheet);
                    if (!shapeNodes.isEmpty()) {
                        md.append("\n### 图形文本\n\n");
                        for (ShapeNode node : shapeNodes) {
                            md.append("- ").append(node.text()).append("\n");
                        }
                        md.append("\n### 图形流程（Mermaid近似）\n\n");
                        md.append(buildMermaidFlow(shapeNodes)).append("\n");
                    }
                }

                md.append("\n");
            }
        }
        return md.toString();
    }

    private String getCellString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim().replace("\n", " ");
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<ShapeNode> extractShapeNodes(XSSFSheet sheet) {
        List<ShapeNode> nodes = new ArrayList<>();
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null) {
            return nodes;
        }

        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFSimpleShape simpleShape)) {
                continue;
            }
            String text = simpleShape.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            int row = Integer.MAX_VALUE;
            int col = Integer.MAX_VALUE;
            if (simpleShape.getAnchor() instanceof XSSFClientAnchor anchor) {
                row = anchor.getRow1();
                col = anchor.getCol1();
            }
            nodes.add(new ShapeNode(text.trim().replace("\n", " "), row, col));
        }

        nodes.sort(Comparator.comparingInt(ShapeNode::row).thenComparingInt(ShapeNode::col));
        return nodes;
    }

    private String buildMermaidFlow(List<ShapeNode> nodes) {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("```mermaid\n");
        mermaid.append("flowchart TD\n");
        for (int i = 0; i < nodes.size(); i++) {
            mermaid.append("N")
                    .append(i + 1)
                    .append("[\"")
                    .append(escapeMermaid(nodes.get(i).text()))
                    .append("\"]\n");
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            mermaid.append("N").append(i + 1).append(" --> N").append(i + 2).append("\n");
        }
        mermaid.append("```\n");
        return mermaid.toString();
    }

    private String escapeMermaid(String text) {
        return text.replace("\"", "'").replace("\n", " ");
    }

    private record ShapeNode(String text, int row, int col) {
    }
}
