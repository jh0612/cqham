package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * @Description 主界面控制器
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-11 23:43
 */
public class HomeViewController {

    /* 预留：其他工具按钮（示例） */
    @FXML
    private Button btnGoCall;

    /* 预留：其他工具按钮（示例） */
    @FXML
    private Button btnGoFreq;

    /* 跳转日志页面按钮 */
    @FXML
    private Button btnGoLog;

    /**
     跳转到通联日志页面
     */
    @FXML
    public void btnGoLogClick(ActionEvent event) {
        // 获取当前窗口
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        // 跳转日志页面 fxml 路径
        ViewUtil.switchView("src/main/resources/com/reiwaxr/cq/cqham/LogView.fxml", stage);
    }

    // 预留：其他工具按钮（示例）
    @FXML
    public void btnGoFreqClick(ActionEvent event) {
        // 后续新增页面在这里补充跳转逻辑
    }

    @FXML
    public void btnGoCallClick(ActionEvent event) {
        // 后续新增页面在这里补充跳转逻辑
    }

    @FXML
    void initialize() {
        assert btnGoCall != null : "fx:id=\"btnGoCall\" was not injected: check your FXML file 'HomeView.fxml'.";
        assert btnGoFreq != null : "fx:id=\"btnGoFreq\" was not injected: check your FXML file 'HomeView.fxml'.";
        assert btnGoLog != null : "fx:id=\"btnGoLog\" was not injected: check your FXML file 'HomeView.fxml'.";

    }

}
