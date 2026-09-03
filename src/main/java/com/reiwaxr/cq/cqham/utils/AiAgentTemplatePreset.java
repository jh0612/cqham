package com.reiwaxr.cq.cqham.utils;

import java.util.List;

/**
 * Agent固定模板预置定义
 */
public final class AiAgentTemplatePreset {

    private AiAgentTemplatePreset() {
    }

    public record TemplateSeed(String templateCode, String templateName, String description, int sortOrder) {}

    public record StepSeed(int stepNo, String skillCode, String stepName, String purpose, String expectedOutput, int sortOrder) {}

    public static List<TemplateSeed> defaultTemplates() {
        return List.of(
                new TemplateSeed("document_summary", "文档总结模板", "用于文档、会议记录、报告等内容的结构化总结", 1),
                new TemplateSeed("document_translation", "文档翻译模板", "用于文档翻译与术语约束校对", 2),
                new TemplateSeed("data_analysis", "数据分析模板", "用于数据结构识别、指标提炼和建议输出", 3),
                new TemplateSeed("issue_diagnosis", "问题诊断模板", "用于问题现象归纳、根因分析与修复建议", 4),
                new TemplateSeed("content_organizer", "内容整理模板", "用于内容拆分、去重、归类和格式化输出", 5),
                new TemplateSeed("code_review", "代码审查模板", "用于代码风险审查与影响评估", 6),
                new TemplateSeed("default", "通用任务模板", "用于未知或通用任务的兜底模板", 99)
        );
    }

    public static List<StepSeed> defaultSteps(String templateCode) {
        return switch (templateCode) {
            case "document_summary" -> List.of(
                    new StepSeed(1, "collect_context", "识别材料范围", "确认材料主题、来源和输出重点。", "明确材料目标和摘要范围。", 1),
                    new StepSeed(2, "extract_topics", "提炼核心主题", "从材料中提取最关键的主题和论点。", "输出 3 到 5 条主题要点。", 2),
                    new StepSeed(3, "extract_conclusions", "归纳关键结论", "总结可直接复用的结论、判断和共识。", "输出结论列表和简短说明。", 3),
                    new StepSeed(4, "extract_actions", "提取行动项", "整理待办事项、负责人和风险提醒。", "输出行动项与风险提示。", 4));
            case "document_translation" -> List.of(
                    new StepSeed(1, "identify_translation_goal", "识别翻译目标", "确认源语言、目标语言、文体和用户的翻译要求。", "输出翻译目标与注意事项。", 1),
                    new StepSeed(2, "extract_translation_constraints", "提炼术语与约束", "识别专有名词、术语、格式要求和敏感表达。", "输出术语与约束列表。", 2),
                    new StepSeed(3, "translate_content", "执行正文翻译", "在保持原意和语气的前提下生成完整译文。", "输出完整译文。", 3),
                    new StepSeed(4, "proofread_translation", "校对译文", "检查译文是否忠实、流畅且符合约束。", "输出校对后的最终版本和必要说明。", 4));
            case "data_analysis" -> List.of(
                    new StepSeed(1, "recognize_structure", "识别数据结构", "确认字段、维度、指标和数据局限。", "输出数据结构说明。", 1),
                    new StepSeed(2, "extract_metrics", "提取关键指标", "挑选能代表当前问题的关键数据。", "输出关键指标与数值。", 2),
                    new StepSeed(3, "identify_patterns", "识别异常和趋势", "分析异常点、趋势和可能原因。", "输出异常与趋势说明。", 3),
                    new StepSeed(4, "generate_recommendations", "形成分析建议", "基于分析结果给出可执行建议。", "输出结论与建议。", 4));
            case "issue_diagnosis" -> List.of(
                    new StepSeed(1, "summarize_symptoms", "归纳问题现象", "整理已知症状、环境和触发条件。", "输出问题现象摘要。", 1),
                    new StepSeed(2, "generate_hypotheses", "列出根因假设", "从高到低列出可能根因。", "输出根因假设及理由。", 2),
                    new StepSeed(3, "prioritize_checks", "确定排查顺序", "给出最小代价、最高收益的排查步骤。", "输出排查优先级。", 3),
                    new StepSeed(4, "repair_plan", "形成修复方案", "输出修复建议和验证方式。", "输出修复建议与验证步骤。", 4));
            case "content_organizer" -> List.of(
                    new StepSeed(1, "split_content", "拆分内容块", "识别原始内容中的主题块和信息段。", "输出内容块列表。", 1),
                    new StepSeed(2, "deduplicate_content", "清理重复噪音", "删除重复、冗余和无关内容。", "输出清理后的关键内容。", 2),
                    new StepSeed(3, "categorize_content", "完成分类整理", "按主题或用途归类内容。", "输出分类结构。", 3),
                    new StepSeed(4, "format_output", "生成目标格式", "将整理结果转成用户可直接使用的结构。", "输出最终整理结果。", 4));
            case "code_review" -> List.of(
                    new StepSeed(1, "identify_context", "识别代码上下文", "确认代码目标、边界和上下文依赖。", "输出代码上下文概述。", 1),
                    new StepSeed(2, "scan_risks", "扫描风险点", "查找真实缺陷、边界问题和回归风险。", "输出风险点清单。", 2),
                    new StepSeed(3, "assess_impact", "评估影响范围", "判断问题严重度和影响面。", "输出严重度与影响说明。", 3),
                    new StepSeed(4, "review_summary", "形成审查结论", "整理问题、建议和测试缺口。", "输出审查结论。", 4));
            default -> defaultGenericSteps();
        };
    }

    public static List<StepSeed> defaultGenericSteps() {
        return List.of(
                new StepSeed(1, "analyze_goal", "分析目标", "澄清用户任务与边界。", "输出目标摘要。", 1),
                new StepSeed(2, "execute_task", "执行任务", "基于上下文执行核心分析。", "输出执行结果。", 2),
                new StepSeed(3, "summarize_result", "汇总结论", "整理结果与风险。", "输出最终结论。", 3));
    }
}
