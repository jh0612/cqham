package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent与Skillの绑定关系
 */
@Setter
@Getter
public class AiAgentSkillBinding {
    private Integer id;
    private Integer agentId;
    private Integer templateStepNo;
    private Integer bindingOrder;
    private Integer skillId;
    private String skillCode;
    private String skillName;
    private boolean enabled;

}