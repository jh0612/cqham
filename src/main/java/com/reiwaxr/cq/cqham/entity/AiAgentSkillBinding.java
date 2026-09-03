package com.reiwaxr.cq.cqham.entity;

/**
 * Agent与Skillの绑定关系
 */
public class AiAgentSkillBinding {
    private Integer id;
    private Integer agentId;
    private Integer templateStepNo;
    private Integer bindingOrder;
    private Integer skillId;
    private String skillCode;
    private String skillName;
    private boolean enabled;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public Integer getTemplateStepNo() {
        return templateStepNo;
    }

    public void setTemplateStepNo(Integer templateStepNo) {
        this.templateStepNo = templateStepNo;
    }

    public Integer getBindingOrder() {
        return bindingOrder;
    }

    public void setBindingOrder(Integer bindingOrder) {
        this.bindingOrder = bindingOrder;
    }

    public Integer getSkillId() {
        return skillId;
    }

    public void setSkillId(Integer skillId) {
        this.skillId = skillId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}