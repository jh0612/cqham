package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent定義エンティティ
 */
@Setter
@Getter
public class AiAgentDefinition {
    /* ID */
    private Integer id;
    /* Agentコード */
    private String agentCode;
    /* Agent名称 */
    private String agentName;
    /* 説明 */
    private String description;
    /* プランナープロンプト */
    private String plannerPrompt;
    /* 実行プロンプト */
    private String executionPrompt;
    /* 固定步骤模板代码 */
    private String templateCode;
    /* 並び順 */
    private Integer sortOrder;
    /* 有効フラグ */
    private boolean enabled;
    /* 最終要約有効フラグ */
    private boolean finalSummaryEnabled;
    /* 内置フラグ */
    private boolean builtIn;
    /* 論理削除フラグ */
    private boolean deleted;

}