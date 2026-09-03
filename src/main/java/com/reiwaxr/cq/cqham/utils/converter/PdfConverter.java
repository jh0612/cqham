package com.reiwaxr.cq.cqham.utils.converter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.reiwaxr.cq.cqham.utils.MarkdownUtil.detectHeadingLevel;

/**
 * PDF 変換器（.pdf 対応）
 */
public class PdfConverter implements DocumentToMarkdownConverter {

    /* 编号行匹配模式 */
    private static final Pattern NUMBERED_ROW_PATTERN = Pattern.compile("^(\\d+)\\s+(.+)$");
    /* 表格分割模式（多个空格或制表符） */
    private static final Pattern TABLE_SPLIT_PATTERN = Pattern.compile("\\t+| {2,}");

    /**
     * 将 PDF 文件转换为 Markdown
     *
     * @param filePath PDF 文件路径
     * @return 转换后的 Markdown 内容
     * @throws IOException 转换失败时抛出
     */
    @Override
    public String convert(Path filePath) throws IOException {
        StringBuilder md = new StringBuilder();
        try {
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfTextStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            Method loadMethod = loaderClass.getMethod("loadPDF", java.io.File.class);
            Method getNumberOfPagesMethod = pdDocumentClass.getMethod("getNumberOfPages");
            Method closeMethod = pdDocumentClass.getMethod("close");

            Object document = loadMethod.invoke(null, filePath.toFile());
            try {
                int pageCount = (Integer) getNumberOfPagesMethod.invoke(document);
                Object stripper = pdfTextStripperClass.getDeclaredConstructor().newInstance();
                pdfTextStripperClass.getMethod("setSortByPosition", boolean.class).invoke(stripper, true);

                md.append("# ").append(filePath.getFileName()).append("\n\n");
                for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                    pdfTextStripperClass.getMethod("setStartPage", int.class).invoke(stripper, pageIndex);
                    pdfTextStripperClass.getMethod("setEndPage", int.class).invoke(stripper, pageIndex);
                    String pageText = String.valueOf(
                            pdfTextStripperClass.getMethod("getText", pdDocumentClass).invoke(stripper, document)
                    ).trim();

                    md.append("## 第 ").append(pageIndex).append(" 页\n\n");
                    if (pageText.isEmpty()) {
                        md.append("*（此页无可提取文本）*\n\n");
                        continue;
                    }

                    String normalized = pageText
                            .replace("\r\n", "\n")
                            .replace("\r", "\n")
                            .replaceAll("\\n{3,}", "\\n\\n")
                            .trim();

                    md.append(toStructuredMarkdown(normalized)).append("\n\n");
                }
            } finally {
                closeMethod.invoke(document);
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("未找到 PDF 转换依赖，请确认已引入 pdfbox", ex);
        } catch (Exception ex) {
            throw new IOException("PDF 转换失败: " + ex.getMessage(), ex);
        }
        return md.toString();
    }

    /**
     * 将 PDF 页文本转换为结构化 Markdown
     * 规则：
     * 1. 识别标题行，转换为 Markdown 标题
     * 2. 识别表格块，转换为 Markdown 表格
     * 3. 识别编号项目表格，转换为 Markdown 表格
     * 4. 保留普通文本行
     * @param pageText PDF 页文本
     * @return 转换后的结构化 Markdown
     * @throws IOException 转换失败时抛出
     */
    private String toStructuredMarkdown(String pageText) {
        StringBuilder out = new StringBuilder();
        List<String> lines = new ArrayList<>();
        for (String raw : pageText.split("\\n")) {
            String line = raw.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }

        int i = 0;
        while (i < lines.size()) {
            List<List<String>> numberedRows = tryParseNumberedItemTable(lines, i);
            if (!numberedRows.isEmpty()) {
                appendTable(out, numberedRows);
                i += numberedRows.size() - 1;
                continue;
            }

            List<List<String>> tableRows = tryParseTableBlock(lines, i);
            if (!tableRows.isEmpty()) {
                appendTable(out, tableRows);
                i += tableRows.size();
                continue;
            }

            String line = lines.get(i);
            int headingLevel = detectHeadingLevel(line);
            if (headingLevel > 0) {
                out.append("#".repeat(headingLevel)).append(" ").append(line).append("\n\n");
            } else {
                out.append(line).append("\n\n");
            }
            i++;
        }

        return out.toString().trim();
    }

    /**
     * 尝试解析表格块，返回表格行列表，如果不是表格则返回空列表
     * @param lines 文本行列表
     * @param start 起始行索引
     * @return 解析出的表格行列表，如果不是表格则返回空列表
     */
    private List<List<String>> tryParseTableBlock(List<String> lines, int start) {
        List<List<String>> rows = new ArrayList<>();
        int expectedCols = -1;
        for (int i = start; i < lines.size(); i++) {
            List<String> cols = splitColumns(lines.get(i));
            if (cols.size() < 2) {
                break;
            }
            if (expectedCols == -1) {
                expectedCols = cols.size();
            }
            if (Math.abs(cols.size() - expectedCols) > 1) {
                break;
            }
            rows.add(cols);
        }

        if (rows.size() < 2) {
            return List.of();
        }
        return rows;
    }


    /**
     * 尝试解析编号项目表格（No. / NO. / No 开头的表格）
     * 规则：
     * 1. 第一行必须以 "No."、"NO." 或 "No " 开头
     * 2. 后续行必须匹配 NUMBERED_ROW_PATTERN
     * 3. 至少有三行数据（包括表头）
     *
     * 修改点：对数据行进行更精细的分割，使其能拆分为多个列（根据表头列数填充）
     */
    private List<List<String>> tryParseNumberedItemTable(List<String> lines, int start) {
        if (start >= lines.size()) {
            return List.of();
        }

        List<List<String>> rows = new ArrayList<>();
        String current = lines.get(start);
        if (!(current.startsWith("No.") || current.startsWith("NO.") || current.startsWith("No "))) {
            return List.of();
        }

        // 解析表头行，获取列数
        List<String> header = splitColumns(current);
        rows.add(header);
        int headerCols = header.size();  // 一般为 4

        int idx = start + 1;
        while (idx < lines.size()) {
            Matcher m = NUMBERED_ROW_PATTERN.matcher(lines.get(idx));
            if (!m.matches()) {
                break;
            }
            String number = m.group(1);
            String rest = m.group(2).trim();

            // 对剩余内容进行分割，得到多个字段
            List<String> extraParts = splitColumnsForData(rest);

            // 构建行：编号 + extraParts，不足补空
            List<String> row = new ArrayList<>();
            row.add(number);
            int maxExtra = headerCols - 1;  // 除去编号列
            for (int i = 0; i < maxExtra; i++) {
                if (i < extraParts.size()) {
                    row.add(extraParts.get(i));
                } else {
                    row.add("");  // 如果字段数不足，补空字符串
                }
            }
            // 如果 extraParts 多于需要的列数，将多余的合并到最后一列？但通常不会，暂不处理
            rows.add(row);
            idx++;
        }

        if (rows.size() < 3) {
            return List.of();
        }
        return rows;
    }

    /**
     * 对数据行的剩余内容进行分割，尝试拆分为多个字段
     * 优先使用多个空格（≥2），如果拆分后字段数少于2，则尝试单个空格
     * @param line 剩余内容（不含编号）
     * @return 字段列表
     */
    private List<String> splitColumnsForData(String line) {
        // 1. 尝试多个空格
        String[] parts = TABLE_SPLIT_PATTERN.split(line);
        List<String> cols = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) cols.add(p);
        }
        if (cols.size() >= 2) {
            return cols;
        }

        // 2. 尝试单个空格
        parts = line.split(" ");
        cols.clear();
        for (String p : parts) {
            if (!p.isEmpty()) cols.add(p);
        }
        if (cols.size() >= 2) {
            return cols;
        }

        // 3. 如果仍无法拆分，则将整个内容作为一个字段
        return Collections.singletonList(line);
    }

    /**
     * 将一行文本按表格列分割，返回列的列表
     * ★ 修改：对以 "No." 等开头的行使用单个空格分割，以正确拆分表头多列
     */
    private List<String> splitColumns(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return Collections.emptyList();

        // 如果是编号表头行，使用单个空格分割（保留多列）
        if (trimmed.startsWith("No.") || trimmed.startsWith("NO.") || trimmed.startsWith("No ")) {
            String[] parts = trimmed.split(" ");
            List<String> cols = new ArrayList<>();
            for (String part : parts) {
                if (!part.isEmpty()) cols.add(part);
            }
            return cols;
        }

        // 否则使用原有模式（多个空格或制表符）
        String[] parts = TABLE_SPLIT_PATTERN.split(trimmed);
        List<String> cols = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) cols.add(part);
        }
        return cols;
    }

    /**
     * 将表格内容追加到 Markdown 文本中
     *
     * @param out 目标 Markdown 文本的 StringBuilder
     * @param rows 表格的行数据
     */
    private void appendTable(StringBuilder out, List<List<String>> rows) {
        int maxCols = 0;
        for (List<String> row : rows) {
            maxCols = Math.max(maxCols, row.size());
        }
        if (maxCols < 2) {
            return;
        }

        List<String> header = padRow(rows.get(0), maxCols);
        out.append("|");
        for (String cell : header) {
            out.append(" ").append(cell).append(" |" );
        }
        out.append("\n|");
        for (int i = 0; i < maxCols; i++) {
            out.append(" --- |" );
        }
        out.append("\n");

        for (int r = 1; r < rows.size(); r++) {
            List<String> row = padRow(rows.get(r), maxCols);
            out.append("|");
            for (String cell : row) {
                out.append(" ").append(cell).append(" |" );
            }
            out.append("\n");
        }
        out.append("\n");
    }

    /**
     * 将行填充到指定列数
     */
    private List<String> padRow(List<String> row, int maxCols) {
        List<String> normalized = new ArrayList<>(row);
        while (normalized.size() < maxCols) {
            normalized.add("");
        }
        return normalized;
    }
}