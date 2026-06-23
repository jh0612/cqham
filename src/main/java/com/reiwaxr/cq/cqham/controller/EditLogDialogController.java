package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.RadioLogDAO;
import com.reiwaxr.cq.cqham.entity.RadioLog;
import com.reiwaxr.cq.cqham.utils.TimeSpinnerValueFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import static com.reiwaxr.cq.cqham.utils.AllSpinnerValueFactory.getDoubleSpinnerValueFactory;

public class EditLogDialogController {
    private final RadioLogDAO logDAO = new RadioLogDAO();
    // 待修改原始数据库记录
    private RadioLog targetLog;
    // 回调：修改成功后通知父页面刷新表格
    private Callback<Void, Void> refreshCallback;

    // ==========输入控件==========
    @FXML private TextField tfCallSign;
    @FXML private TextField tfQth;
    @FXML private Spinner<Double> spfrequency;
    @FXML private ToggleGroup modeGroup;
    @FXML private RadioButton rbFm;
    @FXML private RadioButton rbYsf;
    @FXML private RadioButton rbDmr;
    @FXML private DatePicker dpConnectDate;
    @FXML private Spinner<String> spConnectTime;
    @FXML private ComboBox<String> cbWeather;
    @FXML private TextField tfDevice;
    @FXML private ChoiceBox<String> cbSignalReport;
    @FXML private TextField tfName;
    @FXML private ChoiceBox<String> cbPower;
    @FXML private ChoiceBox<String> cbQslStatus;
    @FXML private TextField tfAddress;
    @FXML private TextArea taRemark;
    // ===========按钮===========
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML
    public void initialize() {
        // 初始化下拉框选项，和主页面保持一致
        cbWeather.getItems().addAll("晴", "多云", "阴", "雨", "雾", "大风");
        cbSignalReport.getItems().addAll("1/1","2/3(声音断续)","3/1","5/1","1/5","3/5","4/5(略有背噪)","5/5","1/9","3/9","5/9(最优清晰)");
        cbPower.getItems().addAll("0.5W","1W","2.5W","5W","10W","25W","50W","75W","100W","100W+");
        cbQslStatus.getItems().addAll("已发送","已接收","未收发");

        // 频率Spinner
        DoubleSpinnerValueFactory freqFactory = getDoubleSpinnerValueFactory();
        spfrequency.setValueFactory(freqFactory);

        // 时间Spinner
        TimeSpinnerValueFactory timeFactory = new TimeSpinnerValueFactory();
        spConnectTime.setValueFactory(timeFactory);
    }

    /**
     * 外部传入待编辑数据 + 刷新回调
     */
    public void setEditData(RadioLog log, Callback<Void, Void> callback) {
        this.targetLog = log;
        this.refreshCallback = callback;
        fillFormData(log);
    }

    /**
     * 将选中数据库记录回填到所有输入框
     */
    private void fillFormData(RadioLog log) {
        tfCallSign.setText(log.getCallSign());
        tfQth.setText(log.getQth());
        spfrequency.getValueFactory().setValue(Double.parseDouble(log.getFrequency()));

        // 匹配模式单选
        String mode = log.getMode();
        if (mode.contains("FM")){
            rbFm.setSelected(true);
        } else if (mode.contains("YSF")){
            rbYsf.setSelected(true);
        } else if (mode.contains("DMR")){
            rbDmr.setSelected(true);
        } else {
            System.out.println("啥也没设上");
        }

        dpConnectDate.setValue(log.getConnectDate());
        spConnectTime.getValueFactory().setValue(log.getConnectTime());
        cbWeather.setValue(log.getWeather());
        tfDevice.setText(log.getDevice());
        cbSignalReport.setValue(log.getSignalReport());
        tfName.setText(log.getName());
        cbPower.setValue(log.getPower());
        cbQslStatus.setValue(log.getQslStatus());
        tfAddress.setText(log.getAddress());
        taRemark.setText(log.getRemark());
    }

    /**
     * 提交修改按钮
     */
    @FXML
    public void btnSubmitClick() {
        // 1. 二次确认弹窗
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认修改");
        confirmAlert.setHeaderText("确定要将修改同步至数据库吗？");
        confirmAlert.setContentText("修改后原始数据会被覆盖，无法撤销");
        confirmAlert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                // 2. 读取表单新值覆盖原有实体属性
                RadioButton selectModeBtn = (RadioButton) modeGroup.getSelectedToggle();
                targetLog.setCallSign(tfCallSign.getText().trim());
                targetLog.setQth(tfQth.getText().trim());
                targetLog.setFrequency(spfrequency.getEditor().getText().trim());
                targetLog.setMode(selectModeBtn.getText());
                targetLog.setConnectDate(dpConnectDate.getValue());
                targetLog.setConnectTime(spConnectTime.getValue());
                targetLog.setWeather(cbWeather.getValue());
                targetLog.setDevice(tfDevice.getText().trim());
                targetLog.setSignalReport(cbSignalReport.getValue());
                targetLog.setName(tfName.getText().trim());
                targetLog.setPower(cbPower.getValue());
                targetLog.setQslStatus(cbQslStatus.getValue());
                targetLog.setAddress(tfAddress.getText().trim());
                targetLog.setRemark(taRemark.getText().trim());

                // 3. 更新数据库
                logDAO.update(targetLog);

                // 4. 关闭弹窗
                Stage dialogStage = (Stage) tfCallSign.getScene().getWindow();
                dialogStage.close();

                // 5. 回调父页面刷新表格数据
                if (refreshCallback != null) refreshCallback.call(null);

                new Alert(Alert.AlertType.INFORMATION, "数据修改并同步数据库成功！").show();
            }
        });
    }

    /**
     * 取消按钮，关闭弹窗
     */
    @FXML
    public void btnCancelClick() {
        Stage dialogStage = (Stage) tfCallSign.getScene().getWindow();
        dialogStage.close();
    }
}
