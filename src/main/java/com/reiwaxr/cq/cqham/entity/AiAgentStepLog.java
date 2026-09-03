package com.reiwaxr.cq.cqham.entity;

/**
 * Agent步骤执行记录
 */
public class AiAgentStepLog {
    private Long id;
    private String runId;
    private Integer stepNo;
    private String stepName;
    private String skillCode;
    private String stepInput;
    private String stepOutput;
    private String status;
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getStepInput() {
        return stepInput;
    }

    public void setStepInput(String stepInput) {
        this.stepInput = stepInput;
    }

    public String getStepOutput() {
        return stepOutput;
    }

    public void setStepOutput(String stepOutput) {
        this.stepOutput = stepOutput;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}