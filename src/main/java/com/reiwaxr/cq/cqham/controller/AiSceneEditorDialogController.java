package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.entity.AiSceneCategory;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * AI自定义场景编辑对话框控制器
 */
public class AiSceneEditorDialogController {

    @FXML private Label lblDialogTitle;
    @FXML private TextField tfSceneName;
    @FXML private TextField tfSubCategory;
    @FXML private TextField tfTags;
    @FXML private TextArea taRemark;
    @FXML private TextArea taSystemPrompt;
    @FXML private Button btnTrial;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private AiSceneCategory sourceScene;
    private AiSceneCategory resultScene;
    private boolean trialConfirmed;
    private boolean saveConfirmed;

    /**
     * 设置对话框初始数据
     * @param title 标题
     * @param initialScene 初始场景
     */
    public void setDialogData(String title, AiSceneCategory initialScene) {
        lblDialogTitle.setText(title);
        this.sourceScene = initialScene;
        this.resultScene = null;
        this.trialConfirmed = false;
        this.saveConfirmed = false;
        if (initialScene == null) {
            return;
        }

        tfSceneName.setText(initialScene.getSceneName());
        tfSubCategory.setText(initialScene.getSubCategory());
        tfTags.setText(initialScene.getTags());
        taRemark.setText(initialScene.getRemark());
        taSystemPrompt.setText(initialScene.getSystemPrompt());
    }

    /**
     * 仅本次试用
     */
    @FXML
    private void btnTrialClick() {
        AiSceneCategory scene = collectSceneFromForm();
        if (scene == null) {
            return;
        }
        this.resultScene = scene;
        this.trialConfirmed = true;
        this.saveConfirmed = false;
        closeDialog();
    }

    /**
     * 保存场景
     */
    @FXML
    private void btnSaveClick() {
        AiSceneCategory scene = collectSceneFromForm();
        if (scene == null) {
            return;
        }
        this.resultScene = scene;
        this.saveConfirmed = true;
        this.trialConfirmed = false;
        closeDialog();
    }

    /**
     * 取消编辑
     */
    @FXML
    private void btnCancelClick() {
        closeDialog();
    }

    /**
     * 采集表单数据
     * @return 场景实体
     */
    private AiSceneCategory collectSceneFromForm() {
        String sceneName = tfSceneName.getText() != null ? tfSceneName.getText().trim() : "";
        String subCategory = tfSubCategory.getText() != null ? tfSubCategory.getText().trim() : "";
        String systemPrompt = taSystemPrompt.getText() != null ? taSystemPrompt.getText().trim() : "";

        if (sceneName.isEmpty()) {
            showAlert("提示", "请输入场景分类");
            return null;
        }
        if (subCategory.isEmpty()) {
            showAlert("提示", "请输入子分类");
            return null;
        }
        if (systemPrompt.isEmpty()) {
            showAlert("提示", "请输入 System Prompt");
            return null;
        }

        AiSceneCategory scene = new AiSceneCategory();
        if (sourceScene != null) {
            scene.setId(sourceScene.getId());
            scene.setSortOrder(sourceScene.getSortOrder());
            scene.setCreateTime(sourceScene.getCreateTime());
            scene.setUpdateTime(sourceScene.getUpdateTime());
        }
        scene.setSceneName(sceneName);
        scene.setSubCategory(subCategory);
        scene.setSystemPrompt(systemPrompt);
        scene.setCustom(true);
        scene.setTags(normalizeOptionalText(tfTags.getText()));
        scene.setRemark(normalizeOptionalText(taRemark.getText()));
        return scene;
    }

    /**
     * 规范化可选文本
     * @param text 原文本
     * @return 规范化文本
     */
    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 关闭弹窗
     */
    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    /**
     * 警告提示
     * @param title 标题
     * @param message 内容
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public AiSceneCategory getResultScene() {
        return resultScene;
    }

    public boolean isTrialConfirmed() {
        return trialConfirmed;
    }

    public boolean isSaveConfirmed() {
        return saveConfirmed;
    }
}