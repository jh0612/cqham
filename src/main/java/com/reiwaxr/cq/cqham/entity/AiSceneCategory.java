package com.reiwaxr.cq.cqham.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * AI场景分类实体
 */
@Setter
@Getter
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

}