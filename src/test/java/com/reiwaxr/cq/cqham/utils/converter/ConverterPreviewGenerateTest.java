package com.reiwaxr.cq.cqham.utils.converter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 样例文件转换预览：将 converTest 下样例批量转换为 Markdown。
 */
class ConverterPreviewGenerateTest {

    @Test
    void generatePreviewMarkdownForSampleFiles() throws Exception {
        Path baseDir = Path.of(System.getProperty("user.dir"), "converTest");
        Path outDir = baseDir.resolve("md-preview");
        Files.createDirectories(outDir);

        List<Path> samples = List.of(
                baseDir.resolve("正規表現作成・解析MD 入力・出力説明書.docx"),
                baseDir.resolve("正規表現作成・解析MD 入力・出力説明書.pdf"),
                baseDir.resolve("自由问答机器人.xlsx"),
                baseDir.resolve("pptTest.pptx")
        );

        for (Path sample : samples) {
            assertFalse(Files.notExists(sample), "样例文件不存在: " + sample);
            DocumentToMarkdownConverter converter = ConverterFactory.getConverter(sample);
            String markdown = converter.convert(sample);
            String baseName = sample.getFileName().toString().replaceFirst("\\.[^.]+$", "");
            String ext = sample.getFileName().toString().replaceFirst("^.*(\\.[^.]+)$", "$1").replace(".", "");
            Path outFile = outDir.resolve(baseName + "-" + ext + ".md");
            Files.writeString(outFile, markdown, StandardCharsets.UTF_8);
        }
    }
}
