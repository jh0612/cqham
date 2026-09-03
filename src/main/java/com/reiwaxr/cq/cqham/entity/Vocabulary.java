package com.reiwaxr.cq.cqham.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class Vocabulary {
    /* ID */
    private Integer id;
    /* 原文 */
    private String sourceText;
    /* 译文 */
    private String targetText;
    /* 分类：名词/短句/专业词汇 */
    private String tagName;
    /* 创建时间 */
    private LocalDateTime createTime;
    /* 删除标记：0-未删除，1-已删除 */
    private Integer deleteFlg;
    /* 删除时间 */
    private LocalDateTime deletedAt;

    public Vocabulary() {
    }

    public Vocabulary(Integer id, String sourceText, String targetText, String tagName, LocalDateTime createTime) {
        this.id = id;
        this.sourceText = sourceText;
        this.targetText = targetText;
        this.tagName = tagName;
        this.createTime = createTime;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleteFlg() {
        return deleteFlg;
    }

    public void setDeleteFlg(Integer deleteFlg) {
        this.deleteFlg = deleteFlg;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
