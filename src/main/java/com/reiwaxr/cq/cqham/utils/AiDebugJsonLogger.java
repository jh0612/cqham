package com.reiwaxr.cq.cqham.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 调试日志工具：把请求/响应按 JSON 落盘，便于快速定位问题。
 */
public class AiDebugJsonLogger {

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    private final List<Path> logDirs;

    public AiDebugJsonLogger() {
        this(Path.of(System.getProperty("user.dir"), ".cqham", "ai-debug"));
    }

    public AiDebugJsonLogger(Path logDir) {
        this.logDirs = new ArrayList<>();
        if (logDir != null) {
            Path normalized = logDir.normalize();
            this.logDirs.add(normalized);
            try {
                Files.createDirectories(normalized);
            } catch (IOException ignored) {
                // ignore
            }
        }
        if (this.logDirs.isEmpty()) {
            Path fallback = Path.of(".", ".cqham", "ai-debug").normalize();
            this.logDirs.add(fallback);
            try {
                Files.createDirectories(fallback);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public AiDebugJsonLogger(List<Path> logDirs) {
        this.logDirs = new ArrayList<>();
        for (Path dir : logDirs) {
            if (dir == null) {
                continue;
            }
            Path normalized = dir.normalize();
            this.logDirs.add(normalized);
            try {
                Files.createDirectories(normalized);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public void logPayload(String eventName, Object payload) {
        if (eventName == null || eventName.isBlank()) {
            return;
        }

        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("event", eventName);
            logEntry.put("payload", sanitize(payload));

            String fileName = sanitizeFileName(eventName)
                    + "-" + FILE_NAME_FORMATTER.format(Instant.now())
                    + ".json";

            boolean written = false;
            for (Path logDir : logDirs) {
                try {
                    Files.createDirectories(logDir);
                    Path filePath = logDir.resolve(fileName);
                    Files.writeString(
                            filePath,
                            OBJECT_MAPPER.writeValueAsString(logEntry),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    );
                    written = true;
                } catch (Exception ignored) {
                    // 继续尝试下一个目标目录
                }
            }

            if (!written) {
                throw new IOException("所有日志目录都无法写入: " + logDirs);
            }
        } catch (Exception ex) {
            System.err.println("AI debug log write failed: " + ex.getMessage());
        }
    }

    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Record recordValue) {
            return sanitizeRecord(recordValue);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                sanitized.put(key, sanitizeValue(key, entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> list = new ArrayList<>();
            for (Object item : iterable) {
                list.add(sanitize(item));
            }
            return list;
        }
        if (value instanceof Object[] array) {
            List<Object> list = new ArrayList<>();
            for (Object item : array) {
                list.add(sanitize(item));
            }
            return list;
        }
        return value;
    }

    /**
     * 属性为 Record 类型时，使用反射获取属性值并进行脱敏处理。
     * @param recordValue Record 对象
     * @return Record 对象的 Map 表示，敏感字段已被脱敏
     */
    private Map<String, Object> sanitizeRecord(Record recordValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (java.lang.reflect.RecordComponent component : recordValue.getClass().getRecordComponents()) {
            if (component == null || component.getName() == null) {
                continue;
            }
            try {
                var accessor = component.getAccessor();
                Object fieldValue = accessor.invoke(recordValue);
                result.put(component.getName(), sanitizeValue(component.getName(), fieldValue));
            } catch (Exception ignored) {
                result.put(component.getName(), "[unavailable]");
            }
        }
        return result;
    }

    private Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key) && value instanceof String text) {
            return maskSecret(text);
        }
        if (value instanceof Map<?, ?> || value instanceof Iterable<?> || value.getClass().isArray()) {
            return sanitize(value);
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase();
        return normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("authorization")
                || normalized.contains("password")
                || normalized.contains("cookie");
    }

    private String maskSecret(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return "*****";
    }

    private String sanitizeFileName(String eventName) {
        String normalized = eventName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return normalized.isBlank() ? "ai_debug" : normalized;
    }
}
