package com.reiwaxr.cq.cqham.entity;

import lombok.Data;

/**
 * AI场景分类实体
 */
@Data
public class AiSceneCategory {
    /* ID */
    private Integer id;
    /* 场景名称 */
    private String sceneName;
    /* 子分类 */
    private String subCategory;
    /* 系统提示 */
    private String systemPrompt;
    /* 排序顺序 */
    private Integer sortOrder;
    /* 是否为自定义场景 */
    private boolean custom;
    /* 标签 */
    private String tags;
    /* 备注 */
    private String remark;
    /* 创建时间 */
    private String createTime;
    /* 更新时间 */
    private String updateTime;

    public AiSceneCategory(Integer id, String sceneName, String subCategory, String systemPrompt, Integer sortOrder) {
        this.id = id;
        this.sceneName = sceneName;
        this.subCategory = subCategory;
        this.systemPrompt = systemPrompt;
        this.sortOrder = sortOrder;
    }

    public AiSceneCategory() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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