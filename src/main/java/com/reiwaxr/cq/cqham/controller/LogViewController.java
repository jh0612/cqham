package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.RadioLogDAO;
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
import static com.reiwaxr.cq.cqham.common.PagePath.SELECT_EDIT_LOG_VIEW;
import static com.reiwaxr.cq.cqham.utils.AllSpinnerValueFactory.getDoubleSpinnerValueFactory;

/**
 * @Description 通联日志页面控制器
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-12 0:06
 */
public class LogViewController {

    private final RadioLogDAO logDAO = new RadioLogDAO();

    // =====================  按钮 =====================
    /* 单选按钮-模拟模式 */
    @FXML public RadioButton radiobuttonFx;
    /* 单选按钮-数字模式ysf */
    @FXML public RadioButton radiobuttonYsf;
    /* 单选按钮-数字模式dmr */
    @FXML private RadioButton radiobuttonDmr;
    /* 返回主菜单按钮 */
    @FXML private Button btnBackMain;
    /* 新增日志按钮（仅存入内存表格，不入库） */
    @FXML private Button btnAdd;
    /* 清空输入框按钮 */
    @FXML private Button btnClear;
    /* 删除选中日志按钮（仅删除内存表格，不删库） */
    @FXML private Button btnDelete;
    /* 保存按钮：批量将表格所有缓存数据插入数据库 */
    @FXML private Button btnSave;
    /* 查询按钮 */
    @FXML private Button btnQuery;
    /* 编辑按钮 */
    @FXML private Button btnEdit;
    // =====================  表格 =====================
    /* 表格主键ID列 */
    @FXML private TableColumn<RadioLog, String> idCol;
    /* 表格呼号 */
    @FXML private TableColumn<RadioLog, String> callSignCol;
    /* 表格通联日期 */
    @FXML private TableColumn<RadioLog, LocalDate> dateCol;
    /* 表格通联时间 */
    @FXML private TableColumn<RadioLog, String> timeCol;
    /* 表格通联模式 */
    @FXML private TableColumn<RadioLog, String> modeCol;
    /* 表格频率 */
    @FXML private TableColumn<RadioLog, String> frequencyCol;
    /* 表格功率 */
    @FXML private TableColumn<RadioLog, String> powerCol;
    /* 表格QTH */
    @FXML private TableColumn<RadioLog, String> qthCol;
    /* 表格设备 */
    @FXML private TableColumn<RadioLog, String> deviceCol;
    /* 表格天气 */
    @FXML private TableColumn<RadioLog, String> weatherCol;
    /* 表格信号报告 */
    @FXML private TableColumn<RadioLog, String> signalReportCol;
    /* 表格姓名 */
    @FXML private TableColumn<RadioLog, String> nameCol;
    /* 表格QSL状态 */
    @FXML private TableColumn<RadioLog, String> qslStatusCol;
    /* 表格住址 */
    @FXML private TableColumn<RadioLog, String> addressCol;
    /* 表格备注 */
    @FXML private TableColumn<RadioLog, String> remarkCol;
    /* 日志列表表格 */
    @FXML private TableView<RadioLog> tbLogList;
    // ===================== Input =====================
    /* 呼号输入框 */
    @FXML private TextField tfCallSign;
    /* 天气选择框 */
    @FXML private ComboBox<String> cbWeather;
    /* 设备输入框 */
    @FXML private TextField tfDevice;
    /* 频率微调器 */
    @FXML private Spinner<Double> spfrequency;
    /* 姓名输入框 */
    @FXML public TextField tfname;
    /* QTH输入框 */
    @FXML private TextField tfQth;
    /* 住址输入框 */
    @FXML public TextField tfAddress;
    /* 信号报告下拉框 */
    @FXML private ChoiceBox<String> cbsignalReport;
    /* 功率下拉框 */
    @FXML public ChoiceBox<String> cbpower;
    /* QSL状态下拉框 */
    @FXML public ChoiceBox<String> cbqslstatus;
    /* 备注输入框 */
    @FXML private TextArea taRemark;
    /* 通联日期选择框 */
    @FXML private DatePicker dpConnectDate;
    /* 通联时间选择框 */
    @FXML private Spinner<String> spConnectTime;
    /* 频率模式单选框群组 */
    @FXML public ToggleGroup frequencyMode;
    // ===================== 常数定义等 =====================
    // 时分秒格式化器
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    //TODO: 初期化List，以后将数据源加入到List中即可显示数据
    LinkedList<RadioLog> tableList = new LinkedList<>();
    /* 表格内存缓存数据源（未入库临时数据） */
    private final ObservableList<RadioLog> logData = FXCollections.observableArrayList(tableList);

    /**
     * 画面初始化
     * 初始化调用refreshTable，该方法仅清空表格，不查询数据库
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
        // 初始化调用刷新表格，仅清空界面，不读取数据库
        refreshTable();
    }

    /**
     * 新增日志按钮
     * 仅添加到内存TableView缓存，不执行数据库插入操作
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
        log.setId(System.currentTimeMillis()); // 临时用时间戳当ID（内存临时标识）
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

        // 仅添加到内存表格，不操作数据库
        logData.add(log);
        new Alert(Alert.AlertType.INFORMATION, "日志已添加至表格缓存（未保存至数据库）！").show();
//        btnClearFormClick(); // 清空表单 test used
    }

    /**
     * 删除选中日志
     * 仅删除TableView内存缓存数据，不执行数据库删除
     */
    @FXML
    public void btnDeleteClick() {
        RadioLog selected = tbLogList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 仅移除内存列表，不调用DAO删库
            logData.remove(selected);
            new Alert(Alert.AlertType.INFORMATION, "已删除表格缓存记录（数据库未变动）").show();
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
     * 将表格缓存内所有数据批量存入数据库
     * 保存按钮一次性insert表格全部内容
     */
    @FXML
    public void btnSaveClick(ActionEvent actionEvent) {
        if(logData.isEmpty()){
            new Alert(Alert.AlertType.WARNING,"表格暂无待保存记录！").show();
            return;
        }
        // 批量插入内存列表全部数据至SQLite
        logDAO.batchInsert(logData);
        new Alert(Alert.AlertType.INFORMATION,"全部表格记录已保存至数据库！").show();
        // 保存完成清空表格缓存
        refreshTable();
    }

    /**
     * 需求5 查询按钮跳转查询页面
     */
    @FXML
    public void btnQueryClick(ActionEvent event) {
        // 校验内存缓存是否存在未保存数据
        if (!logData.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("未保存缓存提示");
            alert.setHeaderText("表格存在未保存记录，无法直接跳转");
            alert.setContentText("请选择操作：\n1.保存全部缓存至数据库\n2.清空表格缓存放弃数据");
            ButtonType btnSaveAll = new ButtonType("全部保存");
            ButtonType btnClearCache = new ButtonType("清空缓存");
            ButtonType btnCancel = new ButtonType("取消跳转");
            alert.getButtonTypes().setAll(btnSaveAll, btnClearCache, btnCancel);

            alert.showAndWait().ifPresent(type -> {
                if (type == btnSaveAll) {
                    // 批量保存全部缓存
                    logDAO.batchInsert(logData);
                    refreshTable();
                    // 执行跳转
                    doSwitchQueryPage(event);
                } else if (type == btnClearCache) {
                    // 直接清空缓存
                    refreshTable();
                    doSwitchQueryPage(event);
                }
            });
            return;
        }
        // 缓存为空，直接跳转
        doSwitchQueryPage(event);
    }

    /**
     * 当前页面编辑仅缓存未入库的数据（内存数据修改，不操作数据库）
     * @param event ActionEvent
     */
    @FXML
    public void btnEditClick(ActionEvent event) {
        RadioLog selected = tbLogList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "请选中需要编辑的记录").show();
            return;
        }

        // ========== 场景1：编辑当前页面内存缓存未入库数据 ==========
        // 1. 将选中行数据回填到页面所有输入控件
        tfCallSign.setText(selected.getCallSign());
        tfQth.setText(selected.getQth());
        spfrequency.getValueFactory().setValue(Double.parseDouble(selected.getFrequency()));
        // 匹配模式单选框
        String mode = selected.getMode();
        if ("FM".contains(mode)) {
            radiobuttonFx.setSelected(true);
        } else if ("YSF".contains(mode)) {
            radiobuttonYsf.setSelected(true);
        } else if ("DMR".contains(mode)) {
            radiobuttonDmr.setSelected(true);
        }
        dpConnectDate.setValue(selected.getConnectDate());
        spConnectTime.getValueFactory().setValue(selected.getConnectTime());
        tfDevice.setText(selected.getDevice());
        cbWeather.setValue(selected.getWeather());
        cbsignalReport.setValue(selected.getSignalReport());
        tfname.setText(selected.getName());
        cbpower.setValue(selected.getPower());
        cbqslstatus.setValue(selected.getQslStatus());
        tfAddress.setText(selected.getAddress());
        taRemark.setText(selected.getRemark());

        // 2. 把当前选中对象存入临时变量，用于更新覆盖
        RadioLog editTarget = selected;

        // 3. 修改新增按钮逻辑：点击新增时不再新增行，而是覆盖当前编辑对象
        // 替换原有btnAddClick逻辑，新增编辑保存分支
        btnAdd.setOnAction(e -> {
            // 表单校验（复用原有校验代码）
            String call = tfCallSign.getText().trim();
            Pattern pattern=Pattern.compile("^([A-Z]{1,3})(\\d)([A-Z]{1,3})$");
            if (call.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "呼号不能为空！").show();
                return;
            } else if (!pattern.matcher(call).matches()) {
                new Alert(Alert.AlertType.WARNING, "呼号不是正规呼号，或输错，请重试！").show();
                return;
            }
            LocalDate date = dpConnectDate.getValue();
            if (date == null) {
                new Alert(Alert.AlertType.WARNING, "请选择通联时间！").show();
                return;
            }
            String frequency = spfrequency.getEditor().getText().trim();
            Pattern patternFrequency=Pattern.compile("^([0-9]{1,3})(.)([0-9]{1,4})$");
            if (!patternFrequency.matcher(frequency).matches() || frequency.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "输入正确频率，请重试！").show();
                return;
            }
            RadioButton userData = (RadioButton) frequencyMode.getSelectedToggle();
            String selectedRadioButtonText = userData.getText();

            // 覆盖原有选中对象属性
            editTarget.setCallSign(call);
            editTarget.setQth(tfQth.getText().trim());
            editTarget.setFrequency(spfrequency.getEditor().getText().trim());
            editTarget.setMode(selectedRadioButtonText.trim());
            editTarget.setDevice(tfDevice.getText().trim());
            editTarget.setWeather(cbWeather.getValue());
            editTarget.setConnectDate(date);
            editTarget.setConnectTime(spConnectTime.getValue());
            editTarget.setSignalReport(cbsignalReport.getValue());
            editTarget.setName(tfname.getText().trim());
            editTarget.setPower(cbpower.getValue());
            editTarget.setQslStatus(cbqslstatus.getValue());
            editTarget.setAddress(tfAddress.getText().trim());
            editTarget.setRemark(taRemark.getText().trim());

            // 刷新表格视图
            tbLogList.refresh();
            new Alert(Alert.AlertType.INFORMATION, "缓存记录编辑完成（未同步数据库）").show();

            // 恢复新增按钮原有新增逻辑，解绑临时编辑事件
            btnAdd.setOnAction(originAddEvent -> btnAddClick());
            btnClearFormClick();
        });
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

    /**
     * 刷新表格数据
     * 仅清空界面表格缓存，不查询数据库
     */
    private void refreshTable() {
        // 移除原查询数据库逻辑，只清空内存列表
        tbLogList.getItems().clear();
        logData.clear();
    }

    /**
     * 跳转至SelectEditLogView查询编辑页面
     */
    private void doSwitchQueryPage(ActionEvent event) {
        try {
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            // 自行定义常量 SelectEditLogView 页面路径，替换你的PagePath常量
            ViewUtil.switchView(SELECT_EDIT_LOG_VIEW, stage);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "跳转查询页面失败：" + e.getMessage()).show();
            e.printStackTrace();
        }
    }
}

