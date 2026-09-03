package com.reiwaxr.cq.cqham.controller;

import com.reiwaxr.cq.cqham.dao.AiSkillDefinitionDao;
import com.reiwaxr.cq.cqham.entity.AiSkillDefinition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Skill管理对话框控制器
 */
public class AiSkillManagerDialogController {

    @FXML private ListView<AiSkillDefinition> lvSkills;
    @FXML private TextField tfSkillName;
    @FXML private TextField tfCategory;
    @FXML private TextField tfTags;
    @FXML private CheckBox chkSkillEnabled;
    @FXML private TextArea taSkillDescription;
    @FXML private TextArea taSystemPrompt;
    @FXML private TextArea taInputTemplate;
    @FXML private TextArea taOutputTemplate;
    @FXML private TextArea taExampleInput;
    @FXML private TextArea taExampleOutput;

    private final AiSkillDefinitionDao skillDefinitionDao = new AiSkillDefinitionDao();
    private final List<AiSkillDefinition> allSkills = new ArrayList<>();
    private AiSkillDefinition selectedSkill;
    private boolean saved;

    /**
     * 初始化
     */
    @FXML
    public void initialize() {
        lvSkills.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AiSkillDefinition item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String prefix = item.isBuiltIn() ? "[内置] " : "[自定义] ";
                String suffix = item.isEnabled() ? "" : " (停用)";
                setText(prefix + item.getSkillName() + suffix);
            }
        });
        lvSkills.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadSkillDetail(newValue));
    }

    /**
     * 初始化数据
     */
    public void initData() {
        try {
            loadSkills(null);
        } catch (Exception e) {
            showAlert("错误", "加载 Skill 数据失败: " + e.getMessage());
        }
    }

    /**
     * 新建Skill
     */
    @FXML
    private void btnNewSkillClick() {
        AiSkillDefinition draft = new AiSkillDefinition();
        draft.setSkillCode("custom_skill_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        draft.setSkillName("");
        draft.setCategoryName("");
        draft.setTags("");
        draft.setDescription("");
        draft.setSystemPrompt("");
        draft.setInputTemplate("");
        draft.setOutputTemplate("");
        draft.setExampleInput("");
        draft.setExampleOutput("");
        draft.setEnabled(true);
        draft.setBuiltIn(false);
        draft.setDeleted(false);
        selectedSkill = draft;
        fillForm(draft);
    }

    /**
     * 删除Skill
     */
    @FXML
    private void btnDeleteSkillClick() {
        if (selectedSkill == null || selectedSkill.getId() == null) {
            return;
        }
        if (selectedSkill.isBuiltIn()) {
            showAlert("提示", "内置 Skill 不允许直接删除");
            return;
        }
        Optional<ButtonType> result = showConfirm("确认删除", "确定要逻辑删除当前 Skill 吗？已绑定的 Agent 将自动失效该绑定。 ");
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            skillDefinitionDao.logicalDelete(selectedSkill.getId());
            saved = true;
            loadSkills(null);
        } catch (Exception e) {
            showAlert("错误", "删除 Skill 失败: " + e.getMessage());
        }
    }

    /**
     * 刷新
     */
    @FXML
    private void btnRefreshClick() {
        try {
            loadSkills(selectedSkill != null ? selectedSkill.getId() : null);
        } catch (Exception e) {
            showAlert("错误", "刷新 Skill 数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存Skill
     */
    @FXML
    private void btnSaveSkillClick() {
        if (selectedSkill == null) {
            showAlert("提示", "请先新建或选择一个 Skill");
            return;
        }
        if (selectedSkill.isBuiltIn() && selectedSkill.getId() != null) {
            showAlert("提示", "内置 Skill 不允许直接编辑");
            return;
        }
        try {
            AiSkillDefinition formSkill = collectFormData();
            if (formSkill.getId() == null) {
                formSkill.setSortOrder(skillDefinitionDao.findNextSortOrder());
                int newId = skillDefinitionDao.insert(formSkill);
                formSkill.setId(newId);
            } else {
                skillDefinitionDao.update(formSkill);
            }
            saved = true;
            selectedSkill = formSkill;
            loadSkills(formSkill.getId());
        } catch (Exception e) {
            showAlert("错误", "保存 Skill 失败: " + e.getMessage());
        }
    }

    /**
     * 关闭
     */
    @FXML
    private void btnCloseClick() {
        Stage stage = (Stage) lvSkills.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void loadSkills(Integer selectedId) throws Exception {
        allSkills.clear();
        allSkills.addAll(skillDefinitionDao.findAllActiveDefinitions());
        lvSkills.setItems(FXCollections.observableArrayList(allSkills));
        if (allSkills.isEmpty()) {
            btnNewSkillClick();
            return;
        }
        AiSkillDefinition target = null;
        if (selectedId != null) {
            for (AiSkillDefinition skill : allSkills) {
                if (selectedId.equals(skill.getId())) {
                    target = skill;
                    break;
                }
            }
        }
        if (target == null) {
            target = allSkills.getFirst();
        }
        lvSkills.getSelectionModel().select(target);
        loadSkillDetail(target);
    }

    private void loadSkillDetail(AiSkillDefinition skill) {
        if (skill == null) {
            return;
        }
        selectedSkill = skill;
        fillForm(skill);
    }

    private void fillForm(AiSkillDefinition skill) {
        tfSkillName.setText(skill.getSkillName());
        tfCategory.setText(skill.getCategoryName());
        tfTags.setText(skill.getTags());
        chkSkillEnabled.setSelected(skill.isEnabled());
        taSkillDescription.setText(skill.getDescription());
        taSystemPrompt.setText(skill.getSystemPrompt());
        taInputTemplate.setText(skill.getInputTemplate());
        taOutputTemplate.setText(skill.getOutputTemplate());
        taExampleInput.setText(skill.getExampleInput());
        taExampleOutput.setText(skill.getExampleOutput());
    }

    private AiSkillDefinition collectFormData() throws Exception {
        String skillName = normalize(tfSkillName.getText());
        if (skillName.isBlank()) {
            throw new IllegalArgumentException("请输入 Skill 名称");
        }
        if (skillDefinitionDao.existsByName(skillName, selectedSkill.getId())) {
            throw new IllegalArgumentException("Skill 名称已存在，请修改后重试");
        }
        selectedSkill.setSkillName(skillName);
        selectedSkill.setCategoryName(normalize(tfCategory.getText()));
        selectedSkill.setTags(normalize(tfTags.getText()));
        selectedSkill.setDescription(normalize(taSkillDescription.getText()));
        selectedSkill.setSystemPrompt(normalize(taSystemPrompt.getText()));
        selectedSkill.setInputTemplate(normalize(taInputTemplate.getText()));
        selectedSkill.setOutputTemplate(normalize(taOutputTemplate.getText()));
        selectedSkill.setExampleInput(normalize(taExampleInput.getText()));
        selectedSkill.setExampleOutput(normalize(taExampleOutput.getText()));
        selectedSkill.setEnabled(chkSkillEnabled.isSelected());
        selectedSkill.setBuiltIn(false);
        selectedSkill.setDeleted(false);
        return selectedSkill;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}