package com.reiwaxr.cq.cqham.config;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;

public class DeepLConfig {

    // 本地DeepLX代理地址
    private static final String PROXY_HOST = "http://127.0.0.1:1188";
    // 官方API Key（使用代理时无需填写）模式切换：""=本地DeepLX代理；有值=官方DeepL API
    private static final String OFFICIAL_AUTH_KEY = "";

    // 获取翻译客户端
    public static DeepLClient getClient() {
        DeepLClientOptions options = new DeepLClientOptions();
        if (OFFICIAL_AUTH_KEY.isEmpty()) {
            // 本地DeepLX代理，替换接口地址
            options.setServerUrl(PROXY_HOST);
            return new DeepLClient("dummy-key", options);
        } else {
            // 官方接口
            return new DeepLClient(OFFICIAL_AUTH_KEY, options);
        }
    }
}
