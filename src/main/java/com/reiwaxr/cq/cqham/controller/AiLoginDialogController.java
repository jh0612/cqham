package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.config.AiConfig;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * AI助手登录对话框控制器
 */
public class AiLoginDialogController {

    /* 用户名字段 */
    @FXML private TextField tfUsername;
    /* 密码字段 */
    @FXML private PasswordField pfPassword;
    /* 登录按钮 */
    @FXML private Button btnLogin;
    /* 直接使用API Key字段 */
    @FXML private PasswordField pfDirectApiKey;
    /* 直接使用API Key登录按钮 */
    @FXML private Button btnDirectLogin;

    /** 登录成功后返回的API Key */
    private String resultApiKey;
    /** 是否登录成功 */
    private boolean loginSuccess = false;

    /**
     * 管理员登录
     * @param event ActionEvent
     */
    @FXML
    private void btnLoginClick(ActionEvent event) {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("提示", "请输入账号和密码");
            return;
        }

        if (AiConfig.verifyAdmin(username, password)) {
            String adminApiKey = AiConfig.getAdminApiKey();
            if (adminApiKey.isBlank()) {
                showAlert("登录失败", "管理员API Key未配置，请使用环境变量 CQHAM_AI_ADMIN_API_KEY 或 JVM 参数 -Dcqham.ai.admin.apikey 注入");
                return;
            }
            this.resultApiKey = adminApiKey;
            this.loginSuccess = true;
            closeDialog();
        } else {
            showAlert("登录失败", "账号或密码错误");
        }
    }

    /**
     * 直接使用API Key登录
     * @param event ActionEvent
     */
    @FXML
    private void btnDirectLoginClick(ActionEvent event) {
        String apiKey = pfDirectApiKey.getText().trim();

        if (apiKey.isEmpty()) {
            showAlert("提示", "请输入API Key");
            return;
        }

        this.resultApiKey = apiKey;
        this.loginSuccess = true;
        closeDialog();
    }

    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) btnLogin.getScene().getWindow();
        stage.close();
    }

    /**
     * 显示警告对话框
     * @param title 标题
     * @param msg 消息内容
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * 获取登录结果的API Key
     * @return API Key
     */
    public String getResultApiKey() {
        return resultApiKey;
    }
    /**
     * 是否登录成功
     * @return true 如果登录成功，否则 false
     */
    public boolean isLoginSuccess() {
        return loginSuccess;
    }
}