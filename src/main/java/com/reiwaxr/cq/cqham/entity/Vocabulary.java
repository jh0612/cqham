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
    private String wordType;     // 分类：名词/短句/专业词汇
    private LocalDateTime createTime;

    public Vocabulary() {
    }

    public Vocabulary(Integer id, String sourceText, String targetText, String wordType, LocalDateTime createTime) {
        this.id = id;
        this.sourceText = sourceText;
        this.targetText = targetText;
        this.wordType = wordType;
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

    public String getWordType() {
        return wordType;
    }

    public void setWordType(String wordType) {
        this.wordType = wordType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
