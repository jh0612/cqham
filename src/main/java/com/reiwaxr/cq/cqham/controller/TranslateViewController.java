package com.reiwaxr.cq.cqham.controller;


import com.deepl.api.DeepLClient;
import com.deepl.api.DocumentTranslationException;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import com.reiwaxr.cq.cqham.config.DeepLConfig;
import com.reiwaxr.cq.cqham.dao.VocabularyDao;
import com.reiwaxr.cq.cqham.dao.VocabularyTagDao;
import com.reiwaxr.cq.cqham.entity.Vocabulary;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;
import static com.reiwaxr.cq.cqham.common.PagePath.VOCABULARY_VIEW_PAGE;

/**
 * @Description DeepL翻译页面
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-07-10 17:16
 */
public class TranslateViewController {

    private record LanguageDirection(String sourceLangCode, String targetLangCode) {}

    @FXML
    private ComboBox<String> cbLang;
    @FXML
    private ComboBox<String> cbWordType;
    @FXML
    private TextArea taSource;
    @FXML
    private TextArea taTarget;

    private final Map<String, LanguageDirection> langMap = new LinkedHashMap<>();
    private final VocabularyDao vocabDao = new VocabularyDao();
    private final VocabularyTagDao tagDao = new VocabularyTagDao();

    // 页面初始化
    @FXML
    public void initialize() {
        langMap.put("中文 → 英文", new LanguageDirection("ZH", "en-US"));
        langMap.put("中文 → 日文", new LanguageDirection("ZH", "JA"));
        langMap.put("英文 → 中文", new LanguageDirection("EN", "ZH"));
        langMap.put("日文 → 中文", new LanguageDirection("JA", "ZH"));

        cbLang.getItems().addAll(langMap.keySet());
        cbLang.setValue("中文 → 英文");
        reloadTags(null);
    }

    // 执行文本翻译
    @FXML
    public void btnTranslateText() {
        String sourceText = taSource.getText().trim();
        if (sourceText.isEmpty()) {
            showAlert("提示", "请输入需要翻译的文本", Alert.AlertType.WARNING);
            return;
        }
        LanguageDirection direction = getSelectedDirection();

        // 多线程执行网络请求，避免UI卡顿
        new Thread(() -> {
            try {
                DeepLClient client = DeepLConfig.getClient();
                TextTranslationOptions opts = new TextTranslationOptions();
                TextResult result = client.translateText(sourceText, direction.sourceLangCode(), direction.targetLangCode(), opts);
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
    // TODO 新建日期不是上海时区，后续可考虑使用ZonedDateTime或LocalDateTime.now(ZoneId.of("Asia/Shanghai"))来转换为上海时间
    @FXML
    public void btnSaveVocab() {
        String source = taSource.getText().trim();
        String target = taTarget.getText().trim();
        String type = cbWordType.getValue();
        if (source.isEmpty() || target.isEmpty()) {
            showAlert("提示", "原文和译文不能为空", Alert.AlertType.WARNING);
            return;
        }
        if (type == null || type.isBlank()) {
            showAlert("提示", "请选择标签", Alert.AlertType.WARNING);
            return;
        }
        Vocabulary vocab = new Vocabulary();
        vocab.setSourceText(source);
        vocab.setTargetText(target);
        vocab.setTagName(type);
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
        Stage stage = (Stage) cbLang.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择需要翻译的文档");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("文档文件", "*.docx", "*.pptx", "*.xlsx", "*.pdf", "*.txt", "*.html", "*.xhtml"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File inputFile = fileChooser.showOpenDialog(stage);
        if (inputFile == null) {
            return;
        }

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("选择翻译结果保存位置");
        saveChooser.setInitialFileName(buildTranslatedFileName(inputFile));
        File outputFile = saveChooser.showSaveDialog(stage);
        if (outputFile == null) {
            return;
        }

        LanguageDirection direction = getSelectedDirection();
        new Thread(() -> {
            try {
                DeepLClient client = DeepLConfig.getClient();
                client.translateDocument(inputFile, outputFile, direction.sourceLangCode(), direction.targetLangCode());
                javafx.application.Platform.runLater(() ->
                        showAlert("成功", "文档翻译完成，已保存到：\n" + outputFile.getAbsolutePath(), Alert.AlertType.INFORMATION)
                );
            } catch (DocumentTranslationException e) {
                javafx.application.Platform.runLater(() ->
                        showAlert("文档翻译失败", e.getMessage(), Alert.AlertType.ERROR)
                );
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showAlert("文档翻译失败", e.getMessage(), Alert.AlertType.ERROR)
                );
            }
        }).start();
    }

    // 返回首页
    @FXML
    public void btnBackMain() throws Exception {
        // 当前窗口获取
        Stage stage = (Stage) cbWordType.getScene().getWindow();
        ViewUtil.switchView(MAIN_PAGE, stage);
    }

    @FXML
    public void btnCopyTarget() {
        String target = taTarget.getText();
        if (target == null || target.isBlank()) {
            showAlert("提示", "当前没有可复制的译文", Alert.AlertType.WARNING);
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(target);
        Clipboard.getSystemClipboard().setContent(content);
        showAlert("成功", "译文已复制到剪贴板", Alert.AlertType.INFORMATION);
    }

    @FXML
    public void btnAddTag() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新增标签");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入标签名：");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String tagName = result.get().trim();
        if (tagName.isEmpty()) {
            showAlert("提示", "标签名不能为空", Alert.AlertType.WARNING);
            return;
        }
        try {
            tagDao.insert(tagName);
            reloadTags(tagName);
        } catch (SQLException e) {
            showAlert("新增失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void btnRenameTag() {
        String currentTag = cbWordType.getValue();
        if (currentTag == null || currentTag.isBlank()) {
            showAlert("提示", "请先选择需要修改的标签", Alert.AlertType.WARNING);
            return;
        }
        TextInputDialog dialog = new TextInputDialog(currentTag);
        dialog.setTitle("修改标签");
        dialog.setHeaderText(null);
        dialog.setContentText("请输入新的标签名：");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String newTagName = result.get().trim();
        if (newTagName.isEmpty()) {
            showAlert("提示", "标签名不能为空", Alert.AlertType.WARNING);
            return;
        }
        try {
            tagDao.rename(currentTag, newTagName);
            vocabDao.renameTag(currentTag, newTagName);
            reloadTags(newTagName);
        } catch (SQLException e) {
            showAlert("修改失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void btnDeleteTag() {
        String currentTag = cbWordType.getValue();
        if (currentTag == null || currentTag.isBlank()) {
            showAlert("提示", "请先选择需要删除的标签", Alert.AlertType.WARNING);
            return;
        }
        try {
            if (vocabDao.countActiveByTag(currentTag) > 0) {
                showAlert("删除失败", "当前标签下仍有单词，无法删除", Alert.AlertType.WARNING);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确认删除标签：" + currentTag + " ?", ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("删除标签");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                tagDao.delete(currentTag);
                reloadTags(null);
            }
        } catch (SQLException e) {
            showAlert("删除失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void btnOpenVocabPage() throws IOException {
        Stage stage = (Stage) cbWordType.getScene().getWindow();
        ViewUtil.switchView(VOCABULARY_VIEW_PAGE, stage);
    }

    private void reloadTags(String selectedTag) {
        try {
            List<String> tagNames = tagDao.findAllTagNames();
            cbWordType.getItems().setAll(tagNames);
            if (selectedTag != null && tagNames.contains(selectedTag)) {
                cbWordType.setValue(selectedTag);
            } else if (!tagNames.isEmpty()) {
                cbWordType.setValue(tagNames.getFirst());
            } else {
                cbWordType.setValue(null);
            }
        } catch (SQLException e) {
            showAlert("标签加载失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private LanguageDirection getSelectedDirection() {
        LanguageDirection direction = langMap.get(cbLang.getValue());
        if (direction == null) {
            return new LanguageDirection(null, "EN");
        }
        return direction;
    }

    private String buildTranslatedFileName(File inputFile) {
        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return fileName + "_translated";
        }
        return fileName.substring(0, dotIndex) + "_translated" + fileName.substring(dotIndex);
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
