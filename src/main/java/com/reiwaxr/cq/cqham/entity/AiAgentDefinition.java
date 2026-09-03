package com.reiwaxr.cq.cqham.entity;

/**
 * Agent定義エンティティ
 */
public class AiAgentDefinition {
    /* ID */
    private Integer id;
    /* Agentコード */
    private String agentCode;
    /* Agent名称 */
    private String agentName;
    /* 説明 */
    private String description;
    /* プランナープロンプト */
    private String plannerPrompt;
    /* 実行プロンプト */
    private String executionPrompt;
    /* 固定步骤模板代码 */
    private String templateCode;
    /* 並び順 */
    private Integer sortOrder;
    /* 有効フラグ */
    private boolean enabled;
    /* 最終要約有効フラグ */
    private boolean finalSummaryEnabled;
    /* 内置フラグ */
    private boolean builtIn;
    /* 論理削除フラグ */
    private boolean deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlannerPrompt() {
        return plannerPrompt;
    }

    public void setPlannerPrompt(String plannerPrompt) {
        this.plannerPrompt = plannerPrompt;
    }

    public String getExecutionPrompt() {
        return executionPrompt;
    }

    public void setExecutionPrompt(String executionPrompt) {
        this.executionPrompt = executionPrompt;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFinalSummaryEnabled() {
        return finalSummaryEnabled;
    }

    public void setFinalSummaryEnabled(boolean finalSummaryEnabled) {
        this.finalSummaryEnabled = finalSummaryEnabled;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}