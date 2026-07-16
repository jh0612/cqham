package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.entity.MorseItem;
import com.reiwaxr.cq.cqham.utils.MorseUtil;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.Map;

public class MorseCodeController {
    /** 字符间分隔符输入框。 */
    @FXML private TextField tfCharSep;
    /** 单词间分隔符输入框。 */
    @FXML private TextField tfWordSep;

    /** 英文文本输入区域。 */
    @FXML private TextArea taInputText;
    /** 复制摩尔斯结果按钮。 */
    @FXML private Button btnCopyMorse;
    /** 文本转换后的摩尔斯结果显示区域。 */
    @FXML private TextArea taMorseResult;

    /** 摩尔斯码输入区域。 */
    @FXML private TextArea taInputMorse;
    /** 复制原文结果按钮。 */
    @FXML private Button btnCopyText;
    /** 摩尔斯还原后的文本结果显示区域。 */
    @FXML private TextArea taTextResult;

    /** 清空全部输入输出按钮。 */
    @FXML private Button btnClearAll;
    /** 关闭窗口按钮。 */
    @FXML private Button btnCloseWin;

    /** 速查表容器。 */
    @FXML private FlowPane fpMorseTable;

    /** 速查表数据集合。 */
    private final ObservableList<MorseItem> tableData = FXCollections.observableArrayList();
    /** 防止双向联动时产生递归更新的标记。 */
    private boolean updating = false;

    /**
     * 初始化控制器，加载速查表数据，并绑定输入区域的实时转换逻辑。
     */
    @FXML
    public void initialize() {
        for (Map.Entry<Character, String> entry : MorseUtil.CHAR_TO_MORSE.entrySet()) {
            tableData.add(new MorseItem(String.valueOf(entry.getKey()), entry.getValue()));
        }
        tableData.sort(Comparator.comparing(MorseItem::getCharKey));
        renderMorseTable();
        bindAutoWrapToFlowPane();

        taInputText.textProperty().addListener((obs, oldVal, newVal) -> {
            if (updating) return;
            updating = true;
            taMorseResult.setText(MorseUtil.textToMorse(newVal, normalizeCharSep(tfCharSep.getText()), normalizeWordSep(tfWordSep.getText())));
            updating = false;
        });

        taInputMorse.textProperty().addListener((obs, oldVal, newVal) -> {
            if (updating) return;
            updating = true;
            taTextResult.setText(MorseUtil.morseToText(newVal, normalizeCharSep(tfCharSep.getText()), normalizeWordSep(tfWordSep.getText())));
            updating = false;
        });

        tfCharSep.textProperty().addListener((obs, oldVal, newVal) -> refreshFromInputs());
        tfWordSep.textProperty().addListener((obs, oldVal, newVal) -> refreshFromInputs());
    }

    /**
     * 绑定速查表容器的换行宽度，使其能够根据当前可用宽度自动换行。
     */
    private void bindAutoWrapToFlowPane() {
        if (fpMorseTable == null) {
            return;
        }
        fpMorseTable.setPrefWrapLength(760);
        fpMorseTable.widthProperty().addListener((obs, oldW, newW) -> updateWrapLength(newW.doubleValue()));
        fpMorseTable.sceneProperty().addListener((obs, oldScene, newScene) -> updateWrapLength(fpMorseTable.getWidth()));
        updateWrapLength(fpMorseTable.getWidth());
    }

    /**
     * 根据当前可用宽度更新 `FlowPane` 的换行长度。
     *
     * @param availableWidth 当前可用宽度。
     */
    private void updateWrapLength(double availableWidth) {
        if (fpMorseTable == null) {
            return;
        }
        double wrap = availableWidth > 0 ? Math.max(620, availableWidth - 20) : 760;
        fpMorseTable.setPrefWrapLength(wrap);
    }

    /**
     * 依据当前输入内容和分隔符设置，刷新两侧结果区域。
     */
    private void refreshFromInputs() {
        if (updating) return;
        updating = true;
        taMorseResult.setText(MorseUtil.textToMorse(taInputText.getText(), normalizeCharSep(tfCharSep.getText()), normalizeWordSep(tfWordSep.getText())));
        taTextResult.setText(MorseUtil.morseToText(taInputMorse.getText(), normalizeCharSep(tfCharSep.getText()), normalizeWordSep(tfWordSep.getText())));
        updating = false;
    }

    /**
     * 渲染摩尔斯码速查表，以卡片形式展示所有映射项。
     */
    private void renderMorseTable() {
        if (fpMorseTable == null) {
            return;
        }
        fpMorseTable.getChildren().clear();

        for (MorseItem item : tableData) {
            VBox card = new VBox(2);
            card.setStyle("-fx-background-color: rgba(0,0,0,0.03); -fx-background-radius: 8; -fx-padding: 6 8 6 8; -fx-min-width: 82; -fx-pref-width: 92; -fx-max-width: 100; -fx-cursor: hand;");

            Label key = new Label(item.getCharKey());
            key.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            Label value = new Label(item.getMorseCode());
            value.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
            card.getChildren().addAll(key, value);

            card.setOnMouseClicked(e -> handleMorseCardClick(card, item.getMorseCode()));
            fpMorseTable.getChildren().add(card);
        }
    }

    /**
     * 处理速查表卡片点击：复制内容并给出短暂高亮反馈。
     *
     * @param card  被点击的卡片。
     * @param morse 对应的摩尔斯码。
     */
    private void handleMorseCardClick(VBox card, String morse) {
//        copyToClipboard(morse);
        ClipboardContent cc = new ClipboardContent();
        cc.putString(morse+"  ");
        Clipboard.getSystemClipboard().setContent(cc);
        flashCard(card);
    }

    /**
     * 给点击的卡片增加短暂高亮反馈。
     *
     * @param card 被点击的速查表卡片。
     */
    private void flashCard(VBox card) {
        if (card == null) {
            return;
        }
        String originalStyle = card.getStyle();
        card.setStyle(originalStyle + " -fx-background-color: #d7ecff; -fx-border-color: #5aa9ff; -fx-border-width: 1; -fx-border-radius: 8;");

        PauseTransition pause = new PauseTransition(Duration.millis(220));
        pause.setOnFinished(e -> card.setStyle(originalStyle));
        pause.play();
    }

    /**
     * 规范化字符间分隔符。
     *
     * @param value 用户输入的字符间分隔符。
     * @return 合法分隔符；若为空则返回两个半角空格。
     */
    private String normalizeCharSep(String value) {
        if (value == null || value.isEmpty()) {
            return "  ";
        }
        return value;
    }

    /**
     * 规范化单词间分隔符。
     *
     * @param value 用户输入的单词间分隔符。
     * @return 合法分隔符；若为空则返回 ` / `。
     */
    private String normalizeWordSep(String value) {
        if (value == null || value.isEmpty()) {
            return " / ";
        }
        return value;
    }

    /**
     * 复制当前摩尔斯码结果到剪贴板。
     */
    @FXML
    public void btnCopyMorseClick() {
        copyToClipboard(taMorseResult.getText());
    }

    /**
     * 复制当前还原文本结果到剪贴板。
     */
    @FXML
    public void btnCopyTextClick() {
        copyToClipboard(taTextResult.getText());
    }

    /**
     * 将指定内容复制到系统剪贴板。
     *
     * @param content 要复制的内容。
     */
    private void copyToClipboard(String content) {
        if (content == null || content.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "暂无内容可复制").show();
            return;
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(content);
        Clipboard.getSystemClipboard().setContent(cc);
        new Alert(Alert.AlertType.INFORMATION, "已复制到剪贴板").show();
    }

    /**
     * 清空所有输入框和结果框内容。
     */
    @FXML
    public void btnClearAllClick() {
        taInputText.clear();
        taMorseResult.clear();
        taInputMorse.clear();
        taTextResult.clear();
    }

    /**
     * 关闭当前窗口。
     */
    @FXML
    public void btnCloseWinClick() {
        Stage stage = (Stage) btnCloseWin.getScene().getWindow();
        stage.close();
    }
}