package com.reiwaxr.cq.cqham.entity;

import java.time.LocalDateTime;

public class VocabularyTag {
    /* ID */
    private Integer id;
    /* 标签名称 */
    private String tagName;
    /* 创建时间 */
    private LocalDateTime createTime;
    /* 更新时间 */
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}