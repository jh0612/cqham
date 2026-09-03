package com.reiwaxr.cq.cqham.utils;

import java.util.regex.Pattern;

/**
 * 通用工具
 */
public class MarkdownUtil {
    /* 章节标题匹配模式 */
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^(第[\\d一二三四五六七八九十百千]+[章节篇部卷].*)$");
    /* 十进制标题匹配模式 */
    private static final Pattern DECIMAL_HEADING_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+){0,5})[\\s、.．-]+(.+)$");


    /**
     * PDF用 检测行是否为标题，并返回标题级别（1-6），如果不是标题则返回 0
     * 规则：
     * 1. CHAPTER_PATTERN 匹配的行返回 2 级标题
     * 2. DECIMAL_HEADING_PATTERN 匹配的行返回对应的数字级别，最大为 6
     * 3. 以“【”开头并以“】”结尾的行，或者以“第”开头并包含“例”的行返回 3 级标题
     * 4. 其他行返回 0
     * @param line 要检测的行
     * @return 标题级别（1-6），如果不是标题则返回 0
     */
    public static int detectHeadingLevel(String line) {
        if (CHAPTER_PATTERN.matcher(line).matches()) {
            return 2;
        }
        java.util.regex.Matcher numericMatcher = DECIMAL_HEADING_PATTERN.matcher(line);
        if (numericMatcher.matches()) {
            String key = numericMatcher.group(1);
            int depth = key.split("\\.").length;
            return Math.min(6, depth + 1);
        }
        if ((line.startsWith("【") && line.endsWith("】")) || line.startsWith("第") && line.contains("例")) {
            return 3;
        }
        return 0;
    }


}
