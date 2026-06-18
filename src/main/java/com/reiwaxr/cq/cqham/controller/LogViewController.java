package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.entity.RadioLog;
import com.reiwaxr.cq.cqham.utils.TimeSpinnerValueFactory;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Pattern;

import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;
import static com.reiwaxr.cq.cqham.utils.AllSpinnerValueFactory.getDoubleSpinnerValueFactory;

/**
 * @Description 通联日志页面控制器
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-12 0:06
 */
public class LogViewController {

    // =====================  按钮 =====================
    /* 单选按钮-模拟模式 */
    @FXML
    public RadioButton radiobuttonFx;
    /* 单选按钮-数字模式ysf */
    @FXML
    public RadioButton radiobuttonYsf;
    /* 单选按钮-数字模式dmr */
    @FXML
    public RadioButton radiobuttonDmr;
    /* 返回主菜单按钮 */
    @FXML
    private Button btnBackMain;
    /* 新增日志按钮 */
    @FXML
    private Button btnAdd;
    /* 清空输入框按钮 */
    @FXML
    private Button btnClear;
    /* 删除选中日志按钮 */
    @FXML
    private Button btnDelete;
    /* 保存按钮 */
    @FXML
    private Button btnSave;

    // =====================  表格 =====================
    /* 表格主键ID列 */
    @FXML
    private TableColumn<RadioLog, String> idCol;

    /* 表格呼号 */
    @FXML
    private TableColumn<RadioLog, String> callSignCol;

    /* 表格通联日期 */
    @FXML
    private TableColumn<RadioLog, LocalDate> dateCol;

    /* 表格通联时间 */
    @FXML
    private TableColumn<RadioLog, String> timeCol;

    /* 表格通联模式 */
    @FXML
    private TableColumn<RadioLog, String> modeCol;

    /* 表格频率 */
    @FXML
    private TableColumn<RadioLog, String> frequencyCol;

    /* 表格功率 */
    @FXML
    private TableColumn<RadioLog, String> powerCol;

    /* 表格QTH */
    @FXML
    private TableColumn<RadioLog, String> qthCol;

    /* 表格设备 */
    @FXML
    private TableColumn<RadioLog, String> deviceCol;

    /* 表格天气 */
    @FXML
    private TableColumn<RadioLog, String> weatherCol;

    /* 表格信号报告 */
    @FXML
    private TableColumn<RadioLog, String> signalReportCol;

    /* 表格姓名 */
    @FXML
    private TableColumn<RadioLog, String> nameCol;

    /* 表格QSL状态 */
    @FXML
    private TableColumn<RadioLog, String> qslStatusCol;

    /* 表格住址 */
    @FXML
    private TableColumn<RadioLog, String> addressCol;

    /* 表格备注 */
    @FXML
    private TableColumn<RadioLog, String> remarkCol;

    /* 日志列表表格 */
    @FXML
    private TableView<RadioLog> tbLogList;

    // ===================== Input =====================
    /* 呼号输入框 */
    @FXML
    private TextField tfCallSign;

    /* 天气选择框 */
    @FXML
    private ComboBox<String> cbWeather;

    /* 设备输入框 */
    @FXML
    private TextField tfDevice;

    /* 频率微调器 */
    @FXML
    private Spinner<Double> spfrequency;

    /* 姓名输入框 */
    @FXML
    public TextField tfname;

    /* QTH输入框 */
    @FXML
    private TextField tfQth;

    /* 住址输入框 */
    @FXML
    public TextField tfAddress;

    /* 信号报告下拉框 */
    @FXML
    private ChoiceBox<String> cbsignalReport;

    /* 功率下拉框 */
    @FXML
    public ChoiceBox<String> cbpower;

    /* QSL状态下拉框 */
    @FXML
    public ChoiceBox<String> cbqslstatus;

    /* 备注输入框 */
    @FXML
    private TextArea taRemark;

    /* 通联日期选择框 */
    @FXML
    private DatePicker dpConnectDate;

    /* 通联时间选择框 */
    @FXML
    private Spinner<String> spConnectTime;

    /* 频率模式单选框群组 */
    @FXML
    public ToggleGroup frequencyMode;

    // ===================== 常数定义等 =====================
    // 时分格式化器
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");



    //TODO: 初期化List，以后将数据源加入到List中即可显示数据
    LinkedList<RadioLog> tableList = new LinkedList<>();
    /* 表格数据源 */
    private final ObservableList<RadioLog> logData = FXCollections.observableArrayList(tableList);

    /**
     * 画面初始化
     */
    @FXML
    public void initialize() {

        // 设置初始化时默认值
        tfCallSign.setText("BI1WCB"); //test used
        tfAddress.setText("北京-丰台-花乡");//test used
        tfname.setText("张三");//test used
        tfDevice.setText("八重洲-500d");//test used
        tfQth.setText("南五环-行驶中");//test used


        cbWeather.getItems().addAll("晴", "多云", "阴", "雨", "雾", "大风");
        cbWeather.setValue("晴");
        dpConnectDate.setValue(LocalDate.now());
        cbsignalReport.getItems().addAll("1/1","2/3(声音断续)","3/1","5/1","1/5","3/5","4/5(略有背噪)","5/5","1/9","3/9","5/9(最优清晰)");
        cbsignalReport.setValue("5/9(最优清晰)");
        cbpower.getItems().addAll("0.5W","1W","2.5W","5W","10W","25W","50W","75W","100W","100W+");
        cbpower.setValue("5W");
        cbqslstatus.getItems().addAll("已发送","已接收","未收发");
        cbqslstatus.setValue("未收发");

        DoubleSpinnerValueFactory factory = getDoubleSpinnerValueFactory();
        spfrequency.setValueFactory(factory);


        // 1. 构建自定义时间Spinner工厂
        SpinnerValueFactory<String> timeFactory = new TimeSpinnerValueFactory();
        spConnectTime.setValueFactory(timeFactory);

        // 开启编辑提交（输入完回车生效）
        spConnectTime.getEditor().setOnAction(event -> {
            String input = spConnectTime.getEditor().getText();
            if (checkTimeFormat(input)) {
                spConnectTime.getValueFactory().setValue(input);
            } else {
                // 格式错误恢复当前有效值
                spConnectTime.getEditor().setText(spConnectTime.getValue());
            }
        });

        // 2. 初始化填充当前系统时间
        String nowTime = TIME_FORMAT.format(new Date());
        spConnectTime.getValueFactory().setValue(nowTime);

        // 【可选】定时器每秒刷新显示当前时间，不需要则注释掉这段
        Timeline timeLine = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            String current = TIME_FORMAT.format(new Date());
            spConnectTime.getValueFactory().setValue(current);
        }));
        timeLine.setCycleCount(Timeline.INDEFINITE);
        timeLine.play();


        // 绑定表格列与实体字段
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        callSignCol.setCellValueFactory(new PropertyValueFactory<>("callSign"));
        qthCol.setCellValueFactory(new PropertyValueFactory<>("qth"));
        frequencyCol.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        modeCol.setCellValueFactory(new PropertyValueFactory<>("mode"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("connectDate"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("connectTime"));
        deviceCol.setCellValueFactory(new PropertyValueFactory<>("device"));
        weatherCol.setCellValueFactory(new PropertyValueFactory<>("weather"));
        signalReportCol.setCellValueFactory(new PropertyValueFactory<>("signalReport"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        powerCol.setCellValueFactory(new PropertyValueFactory<>("power"));
        qslStatusCol.setCellValueFactory(new PropertyValueFactory<>("qslStatus"));
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        remarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));

        tbLogList.setItems(logData);
    }

    /**
     * 新增日志按钮
     */
    @FXML
    public void btnAddClick() {
        // 表单校验-呼号
        String call = tfCallSign.getText().trim();
        Pattern pattern=Pattern.compile("^([A-Z]{1,3})(\\d)([A-Z]{1,3})$");// https://www.jyshare.com/front-end/854/  用于验证正则
        if (call.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "呼号不能为空！").show();
            return;
        } else if (!pattern.matcher(call).matches()) {
            new Alert(Alert.AlertType.WARNING, "呼号不是正规呼号，或输错，请重试！").show();
            return;
        }
        // 表单校验-日期
        LocalDate date = dpConnectDate.getValue();
        if (date == null) {
            new Alert(Alert.AlertType.WARNING, "请选择通联时间！").show();
            return;
        }
        // 表单校验-频率
        String frequency = spfrequency.getEditor().getText().trim();
        Pattern patternFrequency=Pattern.compile("^([0-9]{1,3})(.)([0-9]{1,4})$");
        if (!patternFrequency.matcher(frequency).matches() || frequency.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "输入正确频率，请重试！").show();
            return;
        }

        // 表单校验-通联模式
        RadioButton userData = (RadioButton) frequencyMode.getSelectedToggle();
        String selectedRadioButtonText = userData.getText();

        // 封装实体
        RadioLog log = new RadioLog();
        log.setId(System.currentTimeMillis()); // 临时用时间戳当ID
        log.setCallSign(call);
        log.setQth(tfQth.getText().trim());
        log.setFrequency(spfrequency.getEditor().getText().trim());
        log.setMode(selectedRadioButtonText.trim());
        log.setDevice(tfDevice.getText().trim());
        log.setWeather(cbWeather.getValue());
        log.setConnectDate(LocalDate.from(date.atStartOfDay()));
        log.setConnectTime(spConnectTime.getValue());
        log.setRemark(taRemark.getText().trim());
        log.setSignalReport(cbsignalReport.getValue());
        log.setName(tfname.getText().trim());
        log.setPower(cbpower.getValue());
        log.setQslStatus(cbqslstatus.getValue());
        log.setAddress(tfAddress.getText().trim());
        log.setRemark(taRemark.getText().trim());
        // 添加到表格
        //TODO: 是否添加到数据库或者哪儿，现在是在缓存中
        logData.add(log);
        new Alert(Alert.AlertType.INFORMATION, "日志添加成功！").show();
//        btnClearFormClick(); // 清空表单 test used
    }

    /**
     * 删除选中日志
     */
    @FXML
    public void btnDeleteClick() {
        RadioLog selected = tbLogList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            logData.remove(selected);
        } else {
            new Alert(Alert.AlertType.WARNING, "请先选中一条日志！").show();
        }
    }

    /**
     * 清空表单
     */
    @FXML
    public void btnClearFormClick() {
        tfCallSign.clear();
        cbWeather.setValue("晴");
        tfDevice.clear();
        tfname.clear();
        tfQth.clear();
        tfAddress.clear();
        cbsignalReport.setValue("5/9(最优清晰)");
        cbpower.setValue("5W");
        cbqslstatus.setValue("未收发");
        taRemark.clear();
        dpConnectDate.setValue(LocalDate.now());
        spfrequency.getValueFactory().setValue(438.500);

        radiobuttonFx.setSelected(true);
    }

    /**
     * 返回主菜单
     */
    @FXML
    public void btnBackMainClick(ActionEvent event){
        try {
            // 清空表单
            btnClearFormClick();
            // 获取当前窗口
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            // 跳转日志页面 fxml 路径
            ViewUtil.switchView(MAIN_PAGE, stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 校验格式 HH:mm
     */
    private boolean checkTimeFormat(String timeStr) {
        if (timeStr == null || !timeStr.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        String[] split = timeStr.split(":");
        try {
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}

