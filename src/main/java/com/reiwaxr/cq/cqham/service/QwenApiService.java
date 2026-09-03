package com.reiwaxr.cq.cqham.service;

import com.reiwaxr.cq.cqham.config.AiConfig;
import com.reiwaxr.cq.cqham.utils.AiDebugJsonLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 千问大模型API调用服务（OpenAI兼容接口）
 */
public class QwenApiService {

    /* 默认基础URL */
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    /* JSON对象映射器 */
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AiDebugJsonLogger AI_DEBUG_LOGGER = new AiDebugJsonLogger();

    /* HTTP客户端 */
    private final OkHttpClient client;
    /* 基础URL */
    private final String baseUrl;

    public QwenApiService() {
        this(AiConfig.getApiBaseUrl());
    }

    /**
     * 构造函数
     * @param baseUrl 基础URL
     */
    public QwenApiService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 对话消息
     */
    public record ChatMessage(String role, String content) {}

    /**
     * 对话请求を生成する
     *
     * @param apiKey API Key
     * @param model 模型名称
     * @param systemPrompt 系统提示词
     * @param messages 对话历史
     * @return HTTP请求对象
     * @throws IOException JSON构建异常
     */
    public Call createChatCall(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws IOException {
        Request request = buildChatRequest(apiKey, model, systemPrompt, messages);
        return client.newCall(request);
    }

    /**
     * 実行済みCallから応答本文を取得する
     *
     * @param call HTTP调用对象
     * @return AI返回内容
     * @throws IOException 调用异常
     */
    public String executeChat(Call call) throws IOException {
        try (Response response = call.execute()) {
            return parseChatResponse(response);
        }
    }

    /**
     * 发送对话请求（非流式）
     *
     * @param apiKey       API Key
     * @param model        模型名称
     * @param systemPrompt 系统提示词
     * @param messages     对话历史
     * @return AI返回的内容
     */
    public String chat(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws IOException {
        Call call = createChatCall(apiKey, model, systemPrompt, messages);
        return executeChat(call);
    }

    /**
     * 聊天请求を構築する
     */
    private Request buildChatRequest(String apiKey, String model, String systemPrompt, List<ChatMessage> messages) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        // 构建消息数组
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // 添加系统提示词
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messagesArray.add(systemMsg);
        }

        // 添加对话历史
        for (ChatMessage msg : messages) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("role", msg.role());
            node.put("content", msg.content());
            messagesArray.add(node);
        }

        // 添加用户输入
        requestBody.set("messages", messagesArray);

        // 发送HTTP请求
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        List<Map<String, String>> sanitizedMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> messageMap = new java.util.LinkedHashMap<>();
            messageMap.put("role", msg.role());
            messageMap.put("content", msg.content());
            sanitizedMessages.add(messageMap);
        }
        AI_DEBUG_LOGGER.logPayload("ai.chat.request", Map.of(
                "baseUrl", baseUrl,
                "model", model,
                "apiKey", apiKey,
                "systemPrompt", systemPrompt,
                "messages", sanitizedMessages,
                "requestJson", jsonBody
        ));

        // 构建HTTP请求
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        return request;
    }

    /**
     * 聊天响应を解析する
     */
    private String parseChatResponse(Response response) throws IOException {
        // 检查HTTP响应状态码
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
            AI_DEBUG_LOGGER.logPayload("ai.chat.response.error", java.util.Map.of(
                    "httpStatus", response.code(),
                    "errorBody", errorBody
            ));
            throw new IOException("API请求失败 (HTTP " + response.code() + "): " + errorBody);
        }

        String responseBody = response.body().string();
        AI_DEBUG_LOGGER.logPayload("ai.chat.response", java.util.Map.of(
                "httpStatus", response.code(),
                "responseBody", responseBody
        ));

        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode choices = responseJson.get("choices");
        // 检查返回的choices数组是否存在且非空
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.has("content")) {
                return message.get("content").asText();
            }
        }
        throw new IOException("API返回格式异常: " + responseBody);
    }

    /**
     * 获取可用的模型列表
     */
    public static List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();
        models.add("qwen-turbo");
        models.add("qwen-plus");
        models.add("qwen-max");
        models.add("qwen-max-longcontext");
        models.add("qwen-math-turbo");
        models.add("qwen3-vl-235b-a22b-thinking");
        models.add("qwen-plus-2025-07-28");
        models.add("qwen3.6-plus");
        models.add("qwen3.6-flash");
        return models;
    }
}