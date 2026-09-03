package com.reiwaxr.cq.cqham.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * AI助手配置读取类
 */
public class AiConfig {

    /* 管理员账号密码及API Key属性名和环境变量名 */
    private static final String ADMIN_USERNAME_PROPERTY = "cqham.ai.admin.username";
    /* 管理员密码属性名 */
    private static final String ADMIN_PASSWORD_PROPERTY = "cqham.ai.admin.password";
    /* 管理员API Key属性名 */
    private static final String ADMIN_API_KEY_PROPERTY = "cqham.ai.admin.apikey";
    /* AI接口基础URL属性名 */
    private static final String API_BASE_URL_PROPERTY = "cqham.ai.api.base-url";
    /* AI默认模型属性名 */
    private static final String API_DEFAULT_MODEL_PROPERTY = "cqham.ai.api.default-model";
    /* 管理员用户名环境变量名 */
    private static final String ADMIN_USERNAME_ENV = "CQHAM_AI_ADMIN_USERNAME";
    /* 管理员密码环境变量名 */
    private static final String ADMIN_PASSWORD_ENV = "CQHAM_AI_ADMIN_PASSWORD";
    /* 管理员API Key环境变量名 */
    private static final String ADMIN_API_KEY_ENV = "CQHAM_AI_ADMIN_API_KEY";
    /* AI接口基础URL环境变量名 */
    private static final String API_BASE_URL_ENV = "CQHAM_AI_API_BASE_URL";
    /* AI默认模型环境变量名 */
    private static final String API_DEFAULT_MODEL_ENV = "CQHAM_AI_API_DEFAULT_MODEL";
    /* 统一密钥属性名 */
    private static final String API_KEYS_PROPERTY = "cqham.api.keys";
    /* 统一密钥环境变量名 */
    private static final String API_KEYS_ENV = "CQHAM_API_KEYS";

    private static final Properties props = new Properties();

    /** AI助手配置读取类 */
    static {
        try (InputStream is = AiConfig.class.getResourceAsStream("/ai-config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 获取预设管理员用户名 */
    public static String getAdminUsername() {
        String runtimeValue = getRuntimeValue(ADMIN_USERNAME_PROPERTY, ADMIN_USERNAME_ENV);
        if (runtimeValue != null) {
            return runtimeValue;
        }
        return props.getProperty("ai.admin.username", "admin");
    }

    /** 获取预设管理员密码 */
    public static String getAdminPassword() {
        String runtimeValue = getRuntimeValue(ADMIN_PASSWORD_PROPERTY, ADMIN_PASSWORD_ENV);
        if (runtimeValue != null) {
            return runtimeValue;
        }
        return props.getProperty("ai.admin.password", "reiwaxr2026");
    }

    /** 获取内置API Key */
    public static String getAdminApiKey() {
        String runtimeValue = getRuntimeValue(ADMIN_API_KEY_PROPERTY, ADMIN_API_KEY_ENV);
        if (runtimeValue != null) {
            return runtimeValue;
        }

        String groupedKey = getGroupedApiKey("ai", "qwen", "dashscope");
        return groupedKey != null ? groupedKey : "";
    }

    /** 获取API基础URL */
    public static String getApiBaseUrl() {
        String runtimeValue = getRuntimeValue(API_BASE_URL_PROPERTY, API_BASE_URL_ENV);
        if (runtimeValue != null) {
            return runtimeValue;
        }
        return props.getProperty("ai.api.base-url", "https://dashscope.aliyuncs.com/compatible-mode/v1");
    }

    /** 获取默认模型 */
    public static String getDefaultModel() {
        String runtimeValue = getRuntimeValue(API_DEFAULT_MODEL_PROPERTY, API_DEFAULT_MODEL_ENV);
        if (runtimeValue != null) {
            return runtimeValue;
        }
        return props.getProperty("ai.api.default-model", "qwen-plus");
    }

    /** 获取文件最大大小（字节） */
    public static long getFileMaxSize() {
        return Long.parseLong(props.getProperty("ai.file.max-size", "5242880"));
    }

    /** 
     * 验证管理员账号密码
     * @param username 管理员用户名
     * @param password 管理员密码
     * @return 是否验证通过
     */
    public static boolean verifyAdmin(String username, String password) {
        return getAdminUsername().equals(username) && getAdminPassword().equals(password);
    }

    /**
     * 実行時の機密値を取得する
     * @param propertyName JVM参数名
     * @param envName 环境变量名
     * @return 配置值，未设置时返回null
     */
    private static String getRuntimeValue(String propertyName, String envName) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return null;
    }

    /**
     * 統合設定から対象サービスのAPI Keyを取得する
     * @param aliases 服务别名
     * @return API Key，未设置时返回null
     */
    private static String getGroupedApiKey(String... aliases) {
        Map<String, String> keyMap = parseGroupedKeys();
        for (String alias : aliases) {
            String value = keyMap.get(alias);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 統合キー文字列を分解する
     * @return 服务到API Key的映射
     */
    private static Map<String, String> parseGroupedKeys() {
        Map<String, String> result = new HashMap<>();
        String rawValue = getRuntimeValue(API_KEYS_PROPERTY, API_KEYS_ENV);
        if (rawValue == null) {
            return result;
        }

        String[] entries = rawValue.split("[;,\\r\\n]+");
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }

            String[] pair = entry.split("=", 2);
            if (pair.length != 2) {
                continue;
            }

            String serviceName = pair[0].trim().toLowerCase();
            String apiKey = pair[1].trim();
            if (!serviceName.isEmpty() && !apiKey.isEmpty()) {
                result.put(serviceName, apiKey);
            }
        }
        return result;
    }
}