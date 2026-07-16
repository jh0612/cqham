package com.reiwaxr.cq.cqham.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class Vocabulary {
    private Integer id;
    private String sourceText;   // 原文
    private String targetText;   // 译文
    private String tagName;     // 分类：名词/短句/专业词汇
    private LocalDateTime createTime;
    private Integer deleteFlg;
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

    public String getWordType() {
        return tagName;
    }

    public void setWordType(String wordType) {
        this.tagName = wordType;
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
