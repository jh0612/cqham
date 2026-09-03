package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.AiAgentDefinitionDao;
import com.reiwaxr.cq.cqham.dao.AiAgentSkillBindingDao;
import com.reiwaxr.cq.cqham.dao.AiAgentTemplateDao;
import com.reiwaxr.cq.cqham.dao.AiSkillDefinitionDao;
import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.entity.AiAgentSkillBinding;
import com.reiwaxr.cq.cqham.entity.AiAgentTemplate;
import com.reiwaxr.cq.cqham.entity.AiAgentTemplateStep;
import com.reiwaxr.cq.cqham.entity.AiSkillDefinition;
import com.reiwaxr.cq.cqham.utils.AiAgentTemplatePreset;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Agent管理对话框控制器
 */
public class AiAgentManagerDialogController {

    /* Agent 列表 */
    @FXML private ListView<AiAgentDefinition> lvAgents;
    /* 编辑模式标签 */
    @FXML private Label lblEditMode;
    /* Agent 名称 */
    @FXML private TextField tfAgentName;
    /* Agent 模板 */
    @FXML private ComboBox<String> cbBaseTemplate;
    /* 基础模板选择框 */
    @FXML private CheckBox chkAgentEnabled;
    /* 是否启用 Agent */
    @FXML private CheckBox chkFinalSummaryEnabled;
    /* Agent 描述 */
    @FXML private TextArea taAgentDescription;
    /* 规划提示 */
    @FXML private TextArea taPlannerPrompt;
    /* 执行提示 */
    @FXML private TextArea taExecutionPrompt;
    /* 模板步骤列表 */
    @FXML private ListView<TemplateStepViewItem> lvTemplateSteps;
    /* 模板步骤名称 */
    @FXML private TextField tfTemplateStepName;
    /* 模板步骤目标 */
    @FXML private TextArea taTemplateStepPurpose;
    /* 模板步骤预期输出 */
    @FXML private TextArea taTemplateStepExpectedOutput;
    /* 应用步骤修改按钮 */
    @FXML private Button btnApplyTemplateStep;
    /* 新增模板步骤 */
    @FXML private Button btnTemplateStepAdd;
    /* 删除模板步骤 */
    @FXML private Button btnTemplateStepDelete;
    /* 模板步骤上移 */
    @FXML private Button btnTemplateStepMoveUp;
    /* 模板步骤下移 */
    @FXML private Button btnTemplateStepMoveDown;
    /* 模板另存为按钮 */
    @FXML private Button btnTemplateSaveAs;
    /* 绑定步骤选择框 */
    @FXML private ComboBox<String> cbBindingStep;
    /* 绑定步骤选择框 */
    @FXML private ComboBox<AiSkillDefinition> cbBindingSkill;
    /* 绑定列表 */
    @FXML private ListView<BindingViewItem> lvBindings;
    /* 复制按钮 */
    @FXML private Button btnCopyAgent;
    /* 删除按钮 */
    @FXML private Button btnDeleteAgent;
    /* 保存按钮 */
    @FXML private Button btnSaveAgent;
    /* Agent 定义 DAO */
    private final AiAgentDefinitionDao agentDefinitionDao = new AiAgentDefinitionDao();
    /* 技能定义 DAO */
    private final AiSkillDefinitionDao skillDefinitionDao = new AiSkillDefinitionDao();
    /* Agent 与技能绑定 DAO */
    private final AiAgentSkillBindingDao bindingDao = new AiAgentSkillBindingDao();
    /* 模板 DAO */
    private final AiAgentTemplateDao templateDao = new AiAgentTemplateDao();
    /* 所有 Agent 列表 */
    private final List<AiAgentDefinition> allAgents = new ArrayList<>();
    /* 所有模板列表 */
    private final List<AiAgentTemplate> allTemplates = new ArrayList<>();
    /* 所有技能列表 */
    private final List<AiSkillDefinition> allSkills = new ArrayList<>();
    /* 当前模板步骤列表 */
    private final List<AiAgentTemplateStep> currentTemplateSteps = new ArrayList<>();
    /* 当前 Agent 的技能绑定列表 */
    private final List<AiAgentSkillBinding> currentBindings = new ArrayList<>();
    /* 模板选择映射 */
    private final Map<String, String> templateChoiceMap = new LinkedHashMap<>();
    /* 当前模板代码 */
    private String currentTemplateCode;
    /* 当前模板是否内置 */
    private boolean currentTemplateBuiltIn;
    /* 当前选中的 Agent */
    private AiAgentDefinition selectedAgent;
    /* 是否已保存 */
    private boolean saved;

    /**
     * 初始化
     */
    @FXML
    public void initialize() {
        lvAgents.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiAgentDefinition item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String prefix = item.isBuiltIn() ? "[内置] " : "[自定义] ";
                String suffix = item.isEnabled() ? "" : " (停用)";
                setText(prefix + item.getAgentName() + suffix);
            }
        });
        lvBindings.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(BindingViewItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toDisplayText());
            }
        });
        lvTemplateSteps.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TemplateStepViewItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toDisplayText());
            }
        });
        cbBindingSkill.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiSkillDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSkillName());
            }
        });
        cbBindingSkill.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AiSkillDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getSkillName());
            }
        });
        cbBaseTemplate.valueProperty().addListener((obs, oldValue, newValue) -> {
            String templateCode = templateChoiceMap.get(newValue);
            if (templateCode != null) {
                try {
                    loadTemplateSteps(templateCode);
                    refreshBindingStepOptions(templateCode);
                } catch (Exception e) {
                    showAlert("错误", "加载模板步骤失败: " + e.getMessage());
                }
            }
        });
        lvTemplateSteps.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            showTemplateStepDetail(newValue == null ? null : newValue.step());
            updateEditState();
        });
        lvAgents.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadAgentDetail(newValue));
    }

    /**
     * 初始化数据
     */
    public void initData() {
        try {
            loadSkills();
            loadTemplates();
            loadAgents(null);
        } catch (Exception e) {
            showAlert("错误", "加载 Agent 管理数据失败: " + e.getMessage());
        }
    }

    /**
     * 新建Agent
     */
    @FXML
    private void btnNewAgentClick() {
        AiAgentDefinition draft = new AiAgentDefinition();
        draft.setAgentCode("custom_agent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        draft.setAgentName("");
        draft.setDescription("");
        draft.setPlannerPrompt("");
        draft.setExecutionPrompt("");
        draft.setTemplateCode(defaultTemplateCode());
        draft.setFinalSummaryEnabled(true);
        draft.setEnabled(true);
        draft.setBuiltIn(false);
        draft.setDeleted(false);
        currentBindings.clear();
        selectedAgent = draft;
        fillForm(draft);
    }

    /**
     * 复制Agent
     */
    @FXML
    private void btnCopyAgentClick() {
        if (selectedAgent == null) {
            showAlert("提示", "请先选择一个 Agent");
            return;
        }
        AiAgentDefinition copied = new AiAgentDefinition();
        copied.setAgentCode("custom_agent_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        copied.setAgentName(selectedAgent.getAgentName() + " 副本");
        copied.setDescription(selectedAgent.getDescription());
        copied.setPlannerPrompt(selectedAgent.getPlannerPrompt());
        copied.setExecutionPrompt(selectedAgent.getExecutionPrompt());
        copied.setTemplateCode(resolveTemplateCode(selectedAgent));
        copied.setFinalSummaryEnabled(selectedAgent.isFinalSummaryEnabled());
        copied.setEnabled(selectedAgent.isEnabled());
        copied.setBuiltIn(false);
        copied.setDeleted(false);
        copied.setSortOrder(selectedAgent.getSortOrder());

        List<AiAgentSkillBinding> copiedBindings = new ArrayList<>();
        for (AiAgentSkillBinding binding : currentBindings) {
            AiAgentSkillBinding copiedBinding = new AiAgentSkillBinding();
            copiedBinding.setTemplateStepNo(binding.getTemplateStepNo());
            copiedBinding.setBindingOrder(binding.getBindingOrder());
            copiedBinding.setSkillId(binding.getSkillId());
            copiedBinding.setSkillCode(binding.getSkillCode());
            copiedBinding.setSkillName(binding.getSkillName());
            copiedBinding.setEnabled(true);
            copiedBindings.add(copiedBinding);
        }
        currentBindings.clear();
        currentBindings.addAll(copiedBindings);
        selectedAgent = copied;
        fillForm(copied);
    }

    /**
     * 删除Agent
     */
    @FXML
    private void btnDeleteAgentClick() {
        if (selectedAgent == null || selectedAgent.getId() == null) {
            return;
        }
        if (selectedAgent.isBuiltIn()) {
            showAlert("提示", "内置 Agent 不允许直接删除，请先复制为自定义 Agent");
            return;
        }
        Optional<ButtonType> result = showConfirm("确认删除", "确定要逻辑删除当前自定义 Agent 吗？");
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            agentDefinitionDao.logicalDelete(selectedAgent.getId());
            bindingDao.deleteBindingsByAgentId(selectedAgent.getId());
            saved = true;
            loadAgents(null);
        } catch (Exception e) {
            showAlert("错误", "删除 Agent 失败: " + e.getMessage());
        }
    }

    /**
     * 刷新列表
     */
    @FXML
    private void btnRefreshClick() {
        try {
            loadSkills();
            loadTemplates();
            loadAgents(selectedAgent != null ? selectedAgent.getId() : null);
        } catch (Exception e) {
            showAlert("错误", "刷新 Agent 数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存Agent
     */
    @FXML
    private void btnSaveAgentClick() {
        if (selectedAgent == null) {
            showAlert("提示", "请先新建或选择一个 Agent");
            return;
        }
        if (selectedAgent.isBuiltIn() && selectedAgent.getId() != null) {
            showAlert("提示", "内置 Agent 不允许直接编辑，请使用复制功能");
            return;
        }
        try {
            AiAgentDefinition formAgent = collectFormData();
            if (isTemplateEditable()) {
                applyTemplateStepEditor(true);
                persistTemplateSteps(formAgent.getTemplateCode());
            }
            if (formAgent.getId() == null) {
                formAgent.setSortOrder(agentDefinitionDao.findNextSortOrder());
                int newId = agentDefinitionDao.insert(formAgent);
                formAgent.setId(newId);
            } else {
                agentDefinitionDao.update(formAgent);
            }

            normalizeBindingOrders();
            for (AiAgentSkillBinding binding : currentBindings) {
                binding.setAgentId(formAgent.getId());
            }
            bindingDao.replaceBindings(formAgent.getId(), currentBindings);
            saved = true;
            selectedAgent = formAgent;
            loadAgents(formAgent.getId());
        } catch (Exception e) {
            showAlert("错误", "保存 Agent 失败: " + e.getMessage());
        }
    }

    /**
     * 应用当前模板步骤文案修改
     */
    @FXML
    private void btnApplyTemplateStepClick() {
        if (!isTemplateEditable()) {
            showAlert("提示", "内置模板不支持直接修改，请先使用“另存为模板”");
            return;
        }
        try {
            applyTemplateStepEditor(true);
            Integer selectedStepNo = lvTemplateSteps.getSelectionModel().getSelectedItem() != null
                    ? lvTemplateSteps.getSelectionModel().getSelectedItem().step().getStepNo()
                    : null;
            refreshTemplateStepView(selectedStepNo);
            refreshBindingStepOptions(currentTemplateCode);
        } catch (Exception e) {
            showAlert("错误", "应用步骤文案失败: " + e.getMessage());
        }
    }

    /**
     * 新增模板步骤
     */
    @FXML
    private void btnTemplateStepAddClick() {
        if (!isTemplateEditable()) {
            showAlert("提示", "内置模板不支持直接修改，请先使用“另存为模板”");
            return;
        }

        int nextNo = currentTemplateSteps.size() + 1;
        AiAgentTemplateStep step = new AiAgentTemplateStep();
        step.setTemplateCode(currentTemplateCode);
        step.setStepNo(nextNo);
        step.setSortOrder(nextNo);
        step.setSkillCode("custom_step_" + nextNo);
        step.setStepName("新步骤 " + nextNo);
        step.setPurpose("");
        step.setExpectedOutput("");
        step.setEnabled(true);
        currentTemplateSteps.add(step);
        resequenceTemplateStepsAndBindings();
        refreshTemplateStepView(nextNo);
        refreshBindingStepOptions(currentTemplateCode);
    }

    /**
     * 删除模板步骤
     */
    @FXML
    private void btnTemplateStepDeleteClick() {
        if (!isTemplateEditable()) {
            showAlert("提示", "内置模板不支持直接修改，请先使用“另存为模板”");
            return;
        }
        TemplateStepViewItem selected = lvTemplateSteps.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (currentTemplateSteps.size() <= 1) {
            showAlert("提示", "至少保留一个步骤");
            return;
        }

        int removedStepNo = selected.step().getStepNo();
        currentTemplateSteps.remove(selected.step());
        resequenceTemplateStepsAndBindings();
        Integer preferredStepNo = Math.max(1, removedStepNo - 1);
        refreshTemplateStepView(preferredStepNo);
        refreshBindingStepOptions(currentTemplateCode);
    }

    /**
     * 模板步骤上移
     */
    @FXML
    private void btnTemplateStepMoveUpClick() {
        moveTemplateStep(-1);
    }

    /**
     * 模板步骤下移
     */
    @FXML
    private void btnTemplateStepMoveDownClick() {
        moveTemplateStep(1);
    }

    /**
     * 内置模板另存为自定义模板
     */
    @FXML
    private void btnTemplateSaveAsClick() {
        if (currentTemplateCode == null || currentTemplateCode.isBlank()) {
            showAlert("提示", "请先选择模板");
            return;
        }

        try {
            String sourceCode = currentTemplateCode;
            AiAgentTemplate sourceTemplate = resolveTemplateByCode(sourceCode);
            String defaultName = sourceTemplate != null
                    ? normalize(sourceTemplate.getTemplateName()) + " 副本"
                    : sourceCode + " 副本";
            String defaultCode = buildDefaultCustomTemplateCode(sourceCode);

            String templateName = requestInput("另存为模板", "请输入新模板名称", defaultName);
            if (templateName == null) {
                return;
            }
            templateName = normalize(templateName);
            if (templateName.isBlank()) {
                throw new IllegalArgumentException("模板名称不能为空");
            }

            String templateCode = requestInput("另存为模板", "请输入新模板代码（建议小写字母+下划线）", defaultCode);
            if (templateCode == null) {
                return;
            }
            templateCode = normalize(templateCode).toLowerCase();
            if (!templateCode.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("模板代码只能包含小写字母、数字和下划线");
            }

            if (templateDao.findByTemplateCode(templateCode) != null) {
                throw new IllegalArgumentException("模板代码已存在，请更换后重试");
            }

            AiAgentTemplate newTemplate = new AiAgentTemplate();
            newTemplate.setTemplateCode(templateCode);
            newTemplate.setTemplateName(templateName);
            newTemplate.setDescription(sourceTemplate != null ? sourceTemplate.getDescription() : "");
            newTemplate.setSortOrder(templateDao.findNextSortOrder());
            newTemplate.setEnabled(true);
            newTemplate.setBuiltIn(false);
            templateDao.insertTemplate(newTemplate);

            List<AiAgentTemplateStep> copiedSteps = new ArrayList<>();
            for (AiAgentTemplateStep step : currentTemplateSteps) {
                AiAgentTemplateStep copiedStep = copyTemplateStep(step);
                copiedStep.setTemplateCode(templateCode);
                copiedSteps.add(copiedStep);
            }
            normalizeTemplateSteps(copiedSteps);
            templateDao.replaceTemplateSteps(templateCode, copiedSteps);

            loadTemplates();
            selectTemplateChoice(templateCode);
            if (selectedAgent != null) {
                selectedAgent.setTemplateCode(templateCode);
            }
            loadTemplateSteps(templateCode);
            refreshBindingStepOptions(templateCode);
            updateEditState();
            showAlert("提示", "已另存为自定义模板，可继续编辑步骤文案");
        } catch (Exception e) {
            showAlert("错误", "另存为模板失败: " + e.getMessage());
        }
    }

    /**
     * 添加绑定
     */
    @FXML
    private void btnAddBindingClick() {
        if (selectedAgent == null) {
            showAlert("提示", "请先选择或新建一个 Agent");
            return;
        }
        if (selectedAgent.isBuiltIn() && selectedAgent.getId() != null) {
            showAlert("提示", "内置 Agent 不允许直接编辑绑定，请先复制");
            return;
        }
        String stepOption = cbBindingStep.getValue();
        AiSkillDefinition skill = cbBindingSkill.getValue();
        if (stepOption == null || skill == null) {
            showAlert("提示", "请选择步骤和 Skill");
            return;
        }
        AiAgentSkillBinding binding = new AiAgentSkillBinding();
        binding.setTemplateStepNo(parseStepNo(stepOption));
        binding.setBindingOrder(findNextBindingOrder(binding.getTemplateStepNo()));
        binding.setSkillId(skill.getId());
        binding.setSkillCode(skill.getSkillCode());
        binding.setSkillName(skill.getSkillName());
        binding.setEnabled(true);
        currentBindings.add(binding);
        refreshBindingView();
    }

    /**
     * 移除绑定
     */
    @FXML
    private void btnRemoveBindingClick() {
        BindingViewItem item = lvBindings.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        currentBindings.remove(item.binding());
        normalizeBindingOrders();
        refreshBindingView();
    }

    /**
     * 绑定上移
     */
    @FXML
    private void btnMoveBindingUpClick() {
        moveBinding(-1);
    }

    /**
     * 绑定下移
     */
    @FXML
    private void btnMoveBindingDownClick() {
        moveBinding(1);
    }

    /**
     * 关闭对话框
     */
    @FXML
    private void btnCloseClick() {
        Stage stage = (Stage) lvAgents.getScene().getWindow();
        stage.close();
    }

    /**
     * 是否已保存
     * @return true 已保存，false 未保存
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * 加载技能列表
     * @throws SQLException
     */
    private void loadSkills() throws SQLException {
        allSkills.clear();
        allSkills.addAll(skillDefinitionDao.findEnabledDefinitions());
        cbBindingSkill.setItems(FXCollections.observableArrayList(allSkills));
    }

    /**
     * 加载模板列表
     */
    private void loadTemplates() throws SQLException {
        allTemplates.clear();
        allTemplates.addAll(templateDao.findEnabledTemplates());
        if (allTemplates.isEmpty()) {
            for (AiAgentTemplatePreset.TemplateSeed seed : AiAgentTemplatePreset.defaultTemplates()) {
                AiAgentTemplate template = new AiAgentTemplate();
                template.setTemplateCode(seed.templateCode());
                template.setTemplateName(seed.templateName());
                template.setDescription(seed.description());
                template.setSortOrder(seed.sortOrder());
                template.setEnabled(true);
                template.setBuiltIn(true);
                allTemplates.add(template);
            }
        }
        rebuildTemplateChoices(allTemplates);
    }

    /**
     * 加载 Agent 列表
     * @param selectedId 选中的 Agent ID，如果为 null 则选择第一个
     * @throws SQLException 
     */
    private void loadAgents(Integer selectedId) throws SQLException {
        allAgents.clear();
        allAgents.addAll(agentDefinitionDao.findAllActiveDefinitions());
        lvAgents.setItems(FXCollections.observableArrayList(allAgents));
        if (allAgents.isEmpty()) {
            btnNewAgentClick();
            return;
        }

        AiAgentDefinition target = null;
        if (selectedId != null) {
            for (AiAgentDefinition agent : allAgents) {
                if (selectedId.equals(agent.getId())) {
                    target = agent;
                    break;
                }
            }
        }
        if (target == null) {
            target = allAgents.getFirst();
        }
        lvAgents.getSelectionModel().select(target);
        loadAgentDetail(target);
    }

    /**
     * 重建模板选择列表
     * @param templates 模板列表
     */
    private void rebuildTemplateChoices(List<AiAgentTemplate> templates) {
        templateChoiceMap.clear();
        for (AiAgentTemplate template : templates) {
            String templateCode = template.getTemplateCode();
            String templateName = template.getTemplateName() == null || template.getTemplateName().isBlank()
                    ? templateCode
                    : template.getTemplateName();
            String prefix = template.isBuiltIn() ? "[内置] " : "[自定义] ";
            templateChoiceMap.putIfAbsent(prefix + templateName + " (" + templateCode + ")", templateCode);
        }
        cbBaseTemplate.getItems().setAll(templateChoiceMap.keySet());
    }

    /**
     * 加载 Agent 详情
     * @param agent Agent 定义
     */
    private void loadAgentDetail(AiAgentDefinition agent) {
        if (agent == null) {
            return;
        }
        try {
            selectedAgent = agent;
            currentBindings.clear();
            if (agent.getId() != null) {
                currentBindings.addAll(bindingDao.findBindingsByAgentId(agent.getId()));
            }
            fillForm(agent);
        } catch (Exception e) {
            showAlert("错误", "加载 Agent 详情失败: " + e.getMessage());
        }
    }

    /**
     * 填充表单数据
     * @param agent Agent 定义
     */
    private void fillForm(AiAgentDefinition agent) {
        String templateCode = resolveTemplateCode(agent);
        tfAgentName.setText(agent.getAgentName());
        taAgentDescription.setText(agent.getDescription());// 说明
        taPlannerPrompt.setText(agent.getPlannerPrompt());// Planner Prompt
        taExecutionPrompt.setText(agent.getExecutionPrompt());// Execution Prompt
        chkAgentEnabled.setSelected(agent.isEnabled());
        chkFinalSummaryEnabled.setSelected(agent.isFinalSummaryEnabled());
        selectTemplateChoice(templateCode);
        try {
            loadTemplateSteps(templateCode);
        } catch (Exception e) {
            showAlert("错误", "加载模板步骤失败: " + e.getMessage());
            currentTemplateSteps.clear();
            refreshTemplateStepView(null);
        }
        refreshBindingStepOptions(templateCode);
        refreshBindingView();
        updateEditState();
    }

    /**
     * 采集表单数据
     * @return Agent 定义
     * @throws Exception 
     */
    private AiAgentDefinition collectFormData() throws Exception {
        String agentName = normalize(tfAgentName.getText());
        if (agentName.isBlank()) {
            throw new IllegalArgumentException("请输入 Agent 名称");
        }
        if (agentDefinitionDao.existsByName(agentName, selectedAgent.getId())) {
            throw new IllegalArgumentException("Agent 名称已存在，请修改后重试");
        }
        String templateCode = templateChoiceMap.get(cbBaseTemplate.getValue());
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("请选择固定步骤模板");
        }

        selectedAgent.setAgentName(agentName);
        selectedAgent.setDescription(normalize(taAgentDescription.getText()));
        selectedAgent.setPlannerPrompt(normalize(taPlannerPrompt.getText()));
        selectedAgent.setExecutionPrompt(normalize(taExecutionPrompt.getText()));
        selectedAgent.setTemplateCode(templateCode);
        selectedAgent.setEnabled(chkAgentEnabled.isSelected());
        selectedAgent.setFinalSummaryEnabled(chkFinalSummaryEnabled.isSelected());
        selectedAgent.setBuiltIn(false);
        selectedAgent.setDeleted(false);
        return selectedAgent;
    }

    /**
     * 刷新绑定步骤选项
     * @param templateCode
     */
    private void refreshBindingStepOptions(String templateCode) {
        List<String> stepOptions = buildStepOptions(templateCode);
        cbBindingStep.getItems().setAll(stepOptions);
        pruneBindings(resolveAvailableStepNos());
        if (!stepOptions.isEmpty()) {
            cbBindingStep.setValue(stepOptions.getFirst());
        }
    }

    /**
     * 构建绑定步骤选项
     * @param templateCode
     * @return
     */
    private List<String> buildStepOptions(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return List.of();
        }
        if (!templateCode.equals(currentTemplateCode)) {
            try {
                loadTemplateSteps(templateCode);
            } catch (Exception e) {
                showAlert("错误", "加载模板步骤失败: " + e.getMessage());
                return List.of();
            }
        }

        List<String> stepOptions = new ArrayList<>();
        currentTemplateSteps.stream()
                .sorted(Comparator.comparing(AiAgentTemplateStep::getStepNo))
                .forEach(step -> stepOptions.add(step.getStepNo() + ". " + step.getStepName()));
        return stepOptions;
    }

    /**
     * 读取并缓存当前模板步骤
     */
    private void loadTemplateSteps(String templateCode) throws SQLException {
        currentTemplateCode = templateCode;
        AiAgentTemplate template = resolveTemplateByCode(templateCode);
        currentTemplateBuiltIn = template != null && template.isBuiltIn();
        currentTemplateSteps.clear();

        List<AiAgentTemplateStep> dbSteps = templateDao.findEnabledStepsByTemplateCode(templateCode);
        if (!dbSteps.isEmpty()) {
            for (AiAgentTemplateStep dbStep : dbSteps) {
                currentTemplateSteps.add(copyTemplateStep(dbStep));
            }
        } else {
            for (AiAgentTemplatePreset.StepSeed seed : AiAgentTemplatePreset.defaultSteps(templateCode)) {
                AiAgentTemplateStep step = new AiAgentTemplateStep();
                step.setTemplateCode(templateCode);
                step.setStepNo(seed.stepNo());
                step.setSkillCode(seed.skillCode());
                step.setStepName(seed.stepName());
                step.setPurpose(seed.purpose());
                step.setExpectedOutput(seed.expectedOutput());
                step.setSortOrder(seed.sortOrder());
                step.setEnabled(true);
                currentTemplateSteps.add(step);
            }
        }

        currentTemplateSteps.sort(Comparator.comparing(AiAgentTemplateStep::getStepNo));
        refreshTemplateStepView(null);
    }

    /**
     * 刷新模板步骤列表
     */
    private void refreshTemplateStepView(Integer preferredStepNo) {
        List<TemplateStepViewItem> items = new ArrayList<>();
        for (AiAgentTemplateStep step : currentTemplateSteps) {
            items.add(new TemplateStepViewItem(step));
        }
        lvTemplateSteps.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            TemplateStepViewItem target = null;
            if (preferredStepNo != null) {
                for (TemplateStepViewItem item : items) {
                    if (preferredStepNo.equals(item.step().getStepNo())) {
                        target = item;
                        break;
                    }
                }
            }
            if (target == null) {
                target = items.getFirst();
            }
            lvTemplateSteps.getSelectionModel().select(target);
            showTemplateStepDetail(target.step());
        } else {
            showTemplateStepDetail(null);
        }
        updateEditState();
    }

    /**
     * 展示模板步骤详情
     */
    private void showTemplateStepDetail(AiAgentTemplateStep step) {
        if (step == null) {
            tfTemplateStepName.clear();
            taTemplateStepPurpose.clear();
            taTemplateStepExpectedOutput.clear();
            return;
        }
        tfTemplateStepName.setText(step.getStepName() == null ? "" : step.getStepName());
        taTemplateStepPurpose.setText(step.getPurpose() == null ? "" : step.getPurpose());
        taTemplateStepExpectedOutput.setText(step.getExpectedOutput() == null ? "" : step.getExpectedOutput());
    }

    /**
     * 将编辑区内容回写到当前选中步骤
     */
    private void applyTemplateStepEditor(boolean validate) {
        TemplateStepViewItem selected = lvTemplateSteps.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        String stepName = normalize(tfTemplateStepName.getText());
        String purpose = normalize(taTemplateStepPurpose.getText());
        String expectedOutput = normalize(taTemplateStepExpectedOutput.getText());
        if (validate && stepName.isBlank()) {
            throw new IllegalArgumentException("步骤名称不能为空");
        }

        AiAgentTemplateStep step = selected.step();
        step.setStepName(stepName);
        step.setPurpose(purpose);
        step.setExpectedOutput(expectedOutput);
    }

    /**
     * 重新编号模板步骤并同步绑定关系
     */
    private void resequenceTemplateStepsAndBindings() {
        Map<Integer, Integer> stepNoMapping = normalizeTemplateSteps(currentTemplateSteps);
        remapBindings(stepNoMapping);
    }

    /**
     * 规范模板步骤编号并返回旧号到新号映射
     */
    private Map<Integer, Integer> normalizeTemplateSteps(List<AiAgentTemplateStep> steps) {
        Map<Integer, Integer> mapping = new LinkedHashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            AiAgentTemplateStep step = steps.get(i);
            Integer oldNo = step.getStepNo();
            int newNo = i + 1;
            if (oldNo != null) {
                mapping.put(oldNo, newNo);
            }
            step.setStepNo(newNo);
            step.setSortOrder(newNo);
            if (step.getSkillCode() == null || step.getSkillCode().isBlank()) {
                step.setSkillCode("custom_step_" + newNo);
            }
            if (step.getStepName() == null || step.getStepName().isBlank()) {
                step.setStepName("步骤 " + newNo);
            }
        }
        return mapping;
    }

    /**
     * 根据步骤映射调整绑定关系
     */
    private void remapBindings(Map<Integer, Integer> stepNoMapping) {
        currentBindings.removeIf(binding -> binding.getTemplateStepNo() == null || !stepNoMapping.containsKey(binding.getTemplateStepNo()));
        for (AiAgentSkillBinding binding : currentBindings) {
            binding.setTemplateStepNo(stepNoMapping.get(binding.getTemplateStepNo()));
        }
        normalizeBindingOrders();
    }

    /**
     * 移动模板步骤
     */
    private void moveTemplateStep(int direction) {
        if (!isTemplateEditable()) {
            showAlert("提示", "内置模板不支持直接修改，请先使用“另存为模板”");
            return;
        }
        TemplateStepViewItem selected = lvTemplateSteps.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int currentIndex = currentTemplateSteps.indexOf(selected.step());
        int targetIndex = currentIndex + direction;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= currentTemplateSteps.size()) {
            return;
        }

        Collections.swap(currentTemplateSteps, currentIndex, targetIndex);
        resequenceTemplateStepsAndBindings();
        int selectedStepNo = currentTemplateSteps.get(targetIndex).getStepNo();
        refreshTemplateStepView(selectedStepNo);
        refreshBindingStepOptions(currentTemplateCode);
    }

    /**
     * 持久化当前模板步骤
     */
    private void persistTemplateSteps(String templateCode) throws SQLException {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("模板代码不能为空");
        }
        if (currentTemplateBuiltIn) {
            return;
        }
        if (!templateCode.equals(currentTemplateCode)) {
            loadTemplateSteps(templateCode);
        }
        if (currentTemplateSteps.isEmpty()) {
            throw new IllegalArgumentException("当前模板没有可保存的步骤");
        }

        currentTemplateSteps.sort(Comparator.comparing(AiAgentTemplateStep::getStepNo));
        for (int i = 0; i < currentTemplateSteps.size(); i++) {
            AiAgentTemplateStep step = currentTemplateSteps.get(i);
            if (step.getStepName() == null || step.getStepName().isBlank()) {
                throw new IllegalArgumentException("步骤 " + step.getStepNo() + " 的名称不能为空");
            }
            step.setTemplateCode(templateCode);
            step.setSortOrder(i + 1);
            step.setEnabled(true);
        }
        templateDao.replaceTemplateSteps(templateCode, currentTemplateSteps);
    }

    /**
     * 判断当前模板是否可编辑
     */
    private boolean isTemplateEditable() {
        return !currentTemplateBuiltIn;
    }

    /**
     * 通过模板代码解析模板元数据
     */
    private AiAgentTemplate resolveTemplateByCode(String templateCode) {
        if (templateCode == null) {
            return null;
        }
        for (AiAgentTemplate template : allTemplates) {
            if (templateCode.equals(template.getTemplateCode())) {
                return template;
            }
        }
        return null;
    }

    /**
     * 输入弹窗
     */
    private String requestInput(String title, String message, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * 构建默认自定义模板代码
     */
    private String buildDefaultCustomTemplateCode(String sourceTemplateCode) {
        String base = (sourceTemplateCode == null || sourceTemplateCode.isBlank())
                ? "custom_template"
                : sourceTemplateCode + "_custom";
        Set<String> existingCodes = new LinkedHashSet<>();
        for (AiAgentTemplate template : allTemplates) {
            existingCodes.add(template.getTemplateCode());
        }
        if (!existingCodes.contains(base)) {
            return base;
        }
        int index = 1;
        while (existingCodes.contains(base + "_" + index)) {
            index++;
        }
        return base + "_" + index;
    }

    /**
     * 解析当前模板可用步骤号集合
     */
    private Set<Integer> resolveAvailableStepNos() {
        Set<Integer> stepNos = new java.util.HashSet<>();
        for (AiAgentTemplateStep step : currentTemplateSteps) {
            if (step.getStepNo() != null) {
                stepNos.add(step.getStepNo());
            }
        }
        return stepNos;
    }

    /**
     * 复制模板步骤实体，避免直接操作 DAO 返回对象
     */
    private AiAgentTemplateStep copyTemplateStep(AiAgentTemplateStep source) {
        AiAgentTemplateStep copied = new AiAgentTemplateStep();
        copied.setId(source.getId());
        copied.setTemplateCode(source.getTemplateCode());
        copied.setStepNo(source.getStepNo());
        copied.setSkillCode(source.getSkillCode());
        copied.setStepName(source.getStepName());
        copied.setPurpose(source.getPurpose());
        copied.setExpectedOutput(source.getExpectedOutput());
        copied.setSortOrder(source.getSortOrder());
        copied.setEnabled(source.isEnabled());
        return copied;
    }

    /**
     * 裁剪绑定列表，移除不存在于模板中的步骤绑定
     * @param availableStepNos 可用步骤号集合
     */
    private void pruneBindings(Set<Integer> availableStepNos) {
        currentBindings.removeIf(binding -> binding.getTemplateStepNo() != null && !availableStepNos.contains(binding.getTemplateStepNo()));
        normalizeBindingOrders();
        refreshBindingView();
    }

    /**
     * 刷新绑定列表视图
     * 1. 按步骤号和绑定顺序排序
     */
    private void refreshBindingView() {
        normalizeBindingOrders();
        List<BindingViewItem> viewItems = new ArrayList<>();
        currentBindings.stream()
                .sorted(Comparator.comparing(AiAgentSkillBinding::getTemplateStepNo).thenComparing(AiAgentSkillBinding::getBindingOrder))
                .forEach(binding -> viewItems.add(new BindingViewItem(binding)));
        lvBindings.setItems(FXCollections.observableArrayList(viewItems));
    }

    /**
     * 规范绑定顺序
     */
    private void normalizeBindingOrders() {
        Map<Integer, Integer> stepCounters = new LinkedHashMap<>();
        currentBindings.sort(Comparator.comparing(AiAgentSkillBinding::getTemplateStepNo).thenComparing(AiAgentSkillBinding::getBindingOrder));
        for (AiAgentSkillBinding binding : currentBindings) {
            int nextOrder = stepCounters.getOrDefault(binding.getTemplateStepNo(), 0) + 1;
            binding.setBindingOrder(nextOrder);
            stepCounters.put(binding.getTemplateStepNo(), nextOrder);
        }
    }

    /**
     * 移动绑定项
     * @param direction -1 上移，1 下移
     */
    private void moveBinding(int direction) {
        BindingViewItem item = lvBindings.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        List<AiAgentSkillBinding> sameStepBindings = currentBindings.stream()
                .filter(binding -> binding.getTemplateStepNo().equals(item.binding().getTemplateStepNo()))
                .sorted(Comparator.comparing(AiAgentSkillBinding::getBindingOrder))
                .toList();
        int currentIndex = sameStepBindings.indexOf(item.binding());
        int targetIndex = currentIndex + direction;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= sameStepBindings.size()) {
            return;
        }
        AiAgentSkillBinding current = sameStepBindings.get(currentIndex);
        AiAgentSkillBinding target = sameStepBindings.get(targetIndex);
        int currentOrder = current.getBindingOrder();
        current.setBindingOrder(target.getBindingOrder());
        target.setBindingOrder(currentOrder);
        refreshBindingView();
        lvBindings.getSelectionModel().select(findViewItem(current));
    }

    /**
     * 查找绑定项对应的视图项
     * @param binding 绑定项
     * @return 视图项，如果未找到则返回 null
     */
    private BindingViewItem findViewItem(AiAgentSkillBinding binding) {
        for (BindingViewItem item : lvBindings.getItems()) {
            if (item.binding() == binding) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找指定步骤号的下一个绑定顺序
     * @param stepNo 步骤号
     * @return 下一个绑定顺序
     */
    private int findNextBindingOrder(Integer stepNo) {
        int max = 0;
        for (AiAgentSkillBinding binding : currentBindings) {
            if (binding.getTemplateStepNo().equals(stepNo)) {
                max = Math.max(max, binding.getBindingOrder());
            }
        }
        return max + 1;
    }

    /**
     * 更新编辑状态
     */
    private void updateEditState() {
        boolean builtIn = selectedAgent != null && selectedAgent.isBuiltIn() && selectedAgent.getId() != null;
        boolean templateEditable = isTemplateEditable();
        lblEditMode.setText(builtIn ? "当前为内置 Agent，只允许复制" : "当前为可编辑 Agent");
        tfAgentName.setDisable(builtIn);
        cbBaseTemplate.setDisable(builtIn);
        chkAgentEnabled.setDisable(builtIn);
        chkFinalSummaryEnabled.setDisable(builtIn);
        taAgentDescription.setDisable(builtIn);
        taPlannerPrompt.setDisable(builtIn);
        taExecutionPrompt.setDisable(builtIn);
        lvTemplateSteps.setDisable(builtIn || !templateEditable);
        tfTemplateStepName.setDisable(builtIn || !templateEditable);
        taTemplateStepPurpose.setDisable(builtIn || !templateEditable);
        taTemplateStepExpectedOutput.setDisable(builtIn || !templateEditable);
        btnApplyTemplateStep.setDisable(builtIn || !templateEditable || lvTemplateSteps.getSelectionModel().getSelectedItem() == null);
        btnTemplateStepAdd.setDisable(builtIn || !templateEditable);
        btnTemplateStepDelete.setDisable(builtIn || !templateEditable || lvTemplateSteps.getSelectionModel().getSelectedItem() == null);
        btnTemplateStepMoveUp.setDisable(builtIn || !templateEditable || lvTemplateSteps.getSelectionModel().getSelectedItem() == null);
        btnTemplateStepMoveDown.setDisable(builtIn || !templateEditable || lvTemplateSteps.getSelectionModel().getSelectedItem() == null);
        btnTemplateSaveAs.setDisable(builtIn || !currentTemplateBuiltIn);
        cbBindingStep.setDisable(builtIn);
        cbBindingSkill.setDisable(builtIn);
        btnDeleteAgent.setDisable(builtIn || selectedAgent == null || selectedAgent.getId() == null);
        btnSaveAgent.setDisable(selectedAgent == null);
        btnCopyAgent.setDisable(selectedAgent == null);
    }

    /**
     * 选择模板选项
     * @param templateCode 模板代码
     */
    private void selectTemplateChoice(String templateCode) {
        for (Map.Entry<String, String> entry : templateChoiceMap.entrySet()) {
            if (entry.getValue().equals(templateCode)) {
                cbBaseTemplate.setValue(entry.getKey());
                return;
            }
        }
        if (!cbBaseTemplate.getItems().isEmpty()) {
            cbBaseTemplate.setValue(cbBaseTemplate.getItems().getFirst());
        }
    }

    /**
     * 解析步骤号
     * @param stepOption 步骤选项字符串
     * @return 步骤号
     */
    private int parseStepNo(String stepOption) {
        int dotIndex = stepOption.indexOf('.');
        return Integer.parseInt(dotIndex > 0 ? stepOption.substring(0, dotIndex).trim() : stepOption.trim());
    }

    /**
     * 解析模板代码，如果模板代码为空，则使用 Agent 代码作为模板代码
     * @param definition Agent 定义
     * @return 模板代码
     */
    private String resolveTemplateCode(AiAgentDefinition definition) {
        if (definition.getTemplateCode() == null || definition.getTemplateCode().isBlank()) {
            return definition.getAgentCode();
        }
        return definition.getTemplateCode();
    }

    /**
     * 默认模板代码
     * @return 默认模板代码
     */
    private String defaultTemplateCode() {
        return templateChoiceMap.values().stream().findFirst().orElse("document_summary");
    }

    /**
     * 规范化文本，去除前后空格
     * @param text 待规范化的文本
     * @return 规范化后的文本
     */
    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * 警告提示
     * @param title 标题
     * @param message 内容
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示确认对话框
     * @param title 对话框标题
     * @param message 对话框内容
     * @return 用户点击的按钮类型
     */
    private Optional<ButtonType> showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }

    /**
     * BindingViewItem
     * @param binding 绑定对象
     */
    private record BindingViewItem(AiAgentSkillBinding binding) {
        private String toDisplayText() {
            return "步骤 " + binding.getTemplateStepNo() + " / 顺序 " + binding.getBindingOrder() + " / " + binding.getSkillName();
        }
    }

    /**
     * 模板步骤视图项
     */
    private record TemplateStepViewItem(AiAgentTemplateStep step) {
        private String toDisplayText() {
            return step.getStepNo() + ". " + step.getStepName();
        }
    }
}