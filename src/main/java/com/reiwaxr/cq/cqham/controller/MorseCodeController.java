package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.entity.MorseItem;
import com.reiwaxr.cq.cqham.entity.RadioLog;
import com.reiwaxr.cq.cqham.utils.MorseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Map;

public class MorseCodeController {
    @FXML private TextField tfCharSep;
    @FXML private TextField tfWordSep;

    // 正向转换
    @FXML private TextArea taInputText;
    @FXML private Button btnToMorse;
    @FXML private Button btnCopyMorse;
    @FXML private TextArea taMorseResult;

    // 反向转换
    @FXML private TextArea taInputMorse;
    @FXML private Button btnToText;
    @FXML private Button btnCopyText;
    @FXML private TextArea taTextResult;

    @FXML private Button btnClearAll;
    @FXML private Button btnCloseWin;

    // 速查表
    @FXML private TableView<MorseItem> tbMorseTable;
    // ========== 表格 ==========
    @FXML private TableColumn<MorseItem, String> charKeyCol;
    @FXML private TableColumn<MorseItem, String> morseCodeCol;
    private final ObservableList<MorseItem> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // 绑定表格列与实体字段
        charKeyCol.setCellValueFactory(new PropertyValueFactory<>("charKey"));
        morseCodeCol.setCellValueFactory(new PropertyValueFactory<>("morseCode"));
        tbMorseTable.setItems(tableData);
        // 填充对照表
        for (Map.Entry<Character, String> entry : MorseUtil.CHAR_TO_MORSE.entrySet()) {
            tableData.add(new MorseItem(String.valueOf(entry.getKey()), entry.getValue()));
        }
        // 双向绑定实现数据同步
        taInputText.textProperty().bindBidirectional(taMorseResult.textProperty(), new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }
            @Override
            public String fromString(String string) {
                return MorseUtil.textToMorse(string, " ", "、");
            }
        });

        taInputMorse.textProperty().bindBidirectional(taTextResult.textProperty(), new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return object;
            }
            @Override
            public String fromString(String string) {
                return MorseUtil.morseToText(string, " ", "、");
            }
        });

    }

    // 文本转摩尔斯
    @FXML
    public void btnToMorseClick() {
        String text = taInputText.getText();
        String cSep = tfCharSep.getText();
        String wSep = tfWordSep.getText();
        String morse = MorseUtil.textToMorse(text, cSep, wSep);
        taMorseResult.setText(morse);
    }

    // 摩尔斯转回文本
    @FXML
    public void btnToTextClick() {
        String morse = taInputMorse.getText();
        String cSep = tfCharSep.getText();
        String wSep = tfWordSep.getText();
        String text = MorseUtil.morseToText(morse, cSep, wSep);
        taTextResult.setText(text);
    }

    // 复制摩尔斯码
    @FXML
    public void btnCopyMorseClick() {
        copyToClipboard(taMorseResult.getText());
    }

    // 复制文字
    @FXML
    public void btnCopyTextClick() {
        copyToClipboard(taTextResult.getText());
    }

    private void copyToClipboard(String content) {
        if (content.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "暂无内容可复制").show();
            return;
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(content);
        Clipboard.getSystemClipboard().setContent(cc);
        new Alert(Alert.AlertType.INFORMATION, "已复制到剪贴板").show();
    }

    // 清空所有输入输出
    @FXML
    public void btnClearAllClick() {
        taInputText.clear();
        taMorseResult.clear();
        taInputMorse.clear();
        taTextResult.clear();
    }

    // 关闭窗口
    @FXML
    public void btnCloseWinClick() {
        Stage stage = (Stage) btnCloseWin.getScene().getWindow();
        stage.close();
    }
}