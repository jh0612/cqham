package com.reiwaxr.cq.cqham.utils.converter;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PowerPoint 変換器（.ppt / .pptx）
 */
public class PowerPointConverter implements DocumentToMarkdownConverter {

    @Override
    public String convert(Path filePath) throws IOException {
        String lower = filePath.getFileName().toString().toLowerCase();
        if (lower.endsWith(".ppt")) {
            throw new IllegalArgumentException("暂不支持旧版 .ppt，请先另存为 .pptx 后再转换");
        }
        if (lower.endsWith(".pptx")) {
            return convertPptx(filePath);
        }
        throw new IllegalArgumentException("不支持的 PowerPoint 格式: " + filePath.getFileName());
    }

    private String convertPptx(Path filePath) throws IOException {
        StringBuilder md = new StringBuilder();
           try (InputStream in = Files.newInputStream(filePath);
             XMLSlideShow slideShow = new XMLSlideShow(in)) {

            md.append("# ").append(filePath.getFileName()).append("\n\n");
            int index = 1;
            for (XSLFSlide slide : slideShow.getSlides()) {
                String title = slide.getTitle();
                md.append("## 第 ").append(index).append(" 页");
                if (title != null && !title.isBlank()) {
                    md.append(" - ").append(title.trim());
                }
                md.append("\n\n");

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        appendTextShape(textShape, md);
                    } else if (shape instanceof XSLFTable table) {
                        appendTable(table, md);
                    }
                }
                md.append("\n");
                index++;
            }
        }
        return md.toString();
    }

    private void appendTextShape(XSLFTextShape shape, StringBuilder md) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            StringBuilder line = new StringBuilder();
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                if (run == null || run.getRawText() == null) {
                    continue;
                }
                line.append(run.getRawText());
            }
            String text = line.toString().trim();
            if (!text.isEmpty()) {
                md.append(text).append("\n\n");
            }
        }
    }

    private void appendTable(XSLFTable table, StringBuilder md) {
        boolean headerWritten = false;
        for (XSLFTableRow row : table.getRows()) {
            md.append("|");
            for (XSLFTableCell cell : row.getCells()) {
                String text = cell.getText() == null ? "" : cell.getText().trim().replace("\n", " ");
                md.append(" ").append(text).append(" |" );
            }
            md.append("\n");

            if (!headerWritten) {
                md.append("|");
                for (int i = 0; i < row.getCells().size(); i++) {
                    md.append(" --- |" );
                }
                md.append("\n");
                headerWritten = true;
            }
        }
        md.append("\n");
    }
}
