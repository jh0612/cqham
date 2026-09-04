package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent步骤执行记录
 */
@Setter
@Getter
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

}