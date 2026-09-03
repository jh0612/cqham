package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.AiAgentDefinitionDao;
import com.reiwaxr.cq.cqham.dao.AiAgentRunLogDao;
import com.reiwaxr.cq.cqham.dao.AiSceneCategoryDao;
import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.entity.AiAgentRunRecord;
import com.reiwaxr.cq.cqham.entity.AiSceneCategory;
import com.reiwaxr.cq.cqham.config.AiConfig;
import com.reiwaxr.cq.cqham.service.AiAgentService;
import com.reiwaxr.cq.cqham.service.AiAgentRoutingService;
import com.reiwaxr.cq.cqham.service.FileParseService;
import com.reiwaxr.cq.cqham.service.QwenApiService;
import com.reiwaxr.cq.cqham.view.ViewUtil;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import okhttp3.Call;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static com.reiwaxr.cq.cqham.common.PagePath.AI_AGENT_MANAGER_DIALOG;
import static com.reiwaxr.cq.cqham.common.PagePath.AI_AGENT_RESUME_DIALOG;
import static com.reiwaxr.cq.cqham.common.PagePath.AI_SCENE_EDITOR_DIALOG;
import static com.reiwaxr.cq.cqham.common.PagePath.AI_SKILL_MANAGER_DIALOG;
import static com.reiwaxr.cq.cqham.common.PagePath.MAIN_PAGE;

/**
 * AI智能助手主页面控制器
 */
public class AiAssistantViewController {

    /* 最大最近消息数 */
    private static final int MAX_RECENT_MESSAGES = 8;
    /* 最大历史摘要字符数 */
    private static final int MAX_HISTORY_SUMMARY_CHARS = 3600;//1800
    /* 最大单条消息字符数 */
    private static final int MAX_SINGLE_MESSAGE_CHARS = 2400;//240
    /* 最大文件内容字符数 */
    private static final int MAX_FILE_CONTEXT_CHARS = 4800;//3600

    /* 全局API Key */
    private static String globalApiKey;

    /* UI组件 */
    @FXML private Label lblModelInfo;
    /* 返回主页面按钮 */
    @FXML private Button btnBack;
    /* Agent模式开关 */
    @FXML private CheckBox chkAgentMode;
    /* 执行前确认开关 */
    @FXML private CheckBox chkConfirmBeforeRun;
    /* 使用文件上下文开关 */
    @FXML private CheckBox chkUseFileContext;
    /* 使用历史对话开关 */
    @FXML private CheckBox chkUseChatHistory;
    /* 模型选择 */
    @FXML private ComboBox<String> cbModel;
    /* API Key输入框 */
    @FXML private PasswordField pfApiKey;
    /* 场景选择 */
    @FXML private ComboBox<String> cbScene;
    /* 子场景选择 */
    @FXML private ComboBox<String> cbSubCategory;
    /* 新建场景按钮 */
    @FXML private Button btnSceneNew;
    /* 编辑场景按钮 */
    @FXML private Button btnSceneEdit;
    /* 复制场景按钮 */
    @FXML private Button btnSceneCopy;
    /* 删除场景按钮 */
    @FXML private Button btnSceneDelete;
    /* 保存当前场景按钮 */
    @FXML private Button btnSceneSave;
    /* Agent配置容器 */
    @FXML private VBox vboxAgentConfig;
    /* Agent类型 */
    @FXML private ComboBox<String> cbAgentType;
    /* Agent说明 */
    @FXML private Label lblAgentTypeHint;
    /* Agent管理按钮 */
    @FXML private Button btnManageAgent;
    /* Skill管理按钮 */
    @FXML private Button btnManageSkill;
    /* 恢复执行按钮 */
    @FXML private Button btnResumeAgent;
    /* 可恢复记录提示 */
    @FXML private Label lblRecoverableRuns;
    /* Agent策略 */
    @FXML private ComboBox<String> cbAgentStrategy;
    /* 最大步骤数 */
    @FXML private Spinner<Integer> spAgentMaxSteps;
    /* 上传文件按钮 */
    @FXML private Button btnUploadFile;
    /* 文件名标签 */
    @FXML private Label lblFileName;
    /* 工作表选择 */
    @FXML private ComboBox<String> cbSheetName;
    /* 聊天记录滚动面板 */
    @FXML private ScrollPane scrollChat;
    /* 聊天记录容器 */
    @FXML private VBox vboxChatHistory;
    /* 用户输入框 */
    @FXML private TextArea taUserInput;
    /* 发送按钮 */
    @FXML private Button btnSend;
    /* 取消按钮 */
    @FXML private Button btnCancel;
    /* 清空按钮 */
    @FXML private Button btnClear;
    /* 重新生成按钮 */
    @FXML private Button btnRegenerate;
    /* Agent计划面板 */
    @FXML private TitledPane tpAgentPlan;
    /* Agent目标摘要 */
    @FXML private Label lblAgentPlanGoal;
    /* Agent计划状态 */
    @FXML private Label lblAgentPlanStatus;
    /* Agent计划摘要 */
    @FXML private TextArea taAgentPlanSummary;
    /* Agent步骤详情 */
    @FXML private TextArea taAgentStepDetail;
    /* Agent风险说明 */
    @FXML private TextArea taAgentRiskNotes;
    /* Agent步骤列表 */
    @FXML private ListView<String> lvAgentPlanSteps;
    /* 执行计划按钮 */
    @FXML private Button btnExecuteAgentPlan;
    /* 导出Markdown按钮 */
    @FXML private Button btnExportMd;
    /* 文件名输入框 */
    @FXML private TextField tfFileName;
    /* 状态标签 */
    @FXML private Label lblStatus;

    /* 场景分类数据访问对象 */
    private final AiSceneCategoryDao sceneDao = new AiSceneCategoryDao();
    /* Agent定义数据访问对象 */
    private final AiAgentDefinitionDao agentDefinitionDao = new AiAgentDefinitionDao();
    /* Agent运行日志数据访问对象 */
    private final AiAgentRunLogDao agentRunLogDao = new AiAgentRunLogDao();
    /* 文件解析服务 */
    private final FileParseService fileParseService = new FileParseService();
    /* API服务 */
    private QwenApiService apiService;
    /* Agent服务 */
    private final AiAgentService agentService = new AiAgentService();
    /* Agent路由服务 */
    private final AiAgentRoutingService agentRoutingService = new AiAgentRoutingService();
    /* Agent定义映射 */
    private final Map<String, AiAgentDefinition> agentDefinitionMap = new LinkedHashMap<>();
    /* 当前API Key */
    private String currentApiKey;
    /* 上传的文件 */
    private File uploadedFile;
    /* 文件内容 */
    private String fileContent;
    /* 当前子场景分类 */
    private List<AiSceneCategory> currentCategories = new ArrayList<>();
    /* 聊天记录 */
    private final List<QwenApiService.ChatMessage> chatHistory = new ArrayList<>();
    /* 当前任务 */
    private Task<?> currentTask;
    /* 当前网络请求 */
    private Call currentCall;
    /* 当前场景分类 */
    private AiSceneCategory currentScene;
    /* 当前试用中的临时场景 */
    private AiSceneCategory draftScene;
    /* 场景下拉框是否处于程序化更新中 */
    private boolean sceneSelectionUpdating;
    /* 待执行的Agent计划 */
    private AiAgentService.PreparedAgentRun pendingAgentRun;
    /* 待执行的Agent请求 */
    private AiAgentService.AgentExecutionRequest pendingAgentRequest;
    /* 当前展示中的Agent计划 */
    private AiAgentService.AgentPlan currentAgentPlan;
    /* 当前步骤结果映射 */
    private final Map<Integer, AiAgentService.AgentStepResult> currentAgentStepResultMap = new LinkedHashMap<>();
    /* 当前Agent步骤位置 */
    private int currentAgentStepNo;
    /* 是否手动取消 */
    private volatile boolean requestCancelled;

    /**
     * 静态方法：设置全局API Key
     */
    public static void setGlobalApiKey(String apiKey) {
        globalApiKey = apiKey;
    }

    /**
     * 初始化方法，由FXML加载后自动调用
     */
    @FXML
    public void initialize() {
        // 设置API Key
        if (globalApiKey != null) {
            setApiKey(globalApiKey);
            globalApiKey = null;
        }

        // 初始化模型选择
        cbModel.getItems().addAll(QwenApiService.getAvailableModels());
        String defaultModel = AiConfig.getDefaultModel();
        if (!cbModel.getItems().contains(defaultModel)) {
            cbModel.getItems().addFirst(defaultModel);
        }
        cbModel.setValue(defaultModel);
        lblModelInfo.setText("模型: " + defaultModel);
        cbModel.setOnAction(e -> {
            String model = cbModel.getValue();
            lblModelInfo.setText("模型: " + model);
        });

        cbScene.setOnAction(e -> {
            if (sceneSelectionUpdating) {
                return;
            }
            draftScene = null;
            loadSubCategories(cbScene.getValue());
        });
        cbSubCategory.setOnAction(e -> syncCurrentScene());

        initAgentControls();

        // 初始化场景分类
        loadSceneCategories();

        // 初始化Agent定义
        loadAgentDefinitions();
        refreshRecoverableRunHint();

        // 初始化API服务
        apiService = new QwenApiService();

        // 设置默认文件名
        updateDefaultFileName();
        updateSceneButtonState();
    }

    /**
     * 设置API Key（由登录对话框传入）
     * @param apiKey API Key
     */
    public void setApiKey(String apiKey) {
        this.currentApiKey = apiKey;
        pfApiKey.setText(apiKey);
    }

    /**
     * 加载场景分类
     */
    private void loadSceneCategories() {
        try {
            String selectedScene = cbScene.getValue();
            String selectedSubCategory = cbSubCategory.getValue();
            List<String> sceneNames = sceneDao.findAllSceneNames();
            cbScene.getItems().setAll(sceneNames);
            if (selectedScene != null && sceneNames.contains(selectedScene)) {
                cbScene.setValue(selectedScene);
                loadSubCategories(selectedScene);
                if (selectedSubCategory != null && cbSubCategory.getItems().contains(selectedSubCategory)) {
                    cbSubCategory.setValue(selectedSubCategory);
                    syncCurrentScene();
                }
            } else if (!sceneNames.isEmpty()) {
                cbScene.setValue(sceneNames.getFirst());
                loadSubCategories(sceneNames.getFirst());
            } else {
                cbSubCategory.getItems().clear();
                currentCategories = new ArrayList<>();
                currentScene = null;
                updateDefaultFileName();
                updateSceneButtonState();
            }
        } catch (Exception e) {
            showAlert("错误", "加载场景分类失败: " + e.getMessage());
        }
    }

    /**
     * 加载子分类
     * @param sceneName 场景名称
     */
    private void loadSubCategories(String sceneName) {
        try {
            if (sceneName == null || sceneName.isBlank()) {
                currentCategories = new ArrayList<>();
                cbSubCategory.getItems().clear();
                currentScene = null;
                updateDefaultFileName();
                return;
            }

            String selectedSubCategory = cbSubCategory.getValue();
            currentCategories = new ArrayList<>(sceneDao.findBySceneName(sceneName));
            if (draftScene != null && sceneName.equals(draftScene.getSceneName())) {
                mergeSceneIntoCurrentCategories(draftScene);
            }
            List<String> subCategories = new ArrayList<>();
            for (AiSceneCategory category : currentCategories) {
                String subCategory = category.getSubCategory();
                if (subCategory != null && !subCategory.isBlank() && !subCategories.contains(subCategory)) {
                    subCategories.add(subCategory);
                }
            }
            cbSubCategory.getItems().setAll(subCategories);
            if (!subCategories.isEmpty()) {
                String preferredSubCategory = selectedSubCategory;
                if (draftScene != null && sceneName.equals(draftScene.getSceneName())) {
                    preferredSubCategory = draftScene.getSubCategory();
                }
                cbSubCategory.setValue(subCategories.contains(preferredSubCategory)
                        ? preferredSubCategory
                        : subCategories.getFirst());
            } else {
                currentScene = null;
            }
            syncCurrentScene();
        } catch (Exception e) {
            showAlert("错误", "加载子分类失败: " + e.getMessage());
        }
    }

    /**
     * 同步当前选中的场景配置
     */
    private void syncCurrentScene() {
    
        String sceneName = cbScene.getValue();
        String subCategory = cbSubCategory.getValue();

        currentScene = null;
        for (AiSceneCategory category : currentCategories) {
            if (category.getSceneName().equals(sceneName)
                    && category.getSubCategory().equals(subCategory)) {
                currentScene = category;
                break;
            }
        }

        updateDefaultFileName();
        updateSceneButtonState();
    }

    /**
     * 新建自定义场景
     * @param event 点击事件
     */
    @FXML
    private void btnSceneNewClick(ActionEvent event) {
        openSceneEditorDialog("新建自定义场景", null);
    }

    /**
     * 编辑当前自定义场景
     * @param event 点击事件
     */
    @FXML
    private void btnSceneEditClick(ActionEvent event) {
        if (currentScene == null) {
            showAlert("提示", "请先选择一个场景");
            return;
        }
        if (!currentScene.isCustom()) {
            showAlert("提示", "内置场景不支持直接编辑，请使用复制后再修改");
            return;
        }
        openSceneEditorDialog("编辑自定义场景", copyScene(currentScene, false));
    }

    /**
     * 复制当前场景
     * @param event 点击事件
     */
    @FXML
    private void btnSceneCopyClick(ActionEvent event) {
        if (currentScene == null) {
            showAlert("提示", "请先选择一个场景");
            return;
        }
        openSceneEditorDialog("复制场景", copyScene(currentScene, true));
    }

    /**
     * 删除当前自定义场景
     * @param event 点击事件
     */
    @FXML
    private void btnSceneDeleteClick(ActionEvent event) {
        if (currentScene == null) {
            showAlert("提示", "请先选择一个场景");
            return;
        }
        if (!currentScene.isCustom() || currentScene.getId() == null) {
            showAlert("提示", "只有已保存的自定义场景可以删除");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认删除当前自定义场景？");
        confirm.setTitle("删除确认");
        confirm.setHeaderText(currentScene.getSceneName() + " / " + currentScene.getSubCategory());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    sceneDao.deleteById(currentScene.getId());
                    draftScene = null;
                    loadSceneCategories();
                    lblStatus.setText("自定义场景已删除");
                } catch (Exception e) {
                    showAlert("错误", "删除场景失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 保存当前试用场景
     * @param event 点击事件
     */
    @FXML
    private void btnSceneSaveClick(ActionEvent event) {
        if (currentScene == null) {
            showAlert("提示", "当前没有可保存的场景");
            return;
        }
        if (!currentScene.isCustom()) {
            showAlert("提示", "内置场景不能直接保存，请先复制为自定义场景");
            return;
        }

        try {
            persistCustomScene(copyScene(currentScene, false));
        } catch (Exception e) {
            showAlert("错误", "保存场景失败: " + e.getMessage());
        }
    }

    /**
     * 返回首页
     * @param event ActionEvent
     */
    @FXML
    private void btnBackClick(ActionEvent event) {
        try {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            ViewUtil.switchView(MAIN_PAGE, stage);
        } catch (IOException e) {
            showAlert("错误", "返回主页失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     * @param event ActionEvent
     */
    @FXML
    private void btnUploadFileClick(ActionEvent event) {
        Stage stage = (Stage) btnUploadFile.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("Word文件", "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("文本文件", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("所有支持的文件", "*.xlsx", "*.xls", "*.docx", "*.doc", "*.txt", "*.md")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        // 检查文件大小
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.length() > maxSize) {
            showAlert("警告", "文件大小超过5MB限制");
            return;
        }

        this.uploadedFile = file;
        lblFileName.setText(file.getName());

        // 如果是Excel文件，显示Sheet选择
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            cbSheetName.setVisible(true);
            loadSheetNames(file);
        } else {
            cbSheetName.setVisible(false);
        }

        // 解析文件内容
        parseFileContent();
    }

    /**
     * 加载Excel的Sheet名称
     * @param file Excel文件
     */
    private void loadSheetNames(File file) {
        // TODO: 实现Excel Sheet名称读取
        // 这里简化处理，实际应该使用Apache POI读取
        cbSheetName.getItems().clear();
        cbSheetName.getItems().add("Sheet1");
        cbSheetName.setValue("Sheet1");
    }

    /**
     * 解析文件内容
     */
    private void parseFileContent() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String sheetName = cbSheetName.isVisible() ? cbSheetName.getValue() : null;
                return fileParseService.parseFile(uploadedFile, sheetName);
            }
        };

        task.setOnSucceeded(e -> {
            fileContent = task.getValue();
            lblStatus.setText("文件解析完成");
        });

        task.setOnFailed(e -> {
            showAlert("错误", "文件解析失败: " + task.getException().getMessage());
            lblStatus.setText("文件解析失败");
        });

        new Thread(task).start();
    }

    /**
     * 发送消息
     * @param event ActionEvent
     */
    @FXML
    private void btnSendClick(ActionEvent event) {
        String userInput = taUserInput.getText().trim();
        if (userInput.isEmpty()) {
            showAlert("提示", "请输入问题");
            return;
        }

        currentApiKey = pfApiKey.getText().trim();

        if (currentApiKey == null || currentApiKey.isEmpty()) {
            showAlert("错误", "API Key未设置");
            return;
        }

        if (isAgentModeEnabled()) {
            handleAgentSend(userInput);
            return;
        }

        // 添加用户消息到历史
        chatHistory.add(new QwenApiService.ChatMessage("user", userInput));

        // 显示用户消息
        addMessageToChat("user", userInput);

        // 清空输入框
        taUserInput.clear();

        // 禁用发送按钮，启用取消按钮
        btnSend.setDisable(true);
        btnCancel.setDisable(false);
        lblStatus.setText("正在生成...");

        // 显示加载提示
        Label loadingLabel = new Label("AI 正在思考...");
        loadingLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
        vboxChatHistory.getChildren().add(loadingLabel);

        // 异步调用API
        requestCancelled = false;
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String model = cbModel.getValue();
                String systemPrompt = currentScene != null ? currentScene.getSystemPrompt() : null;
                List<QwenApiService.ChatMessage> requestMessages = buildRequestMessages();
                currentCall = apiService.createChatCall(currentApiKey, model, systemPrompt, requestMessages);
                try {
                    return apiService.executeChat(currentCall);
                } finally {
                    currentCall = null;
                }
            }
        };
        currentTask = task;

        // 设置任务完成后的处理
        task.setOnSucceeded(e -> {
            String response = task.getValue();
            chatHistory.add(new QwenApiService.ChatMessage("assistant", response));

            // 移除加载提示
            vboxChatHistory.getChildren().remove(loadingLabel);

            // 显示AI回复
            addMessageToChat("assistant", response);

            // 恢复按钮状态
            btnSend.setDisable(false);
            btnCancel.setDisable(true);
            lblStatus.setText("生成完成");
            currentTask = null;
            requestCancelled = false;

            // 更新默认文件名
            updateDefaultFileName();
        });

        // 设置任务失败后的处理
        task.setOnFailed(e -> {
            // 移除加载提示
            vboxChatHistory.getChildren().remove(loadingLabel);

            btnSend.setDisable(false);
            btnCancel.setDisable(true);
            currentTask = null;

            if (requestCancelled || isCancelledException(task.getException())) {
                lblStatus.setText("已取消");
            } else {
                showAlert("错误", "生成失败: " + task.getException().getMessage());
                lblStatus.setText("生成失败");
            }
            requestCancelled = false;
        });

        task.setOnCancelled(e -> {
            vboxChatHistory.getChildren().remove(loadingLabel);
            btnSend.setDisable(false);
            btnCancel.setDisable(true);
            currentTask = null;
            currentCall = null;
            lblStatus.setText("已取消");
            requestCancelled = false;
        });

        new Thread(task).start();
    }

    /**
     * 取消生成
     * @param event ActionEvent
     */
    @FXML
    private void btnCancelClick(ActionEvent event) {
        if (currentTask == null && pendingAgentRun != null) {
            agentService.markRunCancelled(pendingAgentRun);
            clearPendingAgentPlan(false);
            lblStatus.setText("已取消待执行计划");
            return;
        }

        requestCancelled = true;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    /**
     * 取消由来の例外か判定する
     * @param throwable 例外
     * @return true: 手动取消
     */
    private boolean isCancelledException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof CancellationException || throwable instanceof InterruptedException) {
            return true;
        }
        String message = throwable.getMessage();
        return message != null && message.toLowerCase().contains("canceled");
    }

    /**
     * 清空对话
     * @param event ActionEvent
     */
    @FXML
    private void btnClearClick(ActionEvent event) {
        chatHistory.clear();
        vboxChatHistory.getChildren().clear();
        clearPendingAgentPlan(true);
        lblStatus.setText("对话已清空");
    }

    /**
     * 重新生成
     * @param event ActionEvent
     */
    @FXML
    private void btnRegenerateClick(ActionEvent event) {
        if (isAgentModeEnabled()) {
            showAlert("提示", "Agent 模式暂不支持重新生成，请直接重新发起任务");
            return;
        }
        if (chatHistory.isEmpty()) {
            showAlert("提示", "没有可重新生成的内容");
            return;
        }

        // 移除最后一条AI回复
        if (chatHistory.size() >= 2) {
            chatHistory.removeLast();
            // 移除界面上最后一条AI消息
            if (!vboxChatHistory.getChildren().isEmpty()) {
                vboxChatHistory.getChildren().removeLast();
            }

            // 重新发送最后一条用户消息
            QwenApiService.ChatMessage lastUserMsg = chatHistory.getLast();
            taUserInput.setText(lastUserMsg.content());
            chatHistory.removeLast();
            btnSendClick(event);
        }
    }

    /**
     * 导出为Markdown
     * @param event ActionEvent
     */
    @FXML
    private void btnExportMdClick(ActionEvent event) {
        if (chatHistory.isEmpty()) {
            showAlert("提示", "没有可导出的内容");
            return;
        }

        Stage stage = (Stage) btnExportMd.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存Markdown文件");

        String fileName = tfFileName.getText().trim();
        if (fileName.isEmpty()) {
            fileName = updateDefaultFileName();
        }
        if (!fileName.endsWith(".md")) {
            fileName += ".md";
        }
        fileChooser.setInitialFileName(fileName);

        File file = fileChooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            // 写入标题
            writer.write("# AI 对话记录\n\n");
            writer.write("**生成时间**: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
            writer.write("**使用模型**: " + cbModel.getValue() + "\n\n");
            if (currentScene != null) {
                writer.write("**场景**: " + currentScene.getSceneName() + " - " + currentScene.getSubCategory() + "\n\n");
            }
            writer.write("---\n\n");

            // 写入对话内容
            for (QwenApiService.ChatMessage msg : chatHistory) {
                if ("user".equals(msg.role())) {
                    writer.write("## 用户提问\n\n");
                    writer.write(msg.content() + "\n\n");
                } else if ("assistant".equals(msg.role())) {
                    writer.write("## AI 回复\n\n");
                    writer.write(msg.content() + "\n\n");
                }
                writer.write("---\n\n");
            }

            showAlert("成功", "Markdown文件已保存到:\n" + file.getAbsolutePath());
            lblStatus.setText("导出成功");
        } catch (IOException e) {
            showAlert("错误", "导出失败: " + e.getMessage());
        }
    }

    /**
     * 添加消息到对话区域
     * @param role 消息角色（user/assistant）
     * @param content 消息内容
     */
    private void addMessageToChat(String role, String content) {
        Platform.runLater(() -> {
            HBox messageRow = new HBox();
            messageRow.setFillHeight(true);
            messageRow.setAlignment(role.equals("user") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox messageBox = new VBox(5);
            messageBox.setPadding(new Insets(10));
            messageBox.setMaxWidth(680);
            messageBox.setFillWidth(true);

            HBox headerBox = new HBox(8);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            // 角色标签
            Label roleLabel = new Label(role.equals("user") ? "👤 用户" : "\uD83E\uDD16 AI");
            roleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            roleLabel.setStyle(role.equals("user") 
                ? "-fx-text-fill: #2c3e50;" 
                : "-fx-text-fill: #27ae60;");

            Region headerSpacer = new Region();
            HBox.setHgrow(headerSpacer, Priority.ALWAYS);

            Button copyButton = createCopyButton(content);

            // 内容文本直接展示，避免内部滚动条
            Label contentLabel = createMessageContentLabel(content);
            
            // 设置背景色
            messageBox.setStyle(role.equals("user")
                ? "-fx-background-color: #ecf0f1; -fx-background-radius: 8;"
                : "-fx-background-color: #e8f5e9; -fx-background-radius: 8;");

            headerBox.getChildren().addAll(roleLabel, headerSpacer, copyButton);
            messageBox.getChildren().addAll(headerBox, contentLabel);
            if (role.equals("user")) {
                messageRow.getChildren().addAll(spacer, messageBox);
            } else {
                messageRow.getChildren().addAll(messageBox, spacer);
            }
            vboxChatHistory.getChildren().add(messageRow);

            // 滚动到底部
            scrollChat.setVvalue(1.0);
        });
    }

    /**
     * 添加Agent消息卡片
     * @param title 标题
     * @param content 内容
     * @param titleColor 标题色
     * @param background 背景色
     */
    private void addAgentMessageToChat(String title, String content, String titleColor, String background) {
        Platform.runLater(() -> {
            VBox messageBox = new VBox(5);
            messageBox.setPadding(new Insets(10));
            messageBox.setMaxWidth(680);
            messageBox.setFillWidth(true);

            HBox headerBox = new HBox(8);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label(title);
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            titleLabel.setStyle("-fx-text-fill: " + titleColor + ";");

            Region headerSpacer = new Region();
            HBox.setHgrow(headerSpacer, Priority.ALWAYS);

            Button copyButton = createCopyButton(content);

            messageBox.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 8;");
            headerBox.getChildren().addAll(titleLabel, headerSpacer, copyButton);
            messageBox.getChildren().addAll(headerBox, createMessageContentLabel(content));
            vboxChatHistory.getChildren().add(messageBox);
            scrollChat.setVvalue(1.0);
        });
    }

    /**
     * 创建消息内容展示控件
     * @param content 消息内容
     * @return 文本标签
     */
    private Label createMessageContentLabel(String content) {
        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        contentLabel.setFont(Font.font("System", 13));
        contentLabel.setStyle("-fx-text-fill: #111111;");
        return contentLabel;
    }

    /**
     * 创建复制消息按钮
     * @param content 消息内容
     * @return 复制按钮
     */
    private Button createCopyButton(String content) {
        Button copyButton = new Button("复制");
        copyButton.setFocusTraversable(false);
        copyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 4 0 4; -fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");
        copyButton.setOnMouseEntered(event -> copyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 4 0 4; -fx-font-size: 10px; -fx-text-fill: #4f5b66; -fx-cursor: hand;"));
        copyButton.setOnMouseExited(event -> copyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 4 0 4; -fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-cursor: hand;"));
        copyButton.setOnAction(event -> copyMessageContent(content, copyButton));
        return copyButton;
    }

    /**
     * 复制消息内容到剪贴板
     * @param content 消息内容
     * @param copyButton 复制按钮
     */
    private void copyMessageContent(String content, Button copyButton) {
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard.getSystemClipboard().setContent(clipboardContent);

        copyButton.setText("已复制");
        copyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 4 0 4; -fx-font-size: 10px; -fx-text-fill: #2980b9; -fx-cursor: hand;");

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(event -> {
            copyButton.setText("复制");
            copyButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 4 0 4; -fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");
        });
        pause.play();
    }

    /**
     * 更新默认文件名
     */
    private String updateDefaultFileName() {
        String sceneName = currentScene != null ? currentScene.getSceneName() : "AI";
        String subCategory = currentScene != null ? currentScene.getSubCategory() : "对话";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = sceneName + "_" + subCategory + "_" + timestamp;
        tfFileName.setText(fileName);
        return fileName;
    }

    /**
     * 构建请求消息列表，包括文件上下文和聊天历史
     * @return 请求消息列表
     */
    private List<QwenApiService.ChatMessage> buildRequestMessages() {
        List<QwenApiService.ChatMessage> requestMessages = new ArrayList<>();

        String fileContextMessage = buildFileContextMessage();
        if (!fileContextMessage.isBlank()) {
            requestMessages.add(new QwenApiService.ChatMessage("system", fileContextMessage));
        }

        if (chatHistory.size() <= MAX_RECENT_MESSAGES) {
            requestMessages.addAll(chatHistory);
            return requestMessages;
        }

        int historyCutIndex = chatHistory.size() - MAX_RECENT_MESSAGES;
        String historySummary = buildHistorySummary(chatHistory.subList(0, historyCutIndex));
        if (!historySummary.isBlank()) {
            requestMessages.add(new QwenApiService.ChatMessage("system", historySummary));
        }
        requestMessages.addAll(chatHistory.subList(historyCutIndex, chatHistory.size()));
        return requestMessages;
    }

    /**
     * 构建文件上下文消息
     * @return 文件上下文消息
     */
    private String buildFileContextMessage() {
        if (fileContent == null || fileContent.isBlank()) {
            return "";
        }

        String normalizedContent = normalizeWhitespace(fileContent);
        String compactContent = abbreviate(normalizedContent, MAX_FILE_CONTEXT_CHARS);
        String fileName = uploadedFile != null ? uploadedFile.getName() : "未命名文件";
        return """
                以下是用户上传的文件上下文，请仅在当前问题需要时引用。
                文件名：%s
                注意：如果文件内容不足以支持结论，请明确说明并请求用户补充更具体的片段。

                文件内容：
                %s
                """.formatted(fileName, compactContent).trim();
    }

    /**
     * 构建历史对话摘要
     * @param historyMessages 历史消息列表
     * @return 压缩后的历史摘要
     */
    private String buildHistorySummary(List<QwenApiService.ChatMessage> historyMessages) {
        StringBuilder builder = new StringBuilder("以下是较早对话的压缩摘要，请在保持上下文一致的前提下，优先参考后续保留的原始对话：\n");
        for (QwenApiService.ChatMessage message : historyMessages) {
            String roleName = switch (message.role()) {
                case "assistant" -> "助手";
                case "system" -> "系统";
                default -> "用户";
            };
            builder.append(roleName)
                    .append("：")
                    .append(abbreviate(normalizeWhitespace(message.content()), MAX_SINGLE_MESSAGE_CHARS))
                    .append("\n");

            if (builder.length() >= MAX_HISTORY_SUMMARY_CHARS) {
                builder.setLength(MAX_HISTORY_SUMMARY_CHARS);
                builder.append("...（更早内容已省略）");
                break;
            }
        }
        return builder.toString().trim();
    }

    /**
     * 规范化空白字符，去除多余换行和空格
     * @param content 原始内容
     * @return 规范化后的内容
     */
    private String normalizeWhitespace(String content) {
        return content.replace("\r", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    /**
     * 对内容进行折叠处理，保留开头和结尾部分，中间部分用提示替代
     * @param content 原始内容
     * @param maxLength 最大长度
     * @return 折叠后的内容
     */
    private String abbreviate(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }

        int headLength = Math.max(1, maxLength * 2 / 3);
        int tailLength = Math.max(1, maxLength - headLength - 24);
        return content.substring(0, headLength)
                + "\n\n...（中间内容已折叠）...\n\n"
                + content.substring(content.length() - tailLength);
    }

    /**
     * 显示警告对话框
     * @param title 标题
     * @param message 消息内容
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 执行待确认的Agent计划
     * @param event 点击事件
     */
    @FXML
    private void btnExecuteAgentPlanClick(ActionEvent event) {
        if (pendingAgentRun == null || pendingAgentRequest == null) {
            showAlert("提示", "当前没有待执行的 Agent 计划");
            return;
        }
        startAgentExecution(pendingAgentRun, pendingAgentRequest);
    }

    /**
     * 初始化Agent控件
     */
    private void initAgentControls() {
        cbAgentStrategy.getItems().setAll("保守", "标准", "深入");
        cbAgentStrategy.setValue("标准");
        spAgentMaxSteps.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 6, 4));
        chkConfirmBeforeRun.setSelected(true);
        chkUseFileContext.setSelected(true);
        chkUseChatHistory.setSelected(true);
        chkAgentMode.selectedProperty().addListener((obs, oldValue, newValue) -> updateAgentModeState());
        cbAgentType.setOnAction(e -> updateAgentTypeHint());
        lvAgentPlanSteps.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> updateAgentStepDetail(newValue == null ? -1 : newValue.intValue()));
        updateAgentModeState();
        clearPendingAgentPlan(true);
    }

    /**
     * 加载Agent定义
     */
    private void loadAgentDefinitions() {
        try {
            agentDefinitionMap.clear();
            List<AiAgentDefinition> definitions = agentDefinitionDao.findEnabledDefinitions();
            List<String> agentNames = new ArrayList<>();
            for (AiAgentDefinition definition : definitions) {
                agentDefinitionMap.put(definition.getAgentName(), definition);
                agentNames.add(definition.getAgentName());
            }
            cbAgentType.getItems().setAll(agentNames);
            if (!agentNames.isEmpty()) {
                cbAgentType.setValue(agentNames.getFirst());
            }
            updateAgentTypeHint();
        } catch (Exception e) {
            showAlert("错误", "加载 Agent 定义失败: " + e.getMessage());
        }
    }

    /**
     * 更新Agent模式显示状态
     */
    private void updateAgentModeState() {
        boolean agentMode = isAgentModeEnabled();
        setNodeVisible(vboxAgentConfig, agentMode);
        setNodeVisible(tpAgentPlan, agentMode && hasPlanContent());
        btnExecuteAgentPlan.setVisible(agentMode);
        btnExecuteAgentPlan.setManaged(agentMode);
        btnRegenerate.setDisable(agentMode);
    }

    /**
     * 处理Agent模式下的发送动作
     * @param userInput 用户输入
     */
    private void handleAgentSend(String userInput) {
        AiAgentDefinition selectedDefinition = getSelectedAgentDefinition();
        if (selectedDefinition == null) {
            showAlert("提示", "请先选择一个 Agent 类型");
            return;
        }

        AiAgentRoutingService.RoutingDecision routingDecision = agentRoutingService.evaluate(userInput, currentScene, selectedDefinition);
        AiAgentDefinition definition = selectedDefinition;
        if (!routingDecision.compatible()) {
            AiAgentDefinition recommendedDefinition = findRecommendedAgentDefinition(routingDecision.recommendedAgentNames());
            if (recommendedDefinition != null) {
                definition = recommendedDefinition;
                cbAgentType.setValue(recommendedDefinition.getAgentName());
                updateAgentTypeHint();
                routingDecision = agentRoutingService.evaluate(userInput, currentScene, recommendedDefinition);
                addAgentMessageToChat(
                        "🔀 Agent 自动切换",
                        "检测到当前任务更适合使用 “" + recommendedDefinition.getAgentName() + "”，系统已自动切换后继续执行。",
                        "#117864",
                        "#eafaf1");
                lblStatus.setText("已自动切换到更匹配的 Agent");
            }
        }

        if (!routingDecision.compatible()) {
            showAlert(
                    routingDecision.summary(),
                    routingDecision.detail()
                            + formatRoutingRecommendations(routingDecision.recommendedAgentNames()));
            lblStatus.setText("当前任务与所选 Agent 不匹配");
            if (isAgentModeEnabled()) {
                addAgentMessageToChat(
                        "⚠ Agent 使用建议",
                        routingDecision.detail() + formatRoutingRecommendations(routingDecision.recommendedAgentNames()),
                        "#b9770e",
                        "#fcf3cf");
            }
            return;
        }

        List<QwenApiService.ChatMessage> historySnapshot = new ArrayList<>(chatHistory);
        chatHistory.add(new QwenApiService.ChatMessage("user", userInput));
        addMessageToChat("user", userInput);
        taUserInput.clear();

        if (pendingAgentRun != null && currentTask == null) {
            agentService.markRunCancelled(pendingAgentRun);
        }
        clearPendingAgentPlan(true);

        AiAgentService.AgentExecutionRequest request = buildAgentExecutionRequest(definition, userInput, historySnapshot);
        startAgentPlanning(request);
    }

    /**
     * 构建Agent请求
     * @param definition Agent定义
     * @param userInput 用户输入
     * @param historySnapshot 历史快照
     * @return Agent请求
     */
    private AiAgentService.AgentExecutionRequest buildAgentExecutionRequest(
            AiAgentDefinition definition,
            String userInput,
            List<QwenApiService.ChatMessage> historySnapshot) {
        String historySummary = "";
        if (chkUseChatHistory.isSelected() && !historySnapshot.isEmpty()) {
            historySummary = buildHistorySummary(historySnapshot);
        }
        String fileContext = chkUseFileContext.isSelected() ? buildFileContextMessage() : "";
        return new AiAgentService.AgentExecutionRequest(
                definition,
                currentApiKey,
                cbModel.getValue(),
                userInput,
                currentScene != null ? currentScene.getSceneName() : null,
                currentScene != null ? currentScene.getSubCategory() : null,
                currentScene != null ? currentScene.getSystemPrompt() : null,
                fileContext,
                historySummary,
                chkConfirmBeforeRun.isSelected(),
                chkUseFileContext.isSelected(),
                chkUseChatHistory.isSelected(),
                cbAgentStrategy.getValue(),
                spAgentMaxSteps.getValue());
    }

    /**
     * 启动Agent规划阶段
     * @param request Agent请求
     */
    private void startAgentPlanning(AiAgentService.AgentExecutionRequest request) {
        requestCancelled = false;
        beginBusyState("Agent 正在生成执行计划...");

        Task<AiAgentService.PreparedAgentRun> task = new Task<>() {
            @Override
            protected AiAgentService.PreparedAgentRun call() throws Exception {
                return agentService.prepareRun(apiService, request, call -> currentCall = call, () -> requestCancelled || isCancelled());
            }
        };
        currentTask = task;

        task.setOnSucceeded(e -> {
            currentTask = null;
            currentCall = null;
            requestCancelled = false;

            AiAgentService.PreparedAgentRun preparedRun = task.getValue();
            pendingAgentRun = preparedRun;
            pendingAgentRequest = request;
            showAgentPlan(preparedRun, request.confirmBeforeRun(), preparedRun.completedSteps(), 0);
            addAgentMessageToChat("\uD83D\uDCA1 Agent 计划", formatAgentPlan(preparedRun.plan()), "#1f618d", "#ebf5fb");

            if (request.confirmBeforeRun()) {
                endBusyState();
                lblStatus.setText("计划已生成，等待确认执行");
                btnExecuteAgentPlan.setDisable(false);
                        refreshRecoverableRunHint();
            } else {
                        refreshRecoverableRunHint();
                startAgentExecution(preparedRun, request);
            }
        });

        task.setOnFailed(e -> {
            endBusyState();
            Throwable throwable = task.getException();
            if (requestCancelled || isCancelledException(throwable)) {
                lblStatus.setText("已取消");
            } else {
                showAlert("错误", "生成 Agent 计划失败: " + throwable.getMessage());
                lblStatus.setText("Agent 计划生成失败");
            }
            requestCancelled = false;
            refreshRecoverableRunHint();
        });

        task.setOnCancelled(e -> {
            endBusyState();
            lblStatus.setText("已取消");
            requestCancelled = false;
        });

        new Thread(task).start();
    }

    /**
     * 启动Agent执行阶段
     * @param preparedRun 已准备好的运行信息
     * @param request Agent请求
     */
    private void startAgentExecution(AiAgentService.PreparedAgentRun preparedRun, AiAgentService.AgentExecutionRequest request) {
        requestCancelled = false;
        beginBusyState("Agent 正在执行计划...");
        btnExecuteAgentPlan.setDisable(true);
        lblAgentPlanStatus.setText("执行中");

        Task<AiAgentService.AgentRunResult> task = new Task<>() {
            @Override
            protected AiAgentService.AgentRunResult call() throws Exception {
                return agentService.executePreparedRun(
                        apiService,
                        request,
                        preparedRun,
                        call -> currentCall = call,
                        () -> requestCancelled || isCancelled(),
                        new AiAgentService.AgentProgressListener() {
                            @Override
                            public void onStatusChanged(String status) {
                                Platform.runLater(() -> lblAgentPlanStatus.setText(status));
                            }

                            @Override
                            public void onStepStarted(AiAgentService.AgentPlanStep step) {
                                Platform.runLater(() -> {
                                    currentAgentStepNo = step.stepNo();
                                    renderAgentPlanSteps(preparedRun.plan(), currentAgentStepNo - 1, currentAgentStepNo);
                                    lblAgentPlanStatus.setText("执行中：" + step.stepName());
                                });
                            }

                            @Override
                            public void onStepCompleted(AiAgentService.AgentPlanStep step, AiAgentService.AgentStepResult result) {
                                Platform.runLater(() -> {
                                    currentAgentStepNo = step.stepNo();
                                    currentAgentStepResultMap.put(step.stepNo(), result);
                                    renderAgentPlanSteps(preparedRun.plan(), currentAgentStepNo, 0);
                                    addAgentMessageToChat(
                                            "🛠 " + step.stepName(),
                                            buildStepCompletionMessage(result),
                                            "#7d3c98",
                                            "#f5eef8");
                                });
                            }
                        });
            }
        };
        currentTask = task;

        task.setOnSucceeded(e -> {
            endBusyState();
            AiAgentService.AgentRunResult result = task.getValue();
            syncAgentStepResults(result.stepResults());
            currentAgentStepNo = result.plan().steps().size();
            renderAgentPlanSteps(result.plan(), currentAgentStepNo, 0);
            lblAgentPlanStatus.setText("已完成");
            if (request.agentDefinition().isFinalSummaryEnabled()) {
                addAgentMessageToChat("✅ Agent 结果", result.finalResponse(), "#1e8449", "#e8f8f5");
            }
            chatHistory.add(new QwenApiService.ChatMessage("assistant", result.finalResponse()));
            lblStatus.setText("Agent 执行完成");
            pendingAgentRun = null;
            pendingAgentRequest = null;
            refreshRecoverableRunHint();
        });

        task.setOnFailed(e -> {
            endBusyState();
            Throwable throwable = task.getException();
            if (requestCancelled || isCancelledException(throwable)) {
                lblAgentPlanStatus.setText("已取消");
                lblStatus.setText("已取消");
            } else {
                lblAgentPlanStatus.setText("执行失败");
                showAlert("错误", "Agent 执行失败: " + throwable.getMessage());
                lblStatus.setText("Agent 执行失败");
            }
            requestCancelled = false;
            refreshRecoverableRunHint();
        });

        task.setOnCancelled(e -> {
            endBusyState();
            lblAgentPlanStatus.setText("已取消");
            lblStatus.setText("已取消");
            requestCancelled = false;
            refreshRecoverableRunHint();
        });

        new Thread(task).start();
    }

    /**
     * 展示Agent计划
     * @param preparedRun 已准备好的运行信息
     * @param waitingConfirmation 是否等待确认执行
     * @param completedSteps 已完成的步骤数
     * @param runningStepNo 当前正在执行的步骤编号
     */
    private void showAgentPlan(AiAgentService.PreparedAgentRun preparedRun, boolean waitingConfirmation, int completedSteps, int runningStepNo) {
        setNodeVisible(tpAgentPlan, true);
        currentAgentPlan = preparedRun.plan();
        syncAgentStepResults(preparedRun.previousStepResults());
        lblAgentPlanGoal.setText(preparedRun.plan().goalSummary());
        lblAgentPlanStatus.setText(waitingConfirmation ? "等待确认" : "计划已生成");
        taAgentPlanSummary.setText(preparedRun.plan().planSummary());
        if (preparedRun.plan().riskNotes().isEmpty()) {
            taAgentRiskNotes.setText("未识别到明显风险");
        } else {
            taAgentRiskNotes.setText(String.join("\n", preparedRun.plan().riskNotes()));
        }
        renderAgentPlanSteps(preparedRun.plan(), completedSteps, runningStepNo);
        tpAgentPlan.setExpanded(true);
        btnExecuteAgentPlan.setDisable(!waitingConfirmation);
        if (!lvAgentPlanSteps.getItems().isEmpty()) {
            lvAgentPlanSteps.getSelectionModel().select(Math.max(0, Math.min(completedSteps, lvAgentPlanSteps.getItems().size() - 1)));
        } else {
            taAgentStepDetail.clear();
        }
        updateAgentModeState();
    }

    /**
     * 更新Agent类型说明
     */
    private void updateAgentTypeHint() {
        AiAgentDefinition definition = getSelectedAgentDefinition();
        if (definition == null) {
            lblAgentTypeHint.setText("请选择一个 Agent 类型。系统会基于任务意图自动推荐更匹配的 Agent。\n当前已支持：总结、翻译、数据分析、问题诊断、内容整理、代码审查。 ");
            return;
        }
        String finalSummaryHint = definition.isFinalSummaryEnabled()
                ? "该 Agent 会在步骤执行完成后追加一条最终汇总结果。"
                : "该 Agent 的最后一步结果即视为最终结果，不会额外生成 Agent 汇总。";
        String templateHint = definition.getTemplateCode() != null && !definition.getTemplateCode().isBlank()
            ? "固定模板：" + definition.getTemplateCode()
            : "固定模板：默认";
        lblAgentTypeHint.setText(definition.getDescription() + "\n" + templateHint + "\n" + finalSummaryHint + "\n如果用户任务与当前 Agent 不匹配，系统会优先自动切换到更合适的 Agent。 ");
    }

    /**
     * 格式化路由建议
     * @param recommendations 路由建议列表
     */
    private String formatRoutingRecommendations(List<String> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "";
        }
        return "\n\n建议：\n- " + String.join("\n- ", recommendations);
    }

    /**
     * 渲染Agent步骤
     * @param plan Agent计划
     * @param completedSteps 已完成的步骤数
     * @param runningStepNo 当前正在执行的步骤编号
     */
    private void renderAgentPlanSteps(AiAgentService.AgentPlan plan, int completedSteps, int runningStepNo) {
        List<String> stepItems = new ArrayList<>();
        for (AiAgentService.AgentPlanStep step : plan.steps()) {
            String prefix;
            if (runningStepNo == step.stepNo()) {
                prefix = "[执行中] ";
            } else if (completedSteps >= step.stepNo()) {
                prefix = "[已完成] ";
            } else {
                prefix = "[待执行] ";
            }
            String skillSuffix = "";
            if (step.boundSkills() != null && !step.boundSkills().isEmpty()) {
                List<String> skillNames = new ArrayList<>();
                for (AiAgentService.AgentBoundSkill skill : step.boundSkills()) {
                    skillNames.add(skill.skillName());
                }
                skillSuffix = " / Skill: " + String.join(" -> ", skillNames);
            }
            AiAgentService.AgentStepResult stepResult = currentAgentStepResultMap.get(step.stepNo());
            if (stepResult != null && stepResult.skillSummaries() != null && !stepResult.skillSummaries().isEmpty()) {
                skillSuffix += " / 已生成 " + stepResult.skillSummaries().size() + " 条 Skill 摘要";
            }
            stepItems.add(prefix + step.stepNo() + ". " + step.stepName() + " - " + step.expectedOutput() + skillSuffix);
        }
        lvAgentPlanSteps.getItems().setAll(stepItems);
        updateAgentStepDetail(lvAgentPlanSteps.getSelectionModel().getSelectedIndex());
    }

    /**
     * 格式化计划文本
     * @param plan Agent计划
     * @return 格式化后的计划文本
     */
    private String formatAgentPlan(AiAgentService.AgentPlan plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("目标：").append(plan.goalSummary()).append("\n\n");
        builder.append("计划摘要：\n").append(plan.planSummary()).append("\n\n");
        builder.append("执行步骤：\n");
        for (AiAgentService.AgentPlanStep step : plan.steps()) {
            builder.append(step.stepNo())
                    .append(". ")
                    .append(step.stepName())
                    .append(" - ")
                    .append(step.purpose());
            if (step.boundSkills() != null && !step.boundSkills().isEmpty()) {
                List<String> skillNames = new ArrayList<>();
                for (AiAgentService.AgentBoundSkill skill : step.boundSkills()) {
                    skillNames.add(skill.skillName());
                }
                builder.append("（Skill: ").append(String.join(" -> ", skillNames)).append("）");
            }
            builder
                    .append("\n");
        }
        if (!plan.riskNotes().isEmpty()) {
            builder.append("\n风险提示：\n");
            for (String risk : plan.riskNotes()) {
                builder.append("- ").append(risk).append("\n");
            }
        }
        return builder.toString().trim();
    }

    /**
     * 设置忙碌状态
     * @param status 忙碌状态文本
     */
    private void beginBusyState(String status) {
        btnSend.setDisable(true);
        btnCancel.setDisable(false);
        btnExecuteAgentPlan.setDisable(true);
        lblStatus.setText(status);
    }

    /**
     * 结束忙碌状态
     */
    private void endBusyState() {
        btnSend.setDisable(false);
        btnCancel.setDisable(true);
        currentTask = null;
        currentCall = null;
        requestCancelled = false;
    }

    /**
     * 清理待执行计划
     * @param hidePanel 是否隐藏计划面板
     */
    private void clearPendingAgentPlan(boolean hidePanel) {
        pendingAgentRun = null;
        pendingAgentRequest = null;
        currentAgentPlan = null;
        currentAgentStepResultMap.clear();
        currentAgentStepNo = 0;
        btnExecuteAgentPlan.setDisable(true);
        lblAgentPlanGoal.setText("未生成计划");
        lblAgentPlanStatus.setText("空闲");
        taAgentPlanSummary.clear();
        taAgentStepDetail.clear();
        taAgentRiskNotes.clear();
        lvAgentPlanSteps.getItems().clear();
        if (hidePanel) {
            setNodeVisible(tpAgentPlan, false);
        }
        updateAgentModeState();
    }

    /**
     * 获取选中的Agent定义
     */
    private AiAgentDefinition getSelectedAgentDefinition() {
        String agentName = cbAgentType.getValue();
        return agentName != null ? agentDefinitionMap.get(agentName) : null;
    }

    /**
     * 根据推荐名称查找可用Agent
     * @param recommendedAgentNames 推荐的Agent名称列表
     */
    private AiAgentDefinition findRecommendedAgentDefinition(List<String> recommendedAgentNames) {
        if (recommendedAgentNames == null || recommendedAgentNames.isEmpty()) {
            return null;
        }
        for (String recommendedName : recommendedAgentNames) {
            AiAgentDefinition definition = agentDefinitionMap.get(recommendedName);
            if (definition != null) {
                return definition;
            }
        }
        return null;
    }

    /**
     * 是否启用Agent模式
     */
    private boolean isAgentModeEnabled() {
        return chkAgentMode != null && chkAgentMode.isSelected();
    }

    /**
     * 节点显隐联动
     * @param node 节点
     * @param visible 是否可见
     */
    private void setNodeVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /**
     * 是否存在计划内容
     */
    private boolean hasPlanContent() {
        return pendingAgentRun != null
                || !lvAgentPlanSteps.getItems().isEmpty()
                || !taAgentPlanSummary.getText().isBlank();
    }

    /**
     * 打开自定义场景编辑对话框
     * @param title 对话框标题
     * @param initialScene 初始数据
     */
    private void openSceneEditorDialog(String title, AiSceneCategory initialScene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AI_SCENE_EDITOR_DIALOG));
            Parent dialogRoot = loader.load();
            AiSceneEditorDialogController controller = loader.getController();
            controller.setDialogData(title, initialScene);

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.initOwner((Stage) cbScene.getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

            AiSceneCategory resultScene = controller.getResultScene();
            if (resultScene == null) {
                return;
            }
            if (controller.isTrialConfirmed()) {
                applyDraftScene(resultScene);
                lblStatus.setText("已应用仅本次使用的自定义场景");
                return;
            }
            if (controller.isSaveConfirmed()) {
                persistCustomScene(resultScene);
            }
        } catch (IOException e) {
            showAlert("错误", "打开场景编辑对话框失败: " + e.getMessage());
        } catch (Exception e) {
            showAlert("错误", "处理自定义场景失败: " + e.getMessage());
        }
    }

    /**
     * 应用仅本次会话使用的场景
     * @param scene 场景实体
     */
    private void applyDraftScene(AiSceneCategory scene) {
        draftScene = copyScene(scene, false);
        draftScene.setCustom(true);

        String sceneName = draftScene.getSceneName();
        if (!cbScene.getItems().contains(sceneName)) {
            cbScene.getItems().add(sceneName);
        }

        try {
            currentCategories = new ArrayList<>(sceneDao.findBySceneName(sceneName));
        } catch (Exception e) {
            currentCategories = new ArrayList<>();
        }

        mergeSceneIntoCurrentCategories(draftScene);
        sceneSelectionUpdating = true;
        try {
            cbScene.setValue(sceneName);
        } finally {
            sceneSelectionUpdating = false;
        }
        refreshSubCategoryItems(draftScene.getSubCategory());
        syncCurrentScene();
        updateDefaultFileName();
        updateSceneButtonState();
    }

    /**
     * 保存自定义场景
     * @param scene 场景实体
     * @throws Exception 保存异常
     */
    private void persistCustomScene(AiSceneCategory scene) throws Exception {
        scene.setCustom(true);
        scene.setSceneName(scene.getSceneName().trim());
        scene.setSubCategory(scene.getSubCategory().trim());
        scene.setSystemPrompt(scene.getSystemPrompt().trim());
        scene.setTags(scene.getTags() != null ? scene.getTags().trim() : null);
        scene.setRemark(scene.getRemark() != null ? scene.getRemark().trim() : null);

        if (sceneDao.existsBySceneAndSubCategory(scene.getSceneName(), scene.getSubCategory(), scene.getId())) {
            throw new IllegalArgumentException("场景分类和子分类组合已存在，请修改后重试");
        }

        if (scene.getSortOrder() == null || scene.getSortOrder() <= 0) {
            scene.setSortOrder(sceneDao.findNextSortOrder(scene.getSceneName()));
        }

        if (scene.getId() == null) {
            sceneDao.insert(scene);
        } else {
            sceneDao.update(scene);
        }

        draftScene = null;
        loadSceneCategories();
        selectScene(scene.getSceneName(), scene.getSubCategory());
        lblStatus.setText("自定义场景已保存");
    }

    /**
     * 在当前列表中合并场景
     * @param scene 场景实体
     */
    private void mergeSceneIntoCurrentCategories(AiSceneCategory scene) {
        for (int i = 0; i < currentCategories.size(); i++) {
            AiSceneCategory category = currentCategories.get(i);
            if (category.getSubCategory().equals(scene.getSubCategory())) {
                currentCategories.set(i, scene);
                return;
            }
        }
        currentCategories.add(scene);
    }

    /**
     * 刷新子分类下拉框
     * @param selectedSubCategory 选中的子分类
     */
    private void refreshSubCategoryItems(String selectedSubCategory) {
        List<String> subCategories = new ArrayList<>();
        for (AiSceneCategory category : currentCategories) {
            if (!subCategories.contains(category.getSubCategory())) {
                subCategories.add(category.getSubCategory());
            }
        }
        cbSubCategory.getItems().setAll(subCategories);
        cbSubCategory.setValue(selectedSubCategory);
    }

    /**
     * 选择指定场景
     * @param sceneName 场景分类
     * @param subCategory 子分类
     */
    private void selectScene(String sceneName, String subCategory) {
        sceneSelectionUpdating = true;
        try {
            cbScene.setValue(sceneName);
        } finally {
            sceneSelectionUpdating = false;
        }
        loadSubCategories(sceneName);
        if (cbSubCategory.getItems().contains(subCategory)) {
            cbSubCategory.setValue(subCategory);
            syncCurrentScene();
        }
    }

    /**
     * 更新场景操作按钮状态
     */
    private void updateSceneButtonState() {
        boolean hasScene = currentScene != null;
        boolean customScene = hasScene && currentScene.isCustom();
        boolean savedCustomScene = customScene && currentScene.getId() != null;

        btnSceneEdit.setDisable(!customScene);
        btnSceneCopy.setDisable(!hasScene);
        btnSceneDelete.setDisable(!savedCustomScene);
        btnSceneSave.setDisable(!customScene);
    }

    /**
     * 复制场景对象
     * @param source 原场景
     * @param resetIdentity 是否重置主键
     * @return 新对象
     */
    private AiSceneCategory copyScene(AiSceneCategory source, boolean resetIdentity) {
        AiSceneCategory copied = new AiSceneCategory();
        copied.setId(resetIdentity ? null : source.getId());
        copied.setSceneName(source.getSceneName());
        copied.setSubCategory(resetIdentity ? source.getSubCategory() + "_副本" : source.getSubCategory());
        copied.setSystemPrompt(source.getSystemPrompt());
        copied.setSortOrder(source.getSortOrder());
        copied.setCustom(true);
        copied.setTags(source.getTags());
        copied.setRemark(source.getRemark());
        copied.setCreateTime(source.getCreateTime());
        copied.setUpdateTime(source.getUpdateTime());
        return copied;
    }

    /**
     * 打开Agent管理对话框
     */
    @FXML
    private void btnManageAgentClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AI_AGENT_MANAGER_DIALOG));
            Parent dialogRoot = loader.load();
            AiAgentManagerDialogController controller = loader.getController();
            controller.initData();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("管理 Agent");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.initOwner((Stage) cbScene.getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

            if (controller.isSaved()) {
                loadAgentDefinitions();
            }
        } catch (Exception e) {
            showAlert("错误", "打开 Agent 管理失败: " + e.getMessage());
        }
    }

    /**
     * 打开Skill管理对话框
     */
    @FXML
    private void btnManageSkillClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AI_SKILL_MANAGER_DIALOG));
            Parent dialogRoot = loader.load();
            AiSkillManagerDialogController controller = loader.getController();
            controller.initData();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("管理 Skill");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.initOwner((Stage) cbScene.getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();
        } catch (Exception e) {
            showAlert("错误", "打开 Skill 管理失败: " + e.getMessage());
        }
    }

    /**
     * 打开恢复执行对话框
     */
    @FXML
    private void btnResumeAgentClick() {
        if (currentTask != null) {
            showAlert("提示", "当前已有 Agent 任务在执行，请先等待完成或取消后再恢复");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(AI_AGENT_RESUME_DIALOG));
            Parent dialogRoot = loader.load();
            AiAgentResumeDialogController controller = loader.getController();
            controller.initData(agentDefinitionDao.findAllActiveDefinitions(), QwenApiService.getAvailableModels(), currentApiKey);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("恢复 Agent 执行");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.initOwner((Stage) cbScene.getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

            AiAgentResumeDialogController.ResumeSelection selection = controller.getResult();
            if (selection == null) {
                return;
            }

            if (pendingAgentRun != null && pendingAgentRequest != null) {
                agentService.markRunCancelled(pendingAgentRun);
            }
            clearPendingAgentPlan(true);

            AiAgentRunRecord sourceRun = selection.sourceRun();
            AiAgentService.AgentExecutionRequest request = selection.request();
            if ((request.apiKey() == null || request.apiKey().isBlank()) && currentApiKey != null) {
                request = new AiAgentService.AgentExecutionRequest(
                        request.agentDefinition(),
                        currentApiKey,
                        request.model(),
                        request.userInput(),
                        request.sceneName(),
                        request.subCategory(),
                        request.scenePrompt(),
                        request.fileContext(),
                        request.historySummary(),
                        request.confirmBeforeRun(),
                        request.useFileContext(),
                        request.useChatHistory(),
                        request.strategy(),
                        request.maxSteps());
            }

            AiAgentService.PreparedAgentRun preparedRun = agentService.restorePreparedRun(sourceRun, request, selection.deriveNewRun());
            pendingAgentRun = preparedRun;
            pendingAgentRequest = request;
            showAgentPlan(preparedRun, request.confirmBeforeRun(), preparedRun.completedSteps(), 0);
            addAgentMessageToChat(
                    "♻ 恢复 Agent",
                    "已加载可恢复记录：" + sourceRun.getUserGoal() + "\n当前将从第 " + (preparedRun.completedSteps() + 1) + " 步继续。",
                    "#0e6655",
                    "#e8f8f5");

            if (request.confirmBeforeRun()) {
                lblStatus.setText(selection.deriveNewRun() ? "已生成派生恢复计划，等待确认执行" : "已恢复原运行，等待确认执行");
                btnExecuteAgentPlan.setDisable(false);
            } else {
                startAgentExecution(preparedRun, request);
            }
            refreshRecoverableRunHint();
        } catch (Exception e) {
            showAlert("错误", "恢复 Agent 执行失败: " + e.getMessage());
        }
    }

    /**
     * 刷新恢复提示
     */
    private void refreshRecoverableRunHint() {
        if (lblRecoverableRuns == null || btnResumeAgent == null) {
            return;
        }
        try {
            int count = agentRunLogDao.countRecoverableRuns();
            if (count > 0) {
                lblRecoverableRuns.setText("有 " + count + " 条未完成运行可恢复");
                btnResumeAgent.setDisable(false);
            } else {
                lblRecoverableRuns.setText("没有可恢复的未完成运行");
                btnResumeAgent.setDisable(true);
            }
        } catch (Exception e) {
            lblRecoverableRuns.setText("读取恢复记录失败");
            btnResumeAgent.setDisable(false);
        }
    }

    /**
     * 同步步骤结果映射
     */
    private void syncAgentStepResults(List<AiAgentService.AgentStepResult> stepResults) {
        currentAgentStepResultMap.clear();
        if (stepResults == null) {
            return;
        }
        for (AiAgentService.AgentStepResult stepResult : stepResults) {
            currentAgentStepResultMap.put(stepResult.stepNo(), stepResult);
        }
    }

    /**
     * 更新步骤详情区
     */
    private void updateAgentStepDetail(int selectedIndex) {
        if (taAgentStepDetail == null) {
            return;
        }
        if (currentAgentPlan == null) {
            taAgentStepDetail.clear();
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= currentAgentPlan.steps().size()) {
            taAgentStepDetail.setText("请选择一个步骤以查看详细信息");
            return;
        }
        AiAgentService.AgentPlanStep step = currentAgentPlan.steps().get(selectedIndex);
        taAgentStepDetail.setText(formatAgentStepDetail(step));
    }

    /**
     * 格式化步骤详情
     */
    private String formatAgentStepDetail(AiAgentService.AgentPlanStep step) {
        StringBuilder builder = new StringBuilder();
        builder.append("步骤 ").append(step.stepNo()).append("：").append(step.stepName()).append("\n");
        builder.append("步骤目标：").append(step.purpose()).append("\n");
        builder.append("期望输出：").append(step.expectedOutput()).append("\n\n");
        if (step.boundSkills() == null || step.boundSkills().isEmpty()) {
            builder.append("绑定 Skill：无，当前步骤由 Agent 执行器直接完成。\n\n");
        } else {
            builder.append("绑定 Skill：\n");
            for (int i = 0; i < step.boundSkills().size(); i++) {
                AiAgentService.AgentBoundSkill skill = step.boundSkills().get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(skill.skillName())
                        .append(" [")
                        .append(skill.skillCode())
                        .append("]\n");
            }
            builder.append("\n");
        }

        AiAgentService.AgentStepResult stepResult = currentAgentStepResultMap.get(step.stepNo());
        if (stepResult == null) {
            builder.append("执行摘要：当前步骤尚未执行。\n");
            return builder.toString().trim();
        }
        if (stepResult.skillSummaries() != null && !stepResult.skillSummaries().isEmpty()) {
            builder.append("Skill 执行摘要：\n");
            for (AiAgentService.SkillExecutionSummary summary : stepResult.skillSummaries()) {
                builder.append("- ")
                        .append(summary.skillName())
                        .append("：\n")
                        .append(abbreviate(summary.output(), 220))
                        .append("\n");
            }
            builder.append("\n");
        }
        builder.append("步骤最终输出：\n").append(abbreviate(stepResult.output(), 500));
        return builder.toString().trim();
    }

    /**
     * 构建步骤完成消息
     */
    private String buildStepCompletionMessage(AiAgentService.AgentStepResult result) {
        StringBuilder builder = new StringBuilder();
        if (result.skillSummaries() != null && !result.skillSummaries().isEmpty()) {
            builder.append("Skill 摘要：\n");
            for (AiAgentService.SkillExecutionSummary summary : result.skillSummaries()) {
                builder.append("- ")
                        .append(summary.skillName())
                        .append("：")
                        .append(abbreviate(summary.output(), 140))
                        .append("\n");
            }
            builder.append("\n");
        }
        builder.append("步骤输出：\n").append(abbreviate(result.output(), 500));
        return builder.toString().trim();
    }
}