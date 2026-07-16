package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.VocabularyDao;
import com.reiwaxr.cq.cqham.dao.VocabularyTagDao;
import com.reiwaxr.cq.cqham.entity.Vocabulary;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;

public class VocabularyViewController {
    @FXML
    private ComboBox<String> cbTagFilter;
    @FXML
    private TableView<Vocabulary> tbVocabulary;
    @FXML
    private TableColumn<Vocabulary, Integer> colId;
    @FXML
    private TableColumn<Vocabulary, String> colSource;
    @FXML
    private TableColumn<Vocabulary, String> colTarget;
    @FXML
    private TableColumn<Vocabulary, String> colTag;
    @FXML
    private TableColumn<Vocabulary, Object> colCreateTime;
    @FXML
    private Label lbCount;

    private final ObservableList<Vocabulary> tableData = FXCollections.observableArrayList();
    private final VocabularyDao vocabularyDao = new VocabularyDao();
    private final VocabularyTagDao tagDao = new VocabularyTagDao();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("sourceText"));
        colTarget.setCellValueFactory(new PropertyValueFactory<>("targetText"));
        colTag.setCellValueFactory(new PropertyValueFactory<>("tagName"));
        colCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        tbVocabulary.setItems(tableData);
        reloadTags();
        search();
    }

    @FXML
    public void btnSearch() {
        search();
    }

    @FXML
    public void btnReset() {
        cbTagFilter.setValue("全部");
        search();
    }

    @FXML
    public void btnDelete() {
        Vocabulary selected = tbVocabulary.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("提示", "请选择需要删除的单词", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认删除该单词吗？", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("删除确认");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            vocabularyDao.logicalDeleteById(selected.getId());
            search();
        } catch (SQLException e) {
            showAlert("删除失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void btnBackMain() throws IOException {
        Stage stage = (Stage) tbVocabulary.getScene().getWindow();
        ViewUtil.switchView(MAIN_PAGE, stage);
    }

    private void reloadTags() {
        try {
            List<String> tags = new ArrayList<>();
            tags.add("全部");
            tags.addAll(tagDao.findAllTagNames());
            cbTagFilter.getItems().setAll(tags);
            if (!tags.isEmpty()) {
                cbTagFilter.setValue(tags.getFirst());
            }
        } catch (SQLException e) {
            showAlert("标签加载失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void search() {
        try {
            String selectedTag = cbTagFilter.getValue();
            if (selectedTag != null && selectedTag.equals("全部")) {
                selectedTag = null;
            }
            tableData.setAll(vocabularyDao.listByTag(selectedTag));
            lbCount.setText("总数：" + tableData.size());
        } catch (SQLException e) {
            showAlert("查询失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}