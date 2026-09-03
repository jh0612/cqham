package com.reiwaxr.cq.cqham.utils.converter;

import javafx.concurrent.Task;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 后台转换任务（Service + Task）
 */
public class ConversionTask extends Task<Void> {
    private static final DateTimeFormatter LOG_FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final List<Path> inputFiles;
    private final Path outputDir;
    private final List<Path> generatedMarkdownFiles = new ArrayList<>();
    private final List<String> failedRecords = new ArrayList<>();
    private Path errorLogFile;

    public ConversionTask(List<Path> inputFiles, Path outputDir) {
        this.inputFiles = inputFiles;
        this.outputDir = outputDir;
    }

    public List<Path> getGeneratedMarkdownFiles() {
        return List.copyOf(generatedMarkdownFiles);
    }

    public Path getErrorLogFile() {
        return errorLogFile;
    }

    @Override
    protected Void call() throws Exception {
        long startTime = System.currentTimeMillis();
        int total = inputFiles.size();
        AtomicInteger processed = new AtomicInteger(0);
        generatedMarkdownFiles.clear();
        failedRecords.clear();
        errorLogFile = null;

        updateMessage("开始转换... 共 " + total + " 个文件");

        for (Path file : inputFiles) {
            if (isCancelled()) break;

            String fileName = file.getFileName().toString();
            updateMessage("正在处理: " + fileName + " (" + processed.get() + "/" + total + ")");
            updateProgress(processed.get(), total);

            try {
                DocumentToMarkdownConverter converter = ConverterFactory.getConverter(file);
                String markdown = converter.convert(file);

                // 生成输出文件名
                String baseName = fileName.replaceFirst("\\.[^.]+$", "");
                Path outputFile = outputDir.resolve(baseName + ".md");
                Files.writeString(outputFile, markdown);
                generatedMarkdownFiles.add(outputFile);

                processed.incrementAndGet();
                updateProgress(processed.get(), total);

            } catch (Exception e) {
                updateMessage("转换失败: " + fileName + " - " + e.getMessage());
                failedRecords.add(file.toString() + " | " + e.getClass().getSimpleName() + " | " + safeMessage(e));
                // 继续处理下一个
            }
        }

        if (!failedRecords.isEmpty()) {
            errorLogFile = writeFailureLog();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        String failedHint = failedRecords.isEmpty() ? "" : " 失败 " + failedRecords.size() + " 个";
        if(elapsed >= 1000){
            elapsed = elapsed / 1000;
            updateMessage("转换完成！耗时 " + elapsed + " 秒。成功 " + processed.get() + "/" + total + " 个文件。" + failedHint);
        } else {
            updateMessage("转换完成！耗时 " + elapsed + " 毫秒。成功 " + processed.get() + "/" + total + " 个文件。" + failedHint);
        }
        updateProgress(total, total);
        return null;
    }

    private Path writeFailureLog() {
        try {
            Files.createDirectories(outputDir);
            Path logFile = outputDir.resolve("conversion-errors-" + LOG_FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".log");

            List<String> lines = new ArrayList<>();
            lines.add("Markdown Conversion Error Log");
            lines.add("Time: " + LocalDateTime.now());
            lines.add("Total Failed: " + failedRecords.size());
            lines.add("");
            lines.addAll(failedRecords);

            Files.write(
                    logFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return logFile;
        } catch (Exception ex) {
            failedRecords.add("日志写入失败 | " + ex.getClass().getSimpleName() + " | " + safeMessage(ex));
            return null;
        }
    }

    private String safeMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "无错误信息";
        }
        return ex.getMessage();
    }
}
