package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent実行记录
 */
@Setter
@Getter
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

}