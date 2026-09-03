package com.reiwaxr.cq.cqham.service;

import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.entity.AiSceneCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Agent任务适配与路由提示服务
 */
public class AiAgentRoutingService {

    public enum TaskIntent {
        DOCUMENT_SUMMARY,// 文档总结
        DATA_ANALYSIS,// 数据分析
        ISSUE_DIAGNOSIS,// 问题诊断
        CONTENT_ORGANIZER,// 内容整理
        CODE_REVIEW,// 代码审查
        TRANSLATION,// 翻译
        UNKNOWN// 未知
    }

    public record RoutingDecision(
            TaskIntent intent,
            boolean compatible,
            String summary,
            String detail,
            List<String> recommendedAgentNames) {}

    /**
     * 评估当前任务是否适合选中的Agent
     * @param userInput 用户输入
     * @param currentScene 当前场景
     * @param agentDefinition 选中的Agent
     * @return 路由决策
     */
    public RoutingDecision evaluate(String userInput, AiSceneCategory currentScene, AiAgentDefinition agentDefinition) {
        TaskIntent intent = detectIntent(userInput, currentScene);
        List<String> recommendedAgents = recommendedAgents(intent);

        if (agentDefinition == null) {
            return new RoutingDecision(intent, false, "尚未选择 Agent 类型", "请先选择 Agent 类型后再执行。", recommendedAgents);
        }

        TaskIntent agentIntent = supportedIntent(agentDefinition.getAgentCode());
        if (intent != TaskIntent.UNKNOWN && agentIntent != intent) {
            return new RoutingDecision(
                    intent,
                    false,
                    "当前任务与所选 Agent 类型不匹配。",
                    "所选 Agent：“" + agentDefinition.getAgentName() + "”。识别到的任务更接近 “"
                            + intentDisplayName(intent) + "”。建议切换到更匹配的 Agent 后再执行。",
                    recommendedAgents);
        }

        String detail = agentDefinition == null
                ? ""
                : "当前任务与 “" + agentDefinition.getAgentName() + "” 基本匹配，可继续执行。";
        return new RoutingDecision(intent, true, "任务与 Agent 匹配", detail, recommendedAgents);
    }

    /**
     * 根据输入检测任务意图
     * @param userInput 用户输入
     * @param currentScene 当前场景
     */
    public TaskIntent detectIntent(String userInput, AiSceneCategory currentScene) {
        String sceneName = currentScene != null && currentScene.getSceneName() != null
                ? currentScene.getSceneName().trim()
                : "";
        String subCategory = currentScene != null && currentScene.getSubCategory() != null
                ? currentScene.getSubCategory().trim()
                : "";
        String normalizedInput = userInput == null ? "" : userInput.toLowerCase(Locale.ROOT);

        if (sceneName.contains("翻译") || subCategory.contains("译")
                || containsAny(normalizedInput, "translate", "translation", "翻译", "中译", "英译", "日译", "韩译", "译成")) {
            return TaskIntent.TRANSLATION;
        }
        if (containsAny(normalizedInput, "代码审查", "code review", "review code", "审查代码", "找 bug", "找问题", "评审代码")) {
            return TaskIntent.CODE_REVIEW;
        }
        if (containsAny(normalizedInput, "报错", "异常", "故障", "问题定位", "排查", "root cause", "diagnose", "debug", "诊断")) {
            return TaskIntent.ISSUE_DIAGNOSIS;
        }
        if (containsAny(normalizedInput, "数据分析", "趋势", "指标", "异常值", "统计", "excel", "表格", "data analysis", "metric")) {
            return TaskIntent.DATA_ANALYSIS;
        }
        if (containsAny(normalizedInput, "整理", "归类", "分类", "重组", "清洗", "格式化", "organize", "整理内容")) {
            return TaskIntent.CONTENT_ORGANIZER;
        }
        if (containsAny(normalizedInput, "总结", "摘要", "纪要", "提炼", "概括", "summary", "summarize")) {
            return TaskIntent.DOCUMENT_SUMMARY;
        }
        return TaskIntent.UNKNOWN;
    }

    /**
     * 获取Agent支持的主意图
     * @param agentCode Agent代码
     * @return 主意图
     */
    public TaskIntent supportedIntent(String agentCode) {
        return switch (agentCode) {
            case "document_summary" -> TaskIntent.DOCUMENT_SUMMARY;
            case "document_translation" -> TaskIntent.TRANSLATION;
            case "data_analysis" -> TaskIntent.DATA_ANALYSIS;
            case "issue_diagnosis" -> TaskIntent.ISSUE_DIAGNOSIS;
            case "content_organizer" -> TaskIntent.CONTENT_ORGANIZER;
            case "code_review" -> TaskIntent.CODE_REVIEW;
            default -> TaskIntent.UNKNOWN;
        };
    }

    /**
     * 根据意图推荐可用Agent
     * @param intent 意图
     * @return 推荐的Agent列表
     */
    private List<String> recommendedAgents(TaskIntent intent) {
        List<String> names = new ArrayList<>();
        switch (intent) {
            case DOCUMENT_SUMMARY -> names.add("文档总结 Agent");
            case TRANSLATION -> {
                names.add("文档翻译 Agent");
                names.add("普通对话模式 + 翻译场景");
            }
            case DATA_ANALYSIS -> names.add("数据分析 Agent");
            case ISSUE_DIAGNOSIS -> names.add("问题诊断 Agent");
            case CONTENT_ORGANIZER -> names.add("内容整理 Agent");
            case CODE_REVIEW -> names.add("代码审查 Agent");
            default -> {
            }
        }
        return names;
    }

    /**
     * 获取意图的显示名称
     * @param intent 意图
     * @return 意图的显示名称
     */
    private String intentDisplayName(TaskIntent intent) {
        return switch (intent) {
            case DOCUMENT_SUMMARY -> "文档总结";
            case DATA_ANALYSIS -> "数据分析";
            case ISSUE_DIAGNOSIS -> "问题诊断";
            case CONTENT_ORGANIZER -> "内容整理";
            case CODE_REVIEW -> "代码审查";
            case TRANSLATION -> "翻译";
            default -> "通用任务";
        };
    }

    /**
     * 检查内容是否包含任意关键字（忽略大小写）
     * @param content 内容
     * @param keywords 关键字列表
     * @return 是否包含任意关键字
     */
    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}