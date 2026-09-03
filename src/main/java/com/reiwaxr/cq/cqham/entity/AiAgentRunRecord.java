package com.reiwaxr.cq.cqham.entity;

/**
 * Agent実行记录
 */
public class AiAgentRunRecord {
    private Integer id;
    private String runId;
    private String agentCode;
    private String userGoal;
    private String contextSummary;
    private String planJson;
    private String requestJson;
    private String status;
    private String finalResult;
    private String errorMessage;
    private Integer completedStepCount;
    private Integer currentStepNo;
    private String resumeSourceRunId;
    private String resumeMode;
    private String currentStepName;
    private String recentError;
    private String createTime;
    private String updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getUserGoal() {
        return userGoal;
    }

    public void setUserGoal(String userGoal) {
        this.userGoal = userGoal;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }

    public String getPlanJson() {
        return planJson;
    }

    public void setPlanJson(String planJson) {
        this.planJson = planJson;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getCompletedStepCount() {
        return completedStepCount;
    }

    public void setCompletedStepCount(Integer completedStepCount) {
        this.completedStepCount = completedStepCount;
    }

    public Integer getCurrentStepNo() {
        return currentStepNo;
    }

    public void setCurrentStepNo(Integer currentStepNo) {
        this.currentStepNo = currentStepNo;
    }

    public String getResumeSourceRunId() {
        return resumeSourceRunId;
    }

    public void setResumeSourceRunId(String resumeSourceRunId) {
        this.resumeSourceRunId = resumeSourceRunId;
    }

    public String getResumeMode() {
        return resumeMode;
    }

    public void setResumeMode(String resumeMode) {
        this.resumeMode = resumeMode;
    }

    public String getCurrentStepName() {
        return currentStepName;
    }

    public void setCurrentStepName(String currentStepName) {
        this.currentStepName = currentStepName;
    }

    public String getRecentError() {
        return recentError;
    }

    public void setRecentError(String recentError) {
        this.recentError = recentError;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}