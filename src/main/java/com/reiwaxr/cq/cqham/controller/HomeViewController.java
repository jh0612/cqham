package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import static com.reiwaxr.cq.cqham.common.PagePath.*;

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
    public void btnGoLogClick(ActionEvent event){
        try {
            // 获取当前窗口
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            // 跳转日志页面 fxml 路径
            ViewUtil.switchView(LOG_VIEW_PAGE, stage);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "打开通联日志页面失败："+e.getMessage()).show();
            throw new RuntimeException(e);
        }
    }

    /**
     * 跳转到通联日志检索页面
     * @param event ActionEvent
     */
    @FXML
    public void btnGoFreqClick(ActionEvent event) {
        try {
            // 获取当前窗口
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            // 跳转日志页面 fxml 路径
            ViewUtil.switchView(SELECT_EDIT_LOG_VIEW, stage);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "打开通联日志检索页面失败："+e.getMessage()).show();
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void btnGoCallClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MORSE_CODE_VIEW));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.setTitle("摩尔斯码速查互转工具");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            // 模态锁定父窗口
            Stage owner = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "打开摩尔斯工具失败："+e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    void initialize() {
        assert btnGoCall != null : "fx:id=\"btnGoCall\" was not injected: check your FXML file 'HomeView.fxml'.";
        assert btnGoFreq != null : "fx:id=\"btnGoFreq\" was not injected: check your FXML file 'HomeView.fxml'.";
        assert btnGoLog != null : "fx:id=\"btnGoLog\" was not injected: check your FXML file 'HomeView.fxml'.";

    }

}
