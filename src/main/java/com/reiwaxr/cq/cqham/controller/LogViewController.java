package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.entity.RadioLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Description 通联日志页面控制器
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-12 0:06
 */
public class LogViewController {

    /* 新增日志按钮 */
    @FXML
    private Button btnAdd;

    /* 清空输入框按钮 */
    @FXML
    private Button btnClear;

    /* 删除选中日志按钮 */
    @FXML
    private Button btnDelete;

    /* QTH输入框 */
    @FXML
    private Button btnSave;

    /* 天气选择框 */
    @FXML
    private TableColumn<RadioLog, String> callSignCol;

    /* 设备输入框 */
    @FXML
    private ComboBox<String> cbWeather;

    /* 表格设备 */
    @FXML
    private TableColumn<RadioLog, String> deviceCol;

    /* 表格频率 */
    @FXML
    private TableColumn<RadioLog, String> frequencyCol;

    /* 表格主键ID列 */
    @FXML
    private TableColumn<RadioLog, String> idCol;

    /* 表格QTH */
    @FXML
    private TableColumn<RadioLog, String> qthCol;

    /* 日志列表表格 */
    @FXML
    private TableView<RadioLog> tbLogList;

    /* 表格通联时间 */
    @FXML
    private TableColumn<RadioLog, LocalDateTime> timeCol;

    /* 表格天气 */
    @FXML
    private TableColumn<RadioLog, String> weatherCol;

    /* 呼号输入框 */
    @FXML
    private TextField tfCallSign;

    /* 设备输入框 */
    @FXML
    private TextField tfDevice;

    /* 频率输入框 */
    @FXML
    private TextField tfFrequency;

    /* QTH输入框 */
    @FXML
    private TextField tfQth;

    /* 备注输入框 */
    @FXML
    private TextArea taRemark;

    /* 通联时间选择框 */
    @FXML
    private DatePicker dpConnectDate;

    // 表格数据源
    private final ObservableList<RadioLog> logData = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        cbWeather.getItems().addAll("晴", "多云", "阴", "雨", "雾", "大风");

        // 绑定表格列与实体字段
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        callSignCol.setCellValueFactory(new PropertyValueFactory<>("callSign"));
        qthCol.setCellValueFactory(new PropertyValueFactory<>("qth"));
        frequencyCol.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("connectTime"));
        deviceCol.setCellValueFactory(new PropertyValueFactory<>("device"));
        weatherCol.setCellValueFactory(new PropertyValueFactory<>("weather"));

        tbLogList.setItems(logData);
    }

    // 新增日志按钮
    @FXML
    public void btnAddClick() {
        // 简单表单校验
        String call = tfCallSign.getText().trim();
        if (call.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "呼号不能为空！").show();
            return;
        }
        LocalDate date = dpConnectDate.getValue();
        if (date == null) {
            new Alert(Alert.AlertType.WARNING, "请选择通联时间！").show();
            return;
        }

        // 封装实体
        RadioLog log = new RadioLog();
        log.setId(System.currentTimeMillis()); // 临时用时间戳当ID
        log.setCallSign(call);
        log.setQth(tfQth.getText().trim());
        log.setFrequency(tfFrequency.getText().trim());
        log.setDevice(tfDevice.getText().trim());
        log.setWeather(cbWeather.getValue());
        log.setConnectTime(date.atStartOfDay());
        log.setRemark(taRemark.getText().trim());

        // 添加到表格
        logData.add(log);
        new Alert(Alert.AlertType.INFORMATION, "日志添加成功！").show();
        clearForm(); // 清空表单
    }

    // 删除选中日志
    @FXML
    public void btnDeleteClick() {
        RadioLog selected = tbLogList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            logData.remove(selected);
        } else {
            new Alert(Alert.AlertType.WARNING, "请先选中一条日志！").show();
        }
    }

    // 清空表单
    @FXML
    public void clearForm() {
        tfCallSign.clear();
        tfQth.clear();
        tfFrequency.clear();
        tfDevice.clear();
        cbWeather.setValue(null);
        dpConnectDate.setValue(null);
        taRemark.clear();
    }
}

