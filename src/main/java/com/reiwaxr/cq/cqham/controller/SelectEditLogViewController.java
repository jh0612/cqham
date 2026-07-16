package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.RadioLogDAO;
import com.reiwaxr.cq.cqham.entity.RadioLog;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.reiwaxr.cq.cqham.common.PagePath.EDIT_LOG_DIALOG;
import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;

/**
 * @Description 通联日志检索编辑页面
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-22 16:06
 */
public class SelectEditLogViewController {
    private final RadioLogDAO logDAO = new RadioLogDAO();
    private final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private int totalRecord = 0;
    private List<RadioLog> cacheAllData; // 初始化加载的140条缓存

    // ========== 检索控件 ==========
    @FXML private TextField tfSearchCall;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbSearchWeather;
    @FXML private TextField tfSearchFreq;
    @FXML private ComboBox<String> cbSearchMode;
    @FXML private ComboBox<String> cbSearchQsl;

    // ========== 按钮 ==========
    @FXML private Button btnSearch;
    @FXML private Button btnReset;
    @FXML private Button btnEditDb;
    @FXML private Button btnDeleteDb;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Button btnBackMain;

    // ========== 分页信息 ==========
    @FXML private Label lbPageInfo;
    @FXML private Label lbTotalCount;

    // ========== 表格 ==========
    @FXML private TableColumn<RadioLog, String> idCol;
    @FXML private TableColumn<RadioLog, String> callSignCol;
    @FXML private TableColumn<RadioLog, String> qthCol;
    @FXML private TableColumn<RadioLog, String> frequencyCol;
    @FXML private TableColumn<RadioLog, String> modeCol;
    @FXML private TableColumn<RadioLog, LocalDate> dateCol;
    @FXML private TableColumn<RadioLog, String> timeCol;
    @FXML private TableColumn<RadioLog, String> deviceCol;
    @FXML private TableColumn<RadioLog, String> weatherCol;
    @FXML private TableColumn<RadioLog, String> signalReportCol;
    @FXML private TableColumn<RadioLog, String> powerCol;
    @FXML private TableColumn<RadioLog, String> qslStatusCol;
    @FXML private TableColumn<RadioLog, String> nameCol;
    @FXML private TableColumn<RadioLog, String> addressCol;
    @FXML private TableColumn<RadioLog, String> remarkCol;
    @FXML private TableView<RadioLog> tbSearchResult;
    private final ObservableList<RadioLog> tableData = FXCollections.observableArrayList();

    // ========== 可视化预留面板 ==========
    @FXML private BorderPane visualPanel;

    @FXML
    public void initialize() {
        cbSearchWeather.getItems().addAll("晴", "多云", "阴", "雨", "雾", "大风");
        cbSearchMode.getItems().addAll("FM","YSF","DMR");
        cbSearchQsl.getItems().addAll("已发送","已接收","未收发");
        tbSearchResult.setItems(tableData);
        // 页面初始化：加载最新140条，分页展示
        cacheAllData = logDAO.initLoadTop140();
        totalRecord = cacheAllData.size();

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

        refreshPageData();
        bindPageButton();
    }

    /**
     * 刷新当前页数据
     */
    private void refreshPageData() {
        tableData.clear();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, cacheAllData.size());
        tableData.addAll(cacheAllData.subList(start, end));
        updatePageLabel();
    }

    /**
     * 更新分页文字
     */
    private void updatePageLabel() {
        int totalPage = totalRecord % PAGE_SIZE == 0 ? totalRecord / PAGE_SIZE : totalRecord / PAGE_SIZE + 1;
        if(totalPage == 0) totalPage = 1;
        lbPageInfo.setText("第" + (currentPage + 1) + "页 / 共" + totalPage + "页");
        lbTotalCount.setText("总数据：" + totalRecord);
    }

    /**
     * 绑定分页上下页
     */
    private void bindPageButton() {
        btnPrevPage.setOnAction(e -> {
            if(currentPage > 0) {
                currentPage--;
                refreshPageData();
            }
        });
        btnNextPage.setOnAction(e -> {
            int maxPage = totalRecord / PAGE_SIZE;
            if(currentPage < maxPage) {
                currentPage++;
                refreshPageData();
            }
        });
    }

    /**
     * 【执行复合检索】任意条件组合查询
     */
    @FXML
    public void btnSearchClick() {
        // 解析呼号列表（英文逗号分割）
        List<String> callList = null;
        String callText = tfSearchCall.getText().trim();
        if(!callText.isBlank()){
            callList = Arrays.stream(callText.split(","))
                    .map(String::trim)
                    .filter(s->!s.isBlank())
                    .collect(Collectors.toList());
        }
        // 解析频率列表，最多200条
        List<String> freqList = null;
        String freqText = tfSearchFreq.getText().trim();
        if(!freqText.isBlank()){
            freqList = Arrays.stream(freqText.split(","))
                    .map(String::trim)
                    .filter(s->!s.isBlank())
                    .limit(200)
                    .collect(Collectors.toList());
        }
        String weather = cbSearchWeather.getValue();
        String mode = cbSearchMode.getValue();
        String qsl = cbSearchQsl.getValue();
        LocalDate sDate = dpStartDate.getValue();
        LocalDate eDate = dpEndDate.getValue();

        // 查询总条数
        totalRecord = logDAO.countByCondition(callList, sDate, eDate, weather, freqList, mode, qsl);
        // 重新全量加载符合条件数据（用于分页）
        cacheAllData = logDAO.queryByCondition(callList, sDate, eDate, weather, freqList, mode, qsl, 0, 99999);
        currentPage = 0;
        refreshPageData();
    }

    /**
     * 重置所有检索条件
     */
    @FXML
    public void btnResetClick() {
        tfSearchCall.clear();
        tfSearchFreq.clear();
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
        cbSearchWeather.setValue(null);
        cbSearchMode.setValue(null);
        cbSearchQsl.setValue(null);
        // 重置为初始140条数据
        cacheAllData = logDAO.initLoadTop140();
        totalRecord = cacheAllData.size();
        currentPage = 0;
        refreshPageData();
    }

    /**
     * 编辑选中数据（更新数据库）
     */
    @FXML
    public void btnEditDbClick() {
        RadioLog selected = tbSearchResult.getSelectionModel().getSelectedItem();
        if(selected == null){
            new Alert(Alert.AlertType.WARNING, "请选中一条数据库记录编辑").show();
            return;
        }
        try {
            // 1. 加载编辑弹窗FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(EDIT_LOG_DIALOG));
            Parent dialogRoot = loader.load();
            EditLogDialogController dialogCtrl = loader.getController();

            // 2. 传入选中数据，以及修改完成后的刷新回调
            dialogCtrl.setEditData(selected, unused -> {
                // 回调执行：重新加载检索数据刷新表格
                cacheAllData = logDAO.initLoadTop140();
                totalRecord = cacheAllData.size();
                currentPage = 0;
                refreshPageData();
                return null;
            });

            // 3. 创建弹窗窗口
            Stage dialogStage = new Stage();
            dialogStage.setTitle("编辑通联记录");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            // 关键：设置弹窗的所有者为当前页面窗口
            Stage ownerStage = (Stage) tbSearchResult.getScene().getWindow();
            dialogStage.initOwner(ownerStage);
            // 设置模态，父窗口完全锁定不可操作
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "打开编辑弹窗失败：" + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    /**
     * 删除数据库指定记录
     */
    @FXML
    public void btnDeleteDbClick() {
        RadioLog selected = tbSearchResult.getSelectionModel().getSelectedItem();
        if(selected == null){
            new Alert(Alert.AlertType.WARNING, "请选中要删除的记录").show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认删除数据库中ID="+selected.getId()+" 的记录？");
        confirm.showAndWait().ifPresent(btn -> {
            if(btn == ButtonType.OK){
                logDAO.deleteById(selected.getId());
                // 重新刷新数据
                cacheAllData = logDAO.initLoadTop140();
                totalRecord = cacheAllData.size();
                refreshPageData();
                new Alert(Alert.AlertType.INFORMATION, "删除成功").show();
            }
        });
    }

    /**
     * 返回主页面
     */
    @FXML
    public void backMain() throws IOException {
        Stage stage = (Stage) tbSearchResult.getScene().getWindow();
        ViewUtil.switchView(MAIN_PAGE, stage);
    }

}
