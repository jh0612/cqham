package com.reiwaxr.cq.cqham.config;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;

import java.util.HashMap;
import java.util.Map;

public class DeepLConfig {

    /* 默认代理主机 */
    private static final String DEFAULT_PROXY_HOST = "http://127.0.0.1:1188";
    /* 默认认证密钥属性名 */
    private static final String AUTH_KEY_PROPERTY = "cqham.deepl.auth-key";
    /* 默认认证密钥环境变量名 */
    private static final String AUTH_KEY_ENV = "CQHAM_DEEPL_AUTH_KEY";
    /* 默认代理主机属性名 */
    private static final String PROXY_HOST_PROPERTY = "cqham.deepl.proxy-host";
    /* 默认代理主机环境变量名 */
    private static final String PROXY_HOST_ENV = "CQHAM_DEEPL_PROXY_HOST";
    /* 统一密钥属性名 */
    private static final String API_KEYS_PROPERTY = "cqham.api.keys";
    /* 统一密钥环境变量名 */
    private static final String API_KEYS_ENV = "CQHAM_API_KEYS";

    /**
     * 获取DeepL客户端实例
     * @return DeepLClient
     */
    public static DeepLClient getClient() {
        DeepLClientOptions options = new DeepLClientOptions();
        String authKey = getRuntimeValue(AUTH_KEY_PROPERTY, AUTH_KEY_ENV);
        if (authKey == null || authKey.isBlank()) {
            authKey = getGroupedApiKey("deepl");
        }
        if (authKey == null || authKey.isBlank()) {
            String proxyHost = getRuntimeValue(PROXY_HOST_PROPERTY, PROXY_HOST_ENV);
            options.setServerUrl(proxyHost != null ? proxyHost : DEFAULT_PROXY_HOST);
            return new DeepLClient("dummy-key", options);
        }
        return new DeepLClient(authKey, options);
    }

    /**
     * 获取运行时配置值
     * @param propertyName 属性名
     * @param envName 环境变量名
     * @return 配置值，如果未设置则返回 null
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
