package com.reiwaxr.cq.cqham.controller;


import com.deepl.api.DeepLClient;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import com.reiwaxr.cq.cqham.config.DeepLConfig;
import com.reiwaxr.cq.cqham.dao.VocabularyDao;
import com.reiwaxr.cq.cqham.entity.Vocabulary;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Map;

import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;

/**
 * @Description DeepL翻译页面
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-07-10 17:16
 */
public class TranslateViewController {
    @FXML
    private ComboBox<String> cbLang;
    @FXML
    private ComboBox<String> cbWordType;
    @FXML
    private TextArea taSource;
    @FXML
    private TextArea taTarget;

    // 语言映射 key:下拉显示文本, value:DeepL语言编码
    private final Map<String, String> langMap = Map.of(
            "中文 → 英文", "en-US",
            "中文 → 日文", "JA",
            "英文 → 中文", "ZH",
            "日文 → 中文", "ZH"
    );
    private final String[] wordTypes = {"名词", "动词", "短句", "专业术语", "日常对话"};
    private final VocabularyDao vocabDao = new VocabularyDao();

    // 页面初始化
    @FXML
    public void initialize() {
        // 加载翻译方向下拉框
        cbLang.getItems().addAll(langMap.keySet());
        cbLang.setValue("中文 → 英文");
        // 加载词汇分类
        cbWordType.getItems().addAll(wordTypes);
        cbWordType.setValue("短句");
    }

    // 执行文本翻译
    @FXML
    public void btnTranslateText() {
        String sourceText = taSource.getText().trim();
        if (sourceText.isEmpty()) {
            showAlert("提示", "请输入需要翻译的文本", Alert.AlertType.WARNING);
            return;
        }
        String selectLang = cbLang.getValue();
        String targetLangCode = langMap.get(selectLang);

        // 多线程执行网络请求，避免UI卡顿
        new Thread(() -> {
            try {
                DeepLClient client = DeepLConfig.getClient();
                TextTranslationOptions opts = new TextTranslationOptions();
                TextResult result = client.translateText(sourceText, null, targetLangCode, opts);
                String translateResult = result.getText();
                // UI线程更新文本框
                javafx.application.Platform.runLater(() -> taTarget.setText(translateResult));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showAlert("翻译失败", e.getMessage(), Alert.AlertType.ERROR)
                );
                e.printStackTrace();
            }
        }).start();
    }

    // 保存词汇到SQLite词库
    @FXML
    public void btnSaveVocab() {
        String source = taSource.getText().trim();
        String target = taTarget.getText().trim();
        String type = cbWordType.getValue();
        if (source.isEmpty() || target.isEmpty()) {
            showAlert("提示", "原文和译文不能为空", Alert.AlertType.WARNING);
            return;
        }
        Vocabulary vocab = new Vocabulary();
        vocab.setSourceText(source);
        vocab.setTargetText(target);
        vocab.setWordType(type);
        try {
            vocabDao.addVocab(vocab);
            showAlert("成功", "词汇已保存至词库", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("保存失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // 文档翻译预留按钮（仅弹窗提示，后续扩展文件上传逻辑）
    @FXML
    public void btnUploadDoc() {
        showAlert("功能预留", "文档批量翻译功能待开发", Alert.AlertType.INFORMATION);
    }

    // 返回首页
    @FXML
    public void btnBackMain() throws Exception {
        // 当前窗口获取
        Stage stage = (Stage) cbWordType.getScene().getWindow();
        ViewUtil.switchView(MAIN_PAGE, stage);
    }

    // 弹窗工具
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
