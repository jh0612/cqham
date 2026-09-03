package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.AiAgentRunLogDao;
import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.entity.AiAgentRunRecord;
import com.reiwaxr.cq.cqham.entity.AiAgentStepLog;
import com.reiwaxr.cq.cqham.service.AiAgentService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent恢复执行对话框控制器
 */
public class AiAgentResumeDialogController {

    @FXML private Button btnDeleteRun;
    @FXML private ListView<AiAgentRunRecord> lvRuns;
    @FXML private Label lblRunStatus;
    @FXML private Label lblCurrentStep;
    @FXML private Label lblRecentError;
    @FXML private Label lblResumeHint;
    @FXML private ComboBox<AiAgentDefinition> cbResumeAgent;
    @FXML private ComboBox<String> cbResumeModel;
    @FXML private CheckBox chkResumeConfirm;
    @FXML private CheckBox chkResumeUseFileContext;
    @FXML private CheckBox chkResumeUseHistory;
    @FXML private ComboBox<String> cbResumeStrategy;
    @FXML private Spinner<Integer> spResumeMaxSteps;
    @FXML private TextArea taResumeGoal;
    @FXML private TextField tfResumeSceneName;
    @FXML private TextField tfResumeSubCategory;
    @FXML private TextArea taResumeScenePrompt;
    @FXML private TextArea taResumeFileContext;
    @FXML private TextArea taResumeHistorySummary;
    @FXML private ListView<AiAgentStepLog> lvStepLogs;
    @FXML private TextArea taStepInputDetail;
    @FXML private TextArea taStepOutputDetail;
    @FXML private TextArea taStepErrorDetail;

    private final AiAgentRunLogDao runLogDao = new AiAgentRunLogDao();
    private final AiAgentService agentService = new AiAgentService();

    private final List<AiAgentDefinition> availableAgents = new ArrayList<>();
    private String currentApiKey;
    private AiAgentRunRecord selectedRun;
    private AiAgentService.AgentExecutionRequest originalRequest;
    private ResumeSelection result;

    /**
     * 初始化
     */
    @FXML
    public void initialize() {
        lvRuns.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiAgentRunRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String stepText = item.getCurrentStepNo() != null && item.getCurrentStepNo() > 0
                        ? " / 当前步骤 " + item.getCurrentStepNo()
                        : "";
                setText(item.getStatus() + " / " + item.getAgentCode() + stepText + " / " + item.getUserGoal());
            }
        });
        lvStepLogs.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiAgentStepLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String errorHint = item.getErrorMessage() != null && !item.getErrorMessage().isBlank() ? " / 有错误" : "";
                setText("步骤 " + item.getStepNo() + " / " + item.getStepName() + " / " + item.getStatus() + errorHint);
            }
        });
        cbResumeAgent.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiAgentDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getAgentName());
            }
        });
        cbResumeAgent.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AiAgentDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getAgentName());
            }
        });
        cbResumeStrategy.getItems().setAll("保守", "标准", "深入");
        spResumeMaxSteps.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 6, 4));
        lvRuns.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadRunDetail(newValue));
        lvStepLogs.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateStepLogDetail(newValue));
        if (btnDeleteRun != null) {
            btnDeleteRun.setDisable(true);
        }
        cbResumeAgent.valueProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        cbResumeModel.valueProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        cbResumeStrategy.valueProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        chkResumeConfirm.selectedProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        chkResumeUseFileContext.selectedProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        chkResumeUseHistory.selectedProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        spResumeMaxSteps.valueProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        taResumeGoal.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        tfResumeSceneName.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        tfResumeSubCategory.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        taResumeScenePrompt.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        taResumeFileContext.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
        taResumeHistorySummary.textProperty().addListener((obs, oldValue, newValue) -> updateResumeHint());
    }

    /**
     * 初始化数据
     */
    public void initData(List<AiAgentDefinition> agentDefinitions, List<String> availableModels, String apiKey) {
        this.availableAgents.clear();
        this.availableAgents.addAll(agentDefinitions);
        this.currentApiKey = apiKey;
        cbResumeAgent.setItems(FXCollections.observableArrayList(this.availableAgents));
        cbResumeModel.getItems().setAll(availableModels);
        try {
            List<AiAgentRunRecord> runs = runLogDao.findRecoverableRuns(20);
            lvRuns.setItems(FXCollections.observableArrayList(runs));
            if (!runs.isEmpty()) {
                lvRuns.getSelectionModel().selectFirst();
                loadRunDetail(runs.getFirst());
            }
        } catch (Exception e) {
            showAlert("错误", "加载可恢复记录失败: " + e.getMessage());
        }
    }

    /**
     * 执行恢复
     */
    @FXML
    private void btnResumeClick() {
        if (selectedRun == null || originalRequest == null) {
            showAlert("提示", "请先选择一条可恢复记录");
            return;
        }
        AiAgentService.AgentExecutionRequest currentRequest = buildCurrentRequest();
        boolean deriveNewRun = hasUserChanges(currentRequest);
        result = new ResumeSelection(selectedRun, currentRequest, deriveNewRun);
        closeDialog();
    }

    /**
     * 刷新列表
     */
    @FXML
    private void btnRefreshClick() {
        initData(availableAgents, cbResumeModel.getItems(), currentApiKey);
    }

    /**
     * 删除记录
     */
    @FXML
    private void btnDeleteClick() {
        if (selectedRun == null) {
            showAlert("提示", "请先选择一条记录");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除后不可恢复");
        alert.setContentText("确定删除该恢复记录及其历史步骤日志吗？\nAgent: "
                + defaultText(selectedRun.getAgentCode())
                + "\n目标: "
                + defaultText(selectedRun.getUserGoal()));
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            runLogDao.deleteRunLog(selectedRun.getRunId());
            selectedRun = null;
            originalRequest = null;
            clearRunDetail();
            initData(availableAgents, cbResumeModel.getItems(), currentApiKey);
        } catch (Exception e) {
            showAlert("错误", "删除恢复记录失败: " + e.getMessage());
        }
    }

    /**
     * 关闭
     */
    @FXML
    private void btnCloseClick() {
        closeDialog();
    }

    public ResumeSelection getResult() {
        return result;
    }

    private void loadRunDetail(AiAgentRunRecord runRecord) {
        if (runRecord == null) {
            selectedRun = null;
            originalRequest = null;
            clearRunDetail();
            return;
        }
        selectedRun = runRecord;
        if (btnDeleteRun != null) {
            btnDeleteRun.setDisable(false);
        }
        try {
            originalRequest = restoreRequestSafely(runRecord);
            ensureAgentAvailable(originalRequest.agentDefinition());
            fillForm(runRecord, originalRequest);
            loadStepLogs(runRecord.getRunId());
        } catch (Exception e) {
            showAlert("错误", "读取恢复快照失败: " + e.getMessage());
        }
    }

    private AiAgentService.AgentExecutionRequest restoreRequestSafely(AiAgentRunRecord runRecord) throws Exception {
        try {
            return agentService.restoreRequestFromRun(runRecord);
        } catch (Exception ignored) {
            return buildFallbackRequest(runRecord);
        }
    }

    private AiAgentService.AgentExecutionRequest buildFallbackRequest(AiAgentRunRecord runRecord) {
        AiAgentDefinition definition = findAgentByCode(runRecord.getAgentCode());
        if (definition == null) {
            definition = new AiAgentDefinition();
            definition.setAgentCode(runRecord.getAgentCode());
            definition.setAgentName(runRecord.getAgentCode());
            definition.setTemplateCode(runRecord.getAgentCode());
            definition.setPlannerPrompt("");
            definition.setExecutionPrompt("");
            definition.setFinalSummaryEnabled(true);
            definition.setEnabled(true);
        }
        return new AiAgentService.AgentExecutionRequest(
                definition,
                currentApiKey == null ? "" : currentApiKey,
                cbResumeModel.getItems().isEmpty() ? "" : cbResumeModel.getItems().getFirst(),
                defaultText(runRecord.getUserGoal()),
                "",
                "",
                "",
                "",
                "",
                true,
                false,
                false,
                "标准",
                4);
    }

    private void ensureAgentAvailable(AiAgentDefinition requestAgent) {
        for (AiAgentDefinition definition : availableAgents) {
            if (definition.getAgentCode().equals(requestAgent.getAgentCode())) {
                return;
            }
        }
        availableAgents.add(requestAgent);
        cbResumeAgent.setItems(FXCollections.observableArrayList(availableAgents));
    }

    private void fillForm(AiAgentRunRecord runRecord, AiAgentService.AgentExecutionRequest request) {
        lblRunStatus.setText(defaultText(runRecord.getStatus()));
        lblCurrentStep.setText(defaultText(runRecord.getCurrentStepName()));
        lblRecentError.setText(defaultText(runRecord.getRecentError()));
        cbResumeAgent.setValue(findAgentByCode(request.agentDefinition().getAgentCode()));
        cbResumeModel.setValue(request.model());
        chkResumeConfirm.setSelected(request.confirmBeforeRun());
        chkResumeUseFileContext.setSelected(request.useFileContext());
        chkResumeUseHistory.setSelected(request.useChatHistory());
        cbResumeStrategy.setValue(request.strategy());
        spResumeMaxSteps.getValueFactory().setValue(request.maxSteps());
        taResumeGoal.setText(request.userInput());
        tfResumeSceneName.setText(request.sceneName());
        tfResumeSubCategory.setText(request.subCategory());
        taResumeScenePrompt.setText(request.scenePrompt());
        taResumeFileContext.setText(request.fileContext());
        taResumeHistorySummary.setText(request.historySummary());
        updateResumeHint();
    }

    private void loadStepLogs(String runId) throws Exception {
        List<AiAgentStepLog> stepLogs = runLogDao.findStepLogs(runId);
        lvStepLogs.setItems(FXCollections.observableArrayList(stepLogs));
        if (stepLogs.isEmpty()) {
            updateStepLogDetail(null);
            return;
        }
        lvStepLogs.getSelectionModel().select(stepLogs.getLast());
        updateStepLogDetail(stepLogs.getLast());
    }

    private void updateStepLogDetail(AiAgentStepLog stepLog) {
        if (stepLog == null) {
            taStepInputDetail.clear();
            taStepOutputDetail.clear();
            taStepErrorDetail.clear();
            return;
        }
        taStepInputDetail.setText(defaultText(stepLog.getStepInput()));
        taStepOutputDetail.setText(defaultText(stepLog.getStepOutput()));
        taStepErrorDetail.setText(defaultText(stepLog.getErrorMessage()));
    }

    private void clearRunDetail() {
        lblRunStatus.setText("-");
        lblCurrentStep.setText("-");
        lblRecentError.setText("-");
        lblResumeHint.setText("请选择可恢复记录");
        cbResumeAgent.setValue(null);
        cbResumeModel.setValue(null);
        chkResumeConfirm.setSelected(false);
        chkResumeUseFileContext.setSelected(false);
        chkResumeUseHistory.setSelected(false);
        cbResumeStrategy.setValue("标准");
        spResumeMaxSteps.getValueFactory().setValue(4);
        taResumeGoal.clear();
        tfResumeSceneName.clear();
        tfResumeSubCategory.clear();
        taResumeScenePrompt.clear();
        taResumeFileContext.clear();
        taResumeHistorySummary.clear();
        lvStepLogs.setItems(FXCollections.observableArrayList());
        updateStepLogDetail(null);
        if (btnDeleteRun != null) {
            btnDeleteRun.setDisable(true);
        }
    }

    private AiAgentService.AgentExecutionRequest buildCurrentRequest() {
        AiAgentDefinition baseDefinition = cbResumeAgent.getValue();
        AiAgentDefinition definition = new AiAgentDefinition();
        if (baseDefinition != null) {
            definition.setId(baseDefinition.getId());
            definition.setAgentCode(baseDefinition.getAgentCode());
            definition.setAgentName(baseDefinition.getAgentName());
            definition.setDescription(baseDefinition.getDescription());
            definition.setPlannerPrompt(baseDefinition.getPlannerPrompt());
            definition.setExecutionPrompt(baseDefinition.getExecutionPrompt());
            definition.setTemplateCode(baseDefinition.getTemplateCode());
            definition.setFinalSummaryEnabled(baseDefinition.isFinalSummaryEnabled());
            definition.setEnabled(baseDefinition.isEnabled());
            definition.setBuiltIn(baseDefinition.isBuiltIn());
        }
        return new AiAgentService.AgentExecutionRequest(
                definition,
                currentApiKey != null ? currentApiKey : originalRequest.apiKey(),
                defaultText(cbResumeModel.getValue()),
                defaultText(taResumeGoal.getText()),
                defaultText(tfResumeSceneName.getText()),
                defaultText(tfResumeSubCategory.getText()),
                defaultText(taResumeScenePrompt.getText()),
                defaultText(taResumeFileContext.getText()),
                defaultText(taResumeHistorySummary.getText()),
                chkResumeConfirm.isSelected(),
                chkResumeUseFileContext.isSelected(),
                chkResumeUseHistory.isSelected(),
                defaultText(cbResumeStrategy.getValue()),
                spResumeMaxSteps.getValue());
    }

    private boolean hasUserChanges(AiAgentService.AgentExecutionRequest currentRequest) {
        if (originalRequest == null) {
            return true;
        }
        boolean changed = !defaultText(currentRequest.agentDefinition().getAgentCode()).equals(defaultText(originalRequest.agentDefinition().getAgentCode()))
                || !defaultText(currentRequest.model()).equals(defaultText(originalRequest.model()))
                || !defaultText(currentRequest.userInput()).equals(defaultText(originalRequest.userInput()))
                || !defaultText(currentRequest.sceneName()).equals(defaultText(originalRequest.sceneName()))
                || !defaultText(currentRequest.subCategory()).equals(defaultText(originalRequest.subCategory()))
                || !defaultText(currentRequest.scenePrompt()).equals(defaultText(originalRequest.scenePrompt()))
                || !defaultText(currentRequest.fileContext()).equals(defaultText(originalRequest.fileContext()))
                || !defaultText(currentRequest.historySummary()).equals(defaultText(originalRequest.historySummary()))
                || currentRequest.confirmBeforeRun() != originalRequest.confirmBeforeRun()
                || currentRequest.useFileContext() != originalRequest.useFileContext()
                || currentRequest.useChatHistory() != originalRequest.useChatHistory()
                || !defaultText(currentRequest.strategy()).equals(defaultText(originalRequest.strategy()))
                || currentRequest.maxSteps() != originalRequest.maxSteps();
        lblResumeHint.setText(changed ? "检测到参数修改：将派生新运行继续" : "未修改关键参数：将继续原运行");
        return changed;
    }

    private void updateResumeHint() {
        if (originalRequest == null) {
            lblResumeHint.setText("请选择可恢复记录");
            return;
        }
        hasUserChanges(buildCurrentRequest());
    }

    private AiAgentDefinition findAgentByCode(String agentCode) {
        for (AiAgentDefinition definition : availableAgents) {
            if (definition.getAgentCode().equals(agentCode)) {
                return definition;
            }
        }
        return null;
    }

    private void closeDialog() {
        Stage stage = (Stage) lvRuns.getScene().getWindow();
        stage.close();
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public record ResumeSelection(
            AiAgentRunRecord sourceRun,
            AiAgentService.AgentExecutionRequest request,
            boolean deriveNewRun) {}
}