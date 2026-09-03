package com.reiwaxr.cq.cqham.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiDebugJsonLoggerTest {

    @Test
    void shouldWriteJsonLogAndMaskApiKey() throws IOException {
        Path tempDir = Files.createTempDirectory("cqham-ai-log-test");
        AiDebugJsonLogger logger = new AiDebugJsonLogger(tempDir);

        logger.logPayload("chat.request", Map.of(
                "model", "qwen-plus",
                "apiKey", "sk-123456",
                "messages", java.util.List.of(Map.of("role", "user", "content", "hello"))
        ));

        Path[] files = Files.list(tempDir).toArray(Path[]::new);
        assertTrue(files.length > 0, "should create at least one json log file");

        String content = Files.readString(files[0]);
        assertTrue(content.contains("qwen-plus"));
        assertFalse(content.contains("sk-123456"));
        assertTrue(content.contains("*****"));
    }
}
