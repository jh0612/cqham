package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent固定模板步骤定义
 */
@Setter
@Getter
public class AiAgentTemplateStep {
    private Integer id;
    private String templateCode;
    private Integer stepNo;
    private String skillCode;
    private String stepName;
    private String purpose;
    private String expectedOutput;
    private Integer sortOrder;
    private boolean enabled;

}
