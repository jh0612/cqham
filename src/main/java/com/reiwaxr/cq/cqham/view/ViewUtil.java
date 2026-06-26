package com.reiwaxr.cq.cqham.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * @Description 页面切换工具类
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-12 0:07
 */
public class ViewUtil {
    /**
     * 切换页面（当前窗口跳转，不新建窗口）
     * @param fxmlPath fxml文件路径
     * @param stage 主窗口对象
     */
    public static void switchView(String fxmlPath, Stage stage, double width, double height) {
        try {
            URL resource = ViewUtil.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML file not found: " + fxmlPath);
            }
            Parent root = FXMLLoader.load(resource);
            Scene scene = new Scene(root, 1100, 750);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重载：使用原有窗口尺寸跳转
     */
    public static void switchView(String fxmlPath, Stage stage) throws IOException {
        switchView(fxmlPath, stage, stage.getWidth(), stage.getHeight());
    }
}
