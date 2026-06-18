package com.reiwaxr.cq.cqham;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("HomeView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);
        // 设置窗口最小尺寸（防止缩得太小导致界面错乱）
        stage.setMinWidth(1100);
        stage.setMinHeight(750);
        // 可选：设置初始最大化
        // stage.setMaximized(true);
        // 允许窗口缩放（默认就是true，但显式设置更明确）
        stage.setResizable(true);
        stage.setTitle("Reiwaxr工具箱");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}