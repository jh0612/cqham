package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.utils.converter.ConversionTask;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;

public class MarkdownToolController {

    @FXML private TextField inputPathField;
    @FXML private TextField outputPathField;
    @FXML private Button chooseFileBtn;
    @FXML private Button chooseFolderBtn;
    @FXML private Button browseOutputBtn;
    @FXML private Button convertBtn;
    @FXML private Button cancelBtn;
    @FXML private Button previewBtn;
    @FXML private Button outPathBtn;
    @FXML private Button btnBackMain;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private List<Path> selectedFiles = new ArrayList<>();
    private final List<Path> latestGeneratedFiles = new ArrayList<>();
    private Path outputDir;
    private ConversionTask currentTask;

    @FXML
    public void initialize() {
        progressBar.setProgress(0);
        statusLabel.setText("就绪");
        if (cancelBtn != null) {
            cancelBtn.setDisable(true);
        }
        if (previewBtn != null) {
            previewBtn.setDisable(true);
        }
    }

    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择文档文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Word 文档", "*.docx"),
                new FileChooser.ExtensionFilter("Excel 工作簿", "*.xlsx"),
                new FileChooser.ExtensionFilter("PDF 文档", "*.pdf"),
                new FileChooser.ExtensionFilter("PowerPoint 演示文稿", "*.ppt", "*.pptx"),
                new FileChooser.ExtensionFilter("所有支持文件", "*.docx", "*.xlsx", "*.pdf", "*.ppt", "*.pptx")
        );
        List<File> files = chooser.showOpenMultipleDialog(getStage());
        if (files != null && !files.isEmpty()) {
            selectedFiles.clear();
            files.forEach(f -> selectedFiles.add(f.toPath()));
            inputPathField.setText("已选 " + selectedFiles.size() + " 个文件");
            // 自动设置输出目录为第一个文件的父目录
            if (outputDir == null) {
                Path parent = selectedFiles.get(0).getParent();
                outputDir = parent;
                outputPathField.setText(parent.toString());
            }
            outPathBtn.setDisable(false);
        }
    }

    @FXML
    private void onChooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择包含文档的文件夹");
        File dir = chooser.showDialog(getStage());
        if (dir != null) {
            // 扫描文件夹下所有支持格式
            selectedFiles.clear();
            File[] files = dir.listFiles((d, name) ->
                    name.toLowerCase().endsWith(".docx")
                            || name.toLowerCase().endsWith(".xlsx")
                            || name.toLowerCase().endsWith(".pdf")
                            || name.toLowerCase().endsWith(".ppt")
                            || name.toLowerCase().endsWith(".pptx"));
            if (files != null) {
                for (File f : files) {
                    selectedFiles.add(f.toPath());
                }
            }
            inputPathField.setText("已选文件夹: " + dir.getAbsolutePath() + " (包含 " + selectedFiles.size() + " 个文件)");
            // 默认输出路径同输入路径
            outputDir = dir.toPath();
            outputPathField.setText(outputDir.toString());
            outPathBtn.setDisable(false);
        }
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择输出目录");
        File dir = chooser.showDialog(getStage());
        if (dir != null) {
            outputDir = dir.toPath();
            outputPathField.setText(outputDir.toString());
        }
    }

    @FXML
    private void onConvert() {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("请先选择文件或文件夹！");
            return;
        }
        if (outputDir == null) {
            statusLabel.setText("请指定输出目录！");
            return;
        }

        convertBtn.setDisable(true);
        if (cancelBtn != null) {
            cancelBtn.setDisable(false);
        }

        currentTask = new ConversionTask(new ArrayList<>(selectedFiles), outputDir);

        progressBar.progressProperty().bind(currentTask.progressProperty());
        statusLabel.textProperty().bind(currentTask.messageProperty());

        currentTask.setOnSucceeded(e -> {
            ConversionTask finishedTask = currentTask;
            resetTaskBindings(false);
            statusLabel.setText(finishedTask.getMessage());
            Path errorLogPath = finishedTask.getErrorLogFile();
            if (errorLogPath != null) {
                new Alert(Alert.AlertType.WARNING, "部分文件转换失败，日志已保存：\n" + errorLogPath).show();
            }
            List<Path> generated = finishedTask.getGeneratedMarkdownFiles();
            latestGeneratedFiles.clear();
            latestGeneratedFiles.addAll(generated);
            if (previewBtn != null) {
                previewBtn.setDisable(latestGeneratedFiles.isEmpty());
            }
            if (!generated.isEmpty()) {
                openPreviewWindow(generated.get(generated.size() - 1));
            }
        });

        currentTask.setOnFailed(e -> {
            String msg = currentTask.getException() == null ? "未知错误" : currentTask.getException().getMessage();
            resetTaskBindings(true);
            statusLabel.setText("转换出错: " + msg);
        });

        currentTask.setOnCancelled(e -> {
            resetTaskBindings(true);
            statusLabel.setText("转换已取消");
        });

        Thread worker = new Thread(currentTask);
        worker.setName("markdown-conversion-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onCancel() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
            statusLabel.textProperty().unbind();
            statusLabel.setText("正在取消，请稍候...");
        }
    }

    @FXML
    private void onPreview() {
        if (latestGeneratedFiles.isEmpty()) {
            statusLabel.setText("暂无可预览的转换结果");
            return;
        }
        openPreviewWindow(latestGeneratedFiles.get(latestGeneratedFiles.size() - 1));
    }

    @FXML
    private void onOutPath(ActionEvent actionEvent) {
        String outputDirPath = String.valueOf(outputDir); // 替换为实际路径


        if (outputDirPath == null || outputDirPath.trim().isEmpty()) {
            statusLabel.setText("输出目录尚未设置，请先选择输出目录。");
            return;
        }

        File dir = new File(outputDirPath);
        if (!dir.exists()) {
            // 目录不存在，可以尝试创建，或提示错误
            boolean created = dir.mkdirs();
            if (!created) {
                statusLabel.setText("输出目录不存在且无法创建：" + outputDirPath);
                return;
            }
        }

        try {
            Desktop.getDesktop().open(dir);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("无法打开输出目录：" + e.getMessage());
        }
    }

    @FXML
    private void btnBackMain() throws Exception {
        // 当前窗口获取
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        ViewUtil.switchView(MAIN_PAGE, stage);
    }

    private void resetTaskBindings(boolean resetProgressZero) {
        convertBtn.setDisable(false);
        if (cancelBtn != null) {
            cancelBtn.setDisable(true);
        }
        progressBar.progressProperty().unbind();
        if (resetProgressZero) {
            progressBar.setProgress(0);
        } else {
            progressBar.setProgress(1.0);
        }
        statusLabel.textProperty().unbind();
        currentTask = null;
    }

    private void openPreviewWindow(Path markdownFile) {
        try {
            String markdown = Files.readString(markdownFile, StandardCharsets.UTF_8);
            String html = markdownToHtml(markdown, markdownFile.getFileName().toString());

            Parent previewRoot = buildPreviewNode(html, markdown);
            BorderPane root = new BorderPane(previewRoot);
            Stage previewStage = new Stage();
            previewStage.setTitle("Markdown 预览 - " + markdownFile.getFileName());
            previewStage.setScene(new Scene(root, 980, 700));
            previewStage.initOwner(getStage());
            previewStage.initModality(Modality.NONE);
            previewStage.show();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "打开预览失败: " + ex.getMessage()).show();
        }
    }

    private Parent buildPreviewNode(String html, String markdown) {
        try {
            Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
            Object webView = webViewClass.getDeclaredConstructor().newInstance();
            Object engine = webViewClass.getMethod("getEngine").invoke(webView);
            engine.getClass().getMethod("loadContent", String.class, String.class).invoke(engine, html, "text/html");
            if (webView instanceof Parent parentNode) {
                return parentNode;
            }
        } catch (Exception ignored) {
            // WebView が利用できない場合はテキスト表示へフォールバック
        }

        TextArea textArea = new TextArea(markdown);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        return textArea;
    }

    private String markdownToHtml(String markdown, String title) {
        String escaped = escapeHtml(markdown);
        String body = escaped
                .replaceAll("(?m)^###### (.*)$", "<h6>$1</h6>")
                .replaceAll("(?m)^##### (.*)$", "<h5>$1</h5>")
                .replaceAll("(?m)^#### (.*)$", "<h4>$1</h4>")
                .replaceAll("(?m)^### (.*)$", "<h3>$1</h3>")
                .replaceAll("(?m)^## (.*)$", "<h2>$1</h2>")
                .replaceAll("(?m)^# (.*)$", "<h1>$1</h1>")
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br/>");

        return "<!doctype html><html><head><meta charset='UTF-8'><title>" + escapeHtml(title) + "</title>"
                + "<style>body{font-family:'Microsoft YaHei UI',sans-serif;background:#f6f8fa;color:#24292f;line-height:1.7;padding:20px;}"
                + "h1,h2,h3,h4,h5,h6{color:#0f172a;}"
                + "p{margin:0 0 10px 0;}"
                + "pre,code{background:#eaeef2;border-radius:6px;padding:2px 6px;}"
                + "table{border-collapse:collapse;margin:10px 0;}th,td{border:1px solid #d0d7de;padding:6px 10px;}</style></head>"
                + "<body><p>" + body + "</p></body></html>";
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Stage getStage() {
        return (Stage) chooseFileBtn.getScene().getWindow();
    }



}
