package com.reiwaxr.cq.cqham.utils.converter;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 转换器（支持 .docx）
 */
public class WordConverter implements DocumentToMarkdownConverter {

    private static final Pattern TEXTBOX_PATTERN = Pattern.compile("<w:txbxContent[\\s\\S]*?</w:txbxContent>");
    private static final Pattern TEXT_NODE_PATTERN = Pattern.compile("<w:t[^>]*>(.*?)</w:t>");
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private int currentIndex = 0;

    @Override
    public String convert(Path filePath) throws IOException {
        StringBuilder md = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(filePath))) {
            List<String> shapeTexts = extractTextBoxTexts(doc);
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    // [修改] 先判断段落是否包含任何有效内容（文本或图形），避免空段落误判
                    boolean hasContent = false;
                    for (XWPFRun run : para.getRuns()) {
                        String runText = run.getText(0);
                        if ((runText != null && !runText.trim().isEmpty()) || hasShape(run)) {
                            hasContent = true;
                            break;
                        }
                    }
                    if (!hasContent) {
                        md.append("\n");
                        continue;
                    }

                    // 使用增强版标题检测
                    int headingLevel = detectHeadingLevelWord(para);

                    if (headingLevel > 0 && headingLevel <= 6) {
                        // 标题段落保持原样：直接输出整体文本（忽略内部图形，原逻辑如此）
                        String text = para.getText().trim();
                        md.append("#".repeat(headingLevel)).append(" ").append(text).append("\n\n");
                    } else {
                        // [修改] 非标题段落：逐 Run 处理，在图形 Run 的位置插入对应的图形文本
                        for (XWPFRun run : para.getRuns()) {
                            String runText = run.getText(0);
                            if (runText != null) {
                                md.append(runText);
                            }
                            // 如果该 Run 包含图形，在此位置输出对应的图形文本（引用格式）
                            if (hasShape(run)) {
                                if (currentIndex < shapeTexts.size()) {
                                    md.append("\n\n> ").append(shapeTexts.get(currentIndex)).append("\n\n");
                                    currentIndex++;
                                }
                            }
                        }
                        md.append("\n\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    md.append(convertTable(table)).append("\n\n");
                }
            }
        }
        return md.toString();
    }

    /**
     * 检测段落的标题级别（1~6），如果不是标题则返回 0
     */
    private int detectHeadingLevelWord(XWPFParagraph para) {
        // 1. 样式检测（原有逻辑）
        String style = para.getStyle();
        if (style != null && style.toLowerCase().startsWith("heading")) {
            try {
                int level = Integer.parseInt(style.replaceAll("\\D+", ""));
                if (level >= 1 && level <= 6) return level;
            } catch (NumberFormatException ignored) {}
        }

        // 2. 大纲级别检测（如果设置了）
        CTPPr pPr = para.getCTP().getPPr();
        if (pPr != null && pPr.getOutlineLvl() != null) {
            int outlineLvl = pPr.getOutlineLvl().getVal().intValueExact();
            // outlineLvl 取值范围 0~8，对应标题级别 1~9，我们只取 1~6
            if (outlineLvl >= 0 && outlineLvl <= 5) {
                return outlineLvl + 1;
            }
        }

        // 3. 启发式：检查段落首个 Run 的字体大小和粗体（简单示例）
        //    （可根据文档特点调整阈值，例如中文文档标题常用 16pt 以上）
        List<XWPFRun> runs = para.getRuns();
        if (!runs.isEmpty()) {
            XWPFRun firstRun = runs.get(0);
            int fontSize = firstRun.getFontSize(); // 可能返回 -1 表示未设置
            boolean isBold = firstRun.isBold();
            // 仅当字体大小 >= 14 且为粗体时，粗略视为标题（级别根据大小划分）
            if (fontSize >= 20) return 1;
            else if (fontSize >= 16) return 2;
            else if (fontSize >= 14 && isBold) return 3;
            // 您可以根据需要增加更多规则
        }

        return 0; // 不是标题
    }

    private String convertTable(XWPFTable table) {
        StringBuilder tableMd = new StringBuilder();
        boolean isFirstRow = true;
        for (XWPFTableRow row : table.getRows()) {
            tableMd.append("|");
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText().trim().replace("\n", " ");
                tableMd.append(" ").append(cellText).append(" |");
            }
            tableMd.append("\n");
            if (isFirstRow) {
                // 添加表头分隔线
                tableMd.append("|");
                for (int i = 0; i < row.getTableCells().size(); i++) {
                    tableMd.append(" --- |");
                }
                tableMd.append("\n");
                isFirstRow = false;
            }
        }
        return tableMd.toString();
    }

    private List<String> extractTextBoxTexts(XWPFDocument doc) {
        List<String> texts = new ArrayList<>();
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        String xml = doc.getDocument().xmlText();

        Matcher boxMatcher = TEXTBOX_PATTERN.matcher(xml);
        while (boxMatcher.find()) {
            String boxXml = boxMatcher.group();
            StringBuilder sb = new StringBuilder();
            Matcher textMatcher = TEXT_NODE_PATTERN.matcher(boxXml);
            while (textMatcher.find()) {
                String piece = decodeXmlEntities(textMatcher.group(1)).trim();
                if (!piece.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(piece);
                }
            }
            String merged = cleanupShapeText(sb.toString());
            if (!merged.isEmpty() && dedup.add(merged)) {
                texts.add(merged);
            }
        }
        return texts;
    }

    private String decodeXmlEntities(String text) {
        return text
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private String cleanupShapeText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw;
        if (cleaned.contains("<w:")) {
            cleaned = XML_TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        }
        cleaned = decodeXmlEntities(cleaned)
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.startsWith("w:")) {
            return "";
        }
        return cleaned;
    }

    /**
     * 判断一个 XWPFRun 是否包含图形（形状、文本框、图片等）
     */
    private boolean hasShape(XWPFRun run) {
        // 检查是否包含绘图对象（形状、文本框等）
        if (run.getCTR().getDrawingList() != null && !run.getCTR().getDrawingList().isEmpty()) {
            return true;
        }
        // 检查是否包含嵌入式图片
        if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) {
            return true;
        }
        // 增加检查：CTR 中是否包含 drawing 标签（更底层）
        if (run.getCTR().toString().contains("<w:drawing")) {
            return true;
        }
        return false;
    }

}