package com.reiwaxr.cq.cqham.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reiwaxr.cq.cqham.dao.AiAgentSkillBindingDao;
import com.reiwaxr.cq.cqham.dao.AiAgentTemplateDao;
import com.reiwaxr.cq.cqham.dao.AiAgentRunLogDao;
import com.reiwaxr.cq.cqham.dao.AiSkillDefinitionDao;
import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.entity.AiAgentRunRecord;
import com.reiwaxr.cq.cqham.entity.AiAgentSkillBinding;
import com.reiwaxr.cq.cqham.entity.AiAgentStepLog;
import com.reiwaxr.cq.cqham.entity.AiAgentTemplateStep;
import com.reiwaxr.cq.cqham.entity.AiSkillDefinition;
import com.reiwaxr.cq.cqham.utils.AiAgentTemplatePreset;
import com.reiwaxr.cq.cqham.utils.AiDebugJsonLogger;
import okhttp3.Call;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Agent実行サービス
 */
public class AiAgentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AiDebugJsonLogger AI_DEBUG_LOGGER = new AiDebugJsonLogger();
        private static final int MAX_PLANNER_USER_INPUT_CHARS = 2600;
        private static final int MAX_PLANNER_FILE_CONTEXT_CHARS = 700;
        private static final int MAX_PLANNER_HISTORY_CHARS = 600;
    private static final int MAX_FILE_CONTEXT_CHARS = 2200;
    private static final int MAX_HISTORY_CONTEXT_CHARS = 1400;
    private static final int MAX_STEP_INPUT_CHARS = 3200;
        private static final String PLANNER_SYSTEM_PROMPT = """
                        你是一个任务规划器，只负责输出执行计划 JSON。
                        必须遵守以下规则：
                        1. 只输出一个 JSON 对象，不要输出 Markdown、解释、前言或代码块标记。
                        2. JSON 结构必须是：
                             {
                                 "goalSummary": "...",
                                 "planSummary": "...",
                                 "riskNotes": ["...", "..."]
                             }
                        3. planSummary 需要按给定固定步骤模板进行总结，不要改写步骤编号和步骤名称。
                        4. riskNotes 至少给出 2 条，最多给出 6 条，使用简洁完整句。
                        5. 若信息不足，也必须返回上述 JSON 结构，不得输出额外说明。
                        """;

    private final AiAgentRunLogDao runLogDao = new AiAgentRunLogDao();
    private final AiAgentSkillBindingDao bindingDao = new AiAgentSkillBindingDao();
    private final AiAgentTemplateDao templateDao = new AiAgentTemplateDao();
    private final AiSkillDefinitionDao skillDefinitionDao = new AiSkillDefinitionDao();

    public record AgentExecutionRequest(
            AiAgentDefinition agentDefinition,
            String apiKey,
            String model,
            String userInput,
            String sceneName,
            String subCategory,
            String scenePrompt,
            String fileContext,
            String historySummary,
            boolean confirmBeforeRun,
            boolean useFileContext,
            boolean useChatHistory,
            String strategy,
            int maxSteps) {}

    public record PreparedAgentRun(
            String runId,
            AgentPlan plan,
            AgentExecutionRequest request,
            int completedSteps,
            List<AgentStepResult> previousStepResults,
            boolean resumed,
            String sourceRunId) {}

    public record AgentPlan(
            String agentCode,
            String agentName,
            String goalSummary,
            String planSummary,
            List<AgentPlanStep> steps,
            List<String> riskNotes,
            boolean requireConfirmation) {}

    /**
     * AgentPlanStep
     * @param stepNo 步骤编号
     * @param skillCode 技能代码
     * @param stepName 步骤名称
     * @param purpose 目的
     * @param expectedOutput 预期输出
     * @param boundSkills 绑定的技能列表
     */
    public record AgentPlanStep(
            int stepNo,
            String skillCode,
            String stepName,
            String purpose,
            String expectedOutput,
            List<AgentBoundSkill> boundSkills) {}

    public record AgentBoundSkill(
            String skillCode,
            String skillName,
            String systemPrompt,
            String inputTemplate,
            String outputTemplate,
            String exampleInput,
            String exampleOutput) {}

    public record SkillExecutionSummary(String skillCode, String skillName, String output) {}

    public record AgentStepResult(int stepNo, String stepName, String output, List<SkillExecutionSummary> skillSummaries) {}

    public record AgentRunResult(String runId, AgentPlan plan, List<AgentStepResult> stepResults, String finalResponse) {}

    public record StepExecutionDetail(String output, List<SkillExecutionSummary> skillSummaries) {}

    private record AgentDefinitionSnapshot(
            String agentCode,
            String agentName,
            String description,
            String plannerPrompt,
            String executionPrompt,
            String templateCode,
            boolean finalSummaryEnabled) {}

    private record AgentRequestSnapshot(
            AgentDefinitionSnapshot agentDefinition,
            String apiKey,
            String model,
            String userInput,
            String sceneName,
            String subCategory,
            String scenePrompt,
            String fileContext,
            String historySummary,
            boolean confirmBeforeRun,
            boolean useFileContext,
            boolean useChatHistory,
            String strategy,
            int maxSteps) {}

    public interface AgentProgressListener {
        void onStatusChanged(String status);

        void onStepStarted(AgentPlanStep step);

        void onStepCompleted(AgentPlanStep step, AgentStepResult result);
    }

    private record PlannerResult(String goalSummary, String planSummary, List<String> riskNotes) {}

    /**
     * Agent计划生成与执行的入口方法
     * @param apiService QwenApiService 实例
     * @param request AgentExecutionRequest 实例
     * @param callRegistrar Call 注册器
     * @param cancelSupplier 取消供应器
     * @return PreparedAgentRun 实例
     */
    public PreparedAgentRun prepareRun(
            QwenApiService apiService,
            AgentExecutionRequest request,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier) throws Exception {
        // 确认操作未被取消        
        ensureNotCancelled(cancelSupplier);
        // 构建计划步骤
        List<AgentPlanStep> steps = buildTemplateSteps(
            resolveTemplateCode(request.agentDefinition()),
            request.agentDefinition().getId(),
                request.strategy(),
                request.maxSteps());
        PlannerResult plannerResult = buildPlannerResult(apiService, request, steps, callRegistrar, cancelSupplier);

        AgentPlan plan = new AgentPlan(
                request.agentDefinition().getAgentCode(),
                request.agentDefinition().getAgentName(),
                plannerResult.goalSummary(),
                plannerResult.planSummary(),
                steps,
                plannerResult.riskNotes(),
                request.confirmBeforeRun());

        String runId = UUID.randomUUID().toString();
        runLogDao.insertRunLog(
                runId,
                request.agentDefinition().getAgentCode(),
                request.userInput(),
                buildContextSummary(request),
                writePlanJson(plan),
                writeRequestJson(request),
                request.confirmBeforeRun() ? "WAITING_CONFIRMATION" : "PLANNED",
                0,
                0,
                null,
                null);

        return new PreparedAgentRun(runId, plan, request, 0, new ArrayList<>(), false, null);
    }

    /**
     * 从已保存的运行记录恢复执行上下文
     * @param sourceRecord 已保存的运行记录
     * @param request 当前的代理执行请求
     * @param deriveNewRun 是否派生新的运行实例
     * @return PreparedAgentRun 实例
     * @throws Exception 异常
     */
    public PreparedAgentRun restorePreparedRun(AiAgentRunRecord sourceRecord, AgentExecutionRequest request, boolean deriveNewRun) throws Exception {
        AgentPlan plan = resolvePlanForResume(sourceRecord, request, deriveNewRun);
        List<AgentStepResult> previousResults = loadCompletedStepResults(sourceRecord.getRunId());
        int completedSteps = Math.min(previousResults.size(), plan.steps().size());
        if (previousResults.size() > completedSteps) {
            previousResults = new ArrayList<>(previousResults.subList(0, completedSteps));
        }
        String requestJson = writeRequestJson(request);

        if (deriveNewRun) {
            String newRunId = UUID.randomUUID().toString();
            runLogDao.insertRunLog(
                    newRunId,
                    request.agentDefinition().getAgentCode(),
                    request.userInput(),
                    buildContextSummary(request),
                    writePlanJson(plan),
                    requestJson,
                    request.confirmBeforeRun() ? "WAITING_CONFIRMATION" : "PLANNED",
                    completedSteps,
                    completedSteps,
                    sourceRecord.getRunId(),
                    "DERIVED");
            return new PreparedAgentRun(newRunId, plan, request, completedSteps, previousResults, true, sourceRecord.getRunId());
        }

        runLogDao.updateRunLog(
                sourceRecord.getRunId(),
                request.confirmBeforeRun() ? "WAITING_CONFIRMATION" : "PLANNED",
                null,
                null,
                writePlanJson(plan),
                requestJson,
                completedSteps,
                completedSteps);
        return new PreparedAgentRun(sourceRecord.getRunId(), plan, request, completedSteps, previousResults, true, sourceRecord.getRunId());
    }

    /**
     * 读取保存的请求快照
     * @param sourceRecord 已保存的运行记录
     * @return AgentExecutionRequest 实例
     * @throws IOException 异常
     */
    public AgentExecutionRequest restoreRequestFromRun(AiAgentRunRecord sourceRecord) throws IOException {
        if (sourceRecord.getRequestJson() == null || sourceRecord.getRequestJson().isBlank()) {
            AiAgentDefinition definition = new AiAgentDefinition();
            definition.setAgentCode(sourceRecord.getAgentCode());
            definition.setAgentName(sourceRecord.getAgentCode());
            definition.setPlannerPrompt("");
            definition.setExecutionPrompt("");
            definition.setTemplateCode(sourceRecord.getAgentCode());
            definition.setFinalSummaryEnabled(true);
            definition.setEnabled(true);
            return new AgentExecutionRequest(
                    definition,
                    "",
                    "",
                    defaultText(sourceRecord.getUserGoal()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    false,
                    false,
                    "标准",
                    4);
        }
        AgentRequestSnapshot snapshot = OBJECT_MAPPER.readValue(sourceRecord.getRequestJson(), AgentRequestSnapshot.class);
        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setAgentCode(snapshot.agentDefinition().agentCode());
        definition.setAgentName(snapshot.agentDefinition().agentName());
        definition.setDescription(snapshot.agentDefinition().description());
        definition.setPlannerPrompt(snapshot.agentDefinition().plannerPrompt());
        definition.setExecutionPrompt(snapshot.agentDefinition().executionPrompt());
        definition.setTemplateCode(snapshot.agentDefinition().templateCode());
        definition.setFinalSummaryEnabled(snapshot.agentDefinition().finalSummaryEnabled());
        definition.setEnabled(true);
        return new AgentExecutionRequest(
                definition,
                snapshot.apiKey(),
                snapshot.model(),
                snapshot.userInput(),
                snapshot.sceneName(),
                snapshot.subCategory(),
                snapshot.scenePrompt(),
                snapshot.fileContext(),
                snapshot.historySummary(),
                snapshot.confirmBeforeRun(),
                snapshot.useFileContext(),
                snapshot.useChatHistory(),
                snapshot.strategy(),
                snapshot.maxSteps());
    }

    /**
     * 执行已准备好的Agent计划
     * @param apiService QwenApiService 实例
     * @param request AgentExecutionRequest 实例
     * @param preparedRun PreparedAgentRun 实例
     * @param callRegistrar Call 注册器
     * @param cancelSupplier 取消供应器
     * @param listener AgentProgressListener 实例
     * @return AgentRunResult 实例
     */
    public AgentRunResult executePreparedRun(
            QwenApiService apiService,
            AgentExecutionRequest request,
            PreparedAgentRun preparedRun,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier,
            AgentProgressListener listener) throws Exception {
        List<AgentStepResult> stepResults = new ArrayList<>(preparedRun.previousStepResults());
        int[] progressState = {preparedRun.completedSteps(), preparedRun.completedSteps()};
        try {
            ensureNotCancelled(cancelSupplier);
            runLogDao.updateRunLog(preparedRun.runId(), "RUNNING", null, null, writePlanJson(preparedRun.plan()), writeRequestJson(request), progressState[0], progressState[1]);
            listener.onStatusChanged("Agent 正在执行计划");

            for (AgentPlanStep step : preparedRun.plan().steps()) {
                if (step.stepNo() <= progressState[0]) {
                    continue;
                }
                ensureNotCancelled(cancelSupplier);
                progressState[1] = step.stepNo();
                runLogDao.updateRunLog(preparedRun.runId(), "RUNNING", null, null, writePlanJson(preparedRun.plan()), writeRequestJson(request), stepResults.size(), progressState[1]);
                listener.onStepStarted(step);

                String stepInput = buildStepInput(request, preparedRun.plan(), step, stepResults);
                long stepLogId = runLogDao.insertStepLog(
                        preparedRun.runId(),
                        step.stepNo(),
                        step.stepName(),
                        step.skillCode(),
                        abbreviate(stepInput, MAX_STEP_INPUT_CHARS),
                        "RUNNING");

                try {
                    StepExecutionDetail executionDetail = executeStep(apiService, request, preparedRun.plan(), step, stepResults, stepInput, callRegistrar, cancelSupplier);
                    String output = executionDetail.output();

                    AgentStepResult stepResult = new AgentStepResult(step.stepNo(), step.stepName(), output, executionDetail.skillSummaries());
                    stepResults.add(stepResult);
                    progressState[0] = stepResults.size();
                    progressState[1] = step.stepNo();
                    runLogDao.updateStepLog(stepLogId, "COMPLETED", output, null);
                    runLogDao.updateRunLog(preparedRun.runId(), "RUNNING", null, null, writePlanJson(preparedRun.plan()), writeRequestJson(request), progressState[0], progressState[1]);
                    listener.onStepCompleted(step, stepResult);
                } catch (Exception ex) {
                    runLogDao.updateStepLog(stepLogId, "FAILED", null, ex.getMessage());
                    throw ex;
                }
            }

            String finalResponse;
            if (request.agentDefinition().isFinalSummaryEnabled()) {
                ensureNotCancelled(cancelSupplier);
                listener.onStatusChanged("Agent 正在汇总最终结论");
                finalResponse = buildFinalResponse(apiService, request, preparedRun.plan(), stepResults, callRegistrar, cancelSupplier);
            } else {
                listener.onStatusChanged("Agent 已完成最后步骤，跳过最终汇总");
                finalResponse = stepResults.isEmpty() ? "" : stepResults.getLast().output();
            }
            runLogDao.updateRunLog(preparedRun.runId(), "COMPLETED", finalResponse, null, writePlanJson(preparedRun.plan()), writeRequestJson(request), stepResults.size(), preparedRun.plan().steps().size());
            return new AgentRunResult(preparedRun.runId(), preparedRun.plan(), stepResults, finalResponse);
        } catch (CancellationException ex) {
            runLogDao.updateRunLog(preparedRun.runId(), "CANCELLED", null, ex.getMessage(), writePlanJson(preparedRun.plan()), writeRequestJson(request), progressState[0], progressState[1]);
            throw ex;
        } catch (Exception ex) {
            int currentStepNo = Math.min(preparedRun.plan().steps().size(), Math.max(progressState[1], progressState[0] + 1));
            runLogDao.updateRunLog(preparedRun.runId(), "FAILED", null, ex.getMessage(), writePlanJson(preparedRun.plan()), writeRequestJson(request), progressState[0], currentStepNo);
            throw ex;
        } finally {
            callRegistrar.accept(null);
        }
    }

    /**
     * 确保操作未被取消
     * @param preparedRun
     */
    public void markRunCancelled(PreparedAgentRun preparedRun) {
        if (preparedRun == null || preparedRun.runId() == null || preparedRun.runId().isBlank()) {
            return;
        }
        try {
            runLogDao.updateRunLog(
                    preparedRun.runId(),
                    "CANCELLED",
                    null,
                    "用户取消了待执行计划",
                    writePlanJson(preparedRun.plan()),
                    writeRequestJson(preparedRun.request()),
                    preparedRun.completedSteps(),
                    preparedRun.completedSteps());
        } catch (Exception ignored) {
        }
    }

    /**
     * 确认是否已取消
     * @param apiService QwenApiService 实例
     * @param request AgentExecutionRequest 实例
     * @param steps 计划步骤列表
     * @param callRegistrar Call 注册器
     * @param cancelSupplier 取消供应器
     * @return PlannerResult 实例
     */
    private PlannerResult buildPlannerResult(
            QwenApiService apiService,
            AgentExecutionRequest request,
            List<AgentPlanStep> steps,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier) {
        String systemPrompt = buildPlannerSystemPrompt(request);
        String userPrompt = buildPlannerUserPrompt(request, steps);
        try {
            String response = invokeChat(
                    apiService,
                    request.apiKey(),
                    request.model(),
                systemPrompt,
                    List.of(new QwenApiService.ChatMessage("user", userPrompt)),
                    callRegistrar,
                    cancelSupplier);
            return parsePlannerResponse(response, request, steps.size());
        } catch (Exception ex) {
            return new PlannerResult(
                    abbreviate(request.userInput(), 120),
                    "规划阶段已回退到内置模板，系统将按固定步骤执行该任务。",
                    List.of("模型规划输出解析失败，已使用内置模板继续执行。", "如结果不够精确，可缩小任务范围后重试。"));
        }
    }

    /**
     * 对文本进行折叠处理，保留开头和结尾部分，中间内容用提示替代
     * @param response JSON 根节点
     * @param request AgentExecutionRequest 实例
     * @param stepCount 最大步骤数
     * @return PlannerResult 实例
     */
    private PlannerResult parsePlannerResponse(String response, AgentExecutionRequest request, int stepCount) throws IOException {
        JsonNode root = resolvePlannerResponseJson(response);
        String goalSummary = readText(root, "goalSummary", abbreviate(request.userInput(), 120));
        String planSummary = readText(root, "planSummary", "系统将按固定步骤模板执行，共 " + stepCount + " 步。");

        List<String> riskNotes = new ArrayList<>();
        JsonNode riskNode = root.get("riskNotes");
        if (riskNode != null && riskNode.isArray()) {
            for (JsonNode node : riskNode) {
                String risk = node.asText().trim();
                if (!risk.isBlank()) {
                    riskNotes.add(risk);
                }
            }
        } else if (riskNode != null && riskNode.isTextual()) {
            String risk = riskNode.asText().trim();
            if (!risk.isBlank()) {
                riskNotes.add(risk);
            }
        }
        if (riskNotes.isEmpty()) {
            riskNotes.add("当前规划未发现明显高风险，但仍需关注上下文是否完整。");
        }
        return new PlannerResult(goalSummary, planSummary, riskNotes);
    }

    /**
     * 构建上下文摘要文本
     * @param templateCode 模板代码
     * @param agentId Agent ID
     * @param strategy 执行策略
     * @param maxSteps 最大步骤数
     * @return 模板步骤列表
     */
    private List<AgentPlanStep> buildTemplateSteps(String templateCode, Integer agentId, String strategy, int maxSteps) throws SQLException {
        List<AgentPlanStep> templateSteps = loadTemplateSteps(templateCode);

        int limit = Math.min(templateSteps.size(), adjustStepCount(strategy, templateSteps.size(), maxSteps));
        List<AgentPlanStep> limitedSteps = new ArrayList<>(templateSteps.subList(0, limit));
        return applyBindings(agentId, limitedSteps);
    }

    /**
     * 从数据库读取模板步骤，缺失时回退到预置模板
     */
    private List<AgentPlanStep> loadTemplateSteps(String templateCode) throws SQLException {
        String resolvedCode = (templateCode == null || templateCode.isBlank()) ? "default" : templateCode;
        List<AiAgentTemplateStep> dbSteps = templateDao.findEnabledStepsByTemplateCode(resolvedCode);

        List<AgentPlanStep> steps = new ArrayList<>();
        if (!dbSteps.isEmpty()) {
            for (AiAgentTemplateStep step : dbSteps) {
                String skillCode = step.getSkillCode() == null || step.getSkillCode().isBlank()
                        ? "step_" + step.getStepNo()
                        : step.getSkillCode();
                steps.add(new AgentPlanStep(
                        step.getStepNo(),
                        skillCode,
                        defaultText(step.getStepName()),
                        defaultText(step.getPurpose()),
                        defaultText(step.getExpectedOutput()),
                        List.of()));
            }
            return steps;
        }

        for (AiAgentTemplatePreset.StepSeed seed : AiAgentTemplatePreset.defaultSteps(resolvedCode)) {
            steps.add(new AgentPlanStep(
                    seed.stepNo(),
                    seed.skillCode(),
                    seed.stepName(),
                    seed.purpose(),
                    seed.expectedOutput(),
                    List.of()));
        }
        return steps;
    }

    /**
     * 根据策略和模板大小调整实际执行步骤数
     * @param strategy 执行策略
     * @param templateSize 模板步骤数
     * @param maxSteps 最大允许步骤数
     */
    private int adjustStepCount(String strategy, int templateSize, int maxSteps) {
        int limited = Math.min(templateSize, Math.max(1, maxSteps));
        if ("保守".equals(strategy)) {
            return Math.max(2, Math.min(limited, templateSize - 1));
        }
        if ("深入".equals(strategy)) {
            return limited;
        }
        return Math.min(limited, Math.max(3, limited));
    }

    /**
     * 调用聊天接口
     * @param request
     * @param steps
     */
    private String buildPlannerUserPrompt(AgentExecutionRequest request, List<AgentPlanStep> steps) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户任务：\n")
                .append(abbreviate(defaultText(request.userInput()), MAX_PLANNER_USER_INPUT_CHARS))
                .append("\n\n");
        if (request.sceneName() != null && !request.sceneName().isBlank()) {
            builder.append("当前场景：").append(request.sceneName());
            if (request.subCategory() != null && !request.subCategory().isBlank()) {
                builder.append(" / ").append(request.subCategory());
            }
            builder.append("\n\n");
        }
        if (request.useFileContext() && request.fileContext() != null && !request.fileContext().isBlank()) {
            builder.append("文件上下文摘要：\n")
                    .append(abbreviate(request.fileContext(), MAX_PLANNER_FILE_CONTEXT_CHARS))
                    .append("\n\n");
        }
        if (request.useChatHistory() && request.historySummary() != null && !request.historySummary().isBlank()) {
            builder.append("历史对话摘要：\n")
                    .append(abbreviate(request.historySummary(), MAX_PLANNER_HISTORY_CHARS))
                    .append("\n\n");
        }
        builder.append("固定执行步骤模板：\n");
        for (AgentPlanStep step : steps) {
            builder.append(step.stepNo())
                    .append(". ")
                    .append(step.stepName())
                    .append(" - ")
                    .append(step.purpose());
            if (!step.boundSkills().isEmpty()) {
                builder.append("（绑定 Skill: ");
                for (int i = 0; i < step.boundSkills().size(); i++) {
                    if (i > 0) {
                        builder.append(" -> ");
                    }
                    builder.append(step.boundSkills().get(i).skillName());
                }
                builder.append("）");
            }
            builder
                    .append("\n");
        }
        builder.append("\n请总结目标、执行重点与风险，不要改写固定步骤模板。\n");
        return builder.toString();
    }

    /**
     * Planner 専用のシステムプロンプトを返す
     */
    private String buildPlannerSystemPrompt(AgentExecutionRequest request) {
        String customPrompt = "";
        if (request != null && request.agentDefinition() != null) {
            customPrompt = defaultText(request.agentDefinition().getPlannerPrompt());
        }
        if (customPrompt.isBlank()) {
            return PLANNER_SYSTEM_PROMPT;
        }
        return PLANNER_SYSTEM_PROMPT
                + "\n\n附加规划约束（仅作为背景，不得覆盖上面的 JSON 输出规则）：\n"
                + abbreviate(customPrompt, 1400);
    }

    /**
     * 构建上下文摘要
     * @param request AgentExecutionRequest 实例
     * @param plan AgentPlan 实例
     * @param step 当前步骤
     * @param previousStepResults 已完成的步骤结果列表
     * @return 上下文摘要文本
     */
    private String buildStepInput(
            AgentExecutionRequest request,
            AgentPlan plan,
            AgentPlanStep step,
            List<AgentStepResult> previousStepResults) {
        StringBuilder builder = new StringBuilder();
        builder.append("你正在执行一个多步 Agent 任务中的单个固定步骤。\n");
        builder.append("Agent 类型：").append(plan.agentName()).append("\n");
        builder.append("用户原始任务：\n").append(request.userInput()).append("\n\n");
        builder.append("当前步骤：第 ").append(step.stepNo()).append(" 步 - ").append(step.stepName()).append("\n");
        builder.append("步骤目标：").append(step.purpose()).append("\n");
        builder.append("期望输出：").append(step.expectedOutput()).append("\n\n");
        if (!step.boundSkills().isEmpty()) {
            builder.append("当前步骤绑定的 Skill 顺序：\n");
            for (int i = 0; i < step.boundSkills().size(); i++) {
                AgentBoundSkill skill = step.boundSkills().get(i);
                builder.append(i + 1).append(". ").append(skill.skillName()).append("\n");
            }
            builder.append("\n");
        }

        if (request.scenePrompt() != null && !request.scenePrompt().isBlank()) {
            builder.append("场景补充提示：\n")
                    .append(abbreviate(request.scenePrompt(), 1000))
                    .append("\n\n");
        }
        if (request.useFileContext() && request.fileContext() != null && !request.fileContext().isBlank()) {
            builder.append("可用文件上下文：\n")
                    .append(abbreviate(request.fileContext(), MAX_FILE_CONTEXT_CHARS))
                    .append("\n\n");
        }
        if (request.useChatHistory() && request.historySummary() != null && !request.historySummary().isBlank()) {
            builder.append("可用历史对话摘要：\n")
                    .append(abbreviate(request.historySummary(), MAX_HISTORY_CONTEXT_CHARS))
                    .append("\n\n");
        }
        if (!previousStepResults.isEmpty()) {
            builder.append("已完成步骤输出：\n");
            for (AgentStepResult result : previousStepResults) {
                builder.append(result.stepNo())
                        .append(". ")
                        .append(result.stepName())
                        .append("\n")
                        .append(abbreviate(result.output(), 1200))
                        .append("\n\n");
            }
        }
        builder.append("请只完成当前步骤，并直接输出该步骤结果。\n");
        return builder.toString();
    }

    /**
     * 构建最终整合回答
     * @param apiService QwenApiService 实例
     * @param request AgentExecutionRequest 实例
     * @param plan AgentPlan 实例
     * @param stepResults 已完成的步骤结果列表
     * @param callRegistrar Call 注册器
     * @param cancelSupplier 取消供应器
     * @return 最终整合回答
     * @throws Exception 异常
     */
    private String buildFinalResponse(
            QwenApiService apiService,
            AgentExecutionRequest request,
            AgentPlan plan,
            List<AgentStepResult> stepResults,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于以下 Agent 步骤结果，生成面向最终用户的整合回答。\n");
        builder.append("要求：\n");
        builder.append("1. 先给最终结论，再给必要说明。\n");
        builder.append("2. 明确哪些结论来自材料，哪些属于推断。\n");
        builder.append("3. 若上下文不足，要直接指出。\n\n");
        builder.append("用户原始任务：\n").append(request.userInput()).append("\n\n");
        builder.append("Agent 计划摘要：\n").append(plan.planSummary()).append("\n\n");
        builder.append("步骤结果：\n");
        for (AgentStepResult result : stepResults) {
            builder.append(result.stepNo())
                    .append(". ")
                    .append(result.stepName())
                    .append("\n")
                    .append(result.output())
                    .append("\n\n");
        }
        return invokeChat(
                apiService,
                request.apiKey(),
                request.model(),
                request.agentDefinition().getExecutionPrompt(),
                List.of(new QwenApiService.ChatMessage("user", builder.toString())),
                callRegistrar,
                cancelSupplier);
    }

    /**
     * 执行单个步骤，必要时串行执行绑定Skill后再汇总
     */
    private StepExecutionDetail executeStep(
            QwenApiService apiService,
            AgentExecutionRequest request,
            AgentPlan plan,
            AgentPlanStep step,
            List<AgentStepResult> previousStepResults,
            String stepInput,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier) throws Exception {
        if (step.boundSkills().isEmpty()) {
            return new StepExecutionDetail(invokeChat(
                    apiService,
                    request.apiKey(),
                    request.model(),
                    request.agentDefinition().getExecutionPrompt(),
                    List.of(new QwenApiService.ChatMessage("user", stepInput)),
                    callRegistrar,
                    cancelSupplier), List.of());
        }

        List<String> skillOutputs = new ArrayList<>();
        List<SkillExecutionSummary> skillSummaries = new ArrayList<>();
        for (AgentBoundSkill skill : step.boundSkills()) {
            ensureNotCancelled(cancelSupplier);
            String skillInput = buildSkillInput(request, plan, step, previousStepResults, skillOutputs, skill);
            String skillOutput = invokeChat(
                    apiService,
                    request.apiKey(),
                    request.model(),
                    resolveSkillSystemPrompt(skill, request.agentDefinition().getExecutionPrompt()),
                    List.of(new QwenApiService.ChatMessage("user", skillInput)),
                    callRegistrar,
                    cancelSupplier);
            skillOutputs.add(skill.skillName() + "\n" + skillOutput);
            skillSummaries.add(new SkillExecutionSummary(skill.skillCode(), skill.skillName(), skillOutput));
        }

        String consolidatedInput = buildStepConsolidationInput(stepInput, step, skillOutputs);
        return new StepExecutionDetail(invokeChat(
                apiService,
                request.apiKey(),
                request.model(),
                request.agentDefinition().getExecutionPrompt(),
                List.of(new QwenApiService.ChatMessage("user", consolidatedInput)),
                callRegistrar,
                cancelSupplier), skillSummaries);
    }

    /**
     * 构建Skill输入
     */
    private String buildSkillInput(
            AgentExecutionRequest request,
            AgentPlan plan,
            AgentPlanStep step,
            List<AgentStepResult> previousStepResults,
            List<String> previousSkillOutputs,
            AgentBoundSkill skill) {
        String baseTemplate = skill.inputTemplate();
        if (baseTemplate == null || baseTemplate.isBlank()) {
            baseTemplate = "请围绕当前 Agent 步骤执行该 Skill。\n用户任务：{{userInput}}\n当前步骤：{{stepName}}\n步骤目标：{{stepPurpose}}\n期望输出：{{expectedOutput}}\n\n已完成步骤结果：\n{{previousStepResults}}\n\n已执行 Skill 输出：\n{{previousSkillOutputs}}\n\n文件上下文：\n{{fileContext}}\n\n历史摘要：\n{{historySummary}}\n";
        }
        Map<String, String> values = new HashMap<>();
        values.put("userInput", defaultText(request.userInput()));
        values.put("agentName", defaultText(plan.agentName()));
        values.put("stepName", defaultText(step.stepName()));
        values.put("stepPurpose", defaultText(step.purpose()));
        values.put("expectedOutput", defaultText(step.expectedOutput()));
        values.put("scenePrompt", defaultText(request.scenePrompt()));
        values.put("fileContext", request.useFileContext() ? defaultText(abbreviate(request.fileContext(), MAX_FILE_CONTEXT_CHARS)) : "");
        values.put("historySummary", request.useChatHistory() ? defaultText(abbreviate(request.historySummary(), MAX_HISTORY_CONTEXT_CHARS)) : "");
        values.put("previousStepResults", joinStepResults(previousStepResults));
        values.put("previousSkillOutputs", previousSkillOutputs.isEmpty() ? "" : String.join("\n\n", previousSkillOutputs));

        StringBuilder builder = new StringBuilder(applyTemplate(baseTemplate, values));
        if (skill.outputTemplate() != null && !skill.outputTemplate().isBlank()) {
            builder.append("\n\n输出模板要求：\n").append(skill.outputTemplate().trim());
        }
        if (skill.exampleInput() != null && !skill.exampleInput().isBlank()) {
            builder.append("\n\n示例输入：\n").append(skill.exampleInput().trim());
        }
        if (skill.exampleOutput() != null && !skill.exampleOutput().isBlank()) {
            builder.append("\n\n示例输出：\n").append(skill.exampleOutput().trim());
        }
        return builder.toString().trim();
    }

    /**
     * 构建步骤汇总输入
     */
    private String buildStepConsolidationInput(String stepInput, AgentPlanStep step, List<String> skillOutputs) {
        StringBuilder builder = new StringBuilder();
        builder.append(stepInput).append("\n\n");
        builder.append("以下是按顺序执行的 Skill 输出，请基于这些结果完成当前固定步骤：\n\n");
        for (int i = 0; i < skillOutputs.size(); i++) {
            builder.append("[Skill ").append(i + 1).append("]\n")
                    .append(skillOutputs.get(i))
                    .append("\n\n");
        }
        builder.append("请输出当前步骤的最终结果，不要重复解释内部 Skill 流程。\n");
        if (!step.boundSkills().isEmpty()) {
            builder.append("当前步骤绑定 Skill：");
            for (int i = 0; i < step.boundSkills().size(); i++) {
                if (i > 0) {
                    builder.append(" -> ");
                }
                builder.append(step.boundSkills().get(i).skillName());
            }
        }
        return builder.toString().trim();
    }

    /**
     * 为模板步骤附加已绑定Skill
     */
    private List<AgentPlanStep> applyBindings(Integer agentId, List<AgentPlanStep> steps) throws SQLException {
        if (agentId == null || steps.isEmpty()) {
            return steps;
        }
        List<AiAgentSkillBinding> bindings = bindingDao.findBindingsByAgentId(agentId);
        if (bindings.isEmpty()) {
            return steps;
        }

        Map<Integer, List<AgentBoundSkill>> groupedSkills = new HashMap<>();
        Map<Integer, AiSkillDefinition> skillCache = new HashMap<>();
        for (AiAgentSkillBinding binding : bindings) {
            AiSkillDefinition skill = skillCache.computeIfAbsent(binding.getSkillId(), skillId -> {
                try {
                    return skillDefinitionDao.findById(skillId);
                } catch (SQLException ex) {
                    return null;
                }
            });
            if (skill == null) {
                continue;
            }
            groupedSkills.computeIfAbsent(binding.getTemplateStepNo(), key -> new ArrayList<>())
                    .add(new AgentBoundSkill(
                            skill.getSkillCode(),
                            skill.getSkillName(),
                            skill.getSystemPrompt(),
                            skill.getInputTemplate(),
                            skill.getOutputTemplate(),
                            skill.getExampleInput(),
                            skill.getExampleOutput()));
        }

        List<AgentPlanStep> appliedSteps = new ArrayList<>();
        for (AgentPlanStep step : steps) {
            List<AgentBoundSkill> boundSkills = groupedSkills.getOrDefault(step.stepNo(), List.of());
            appliedSteps.add(new AgentPlanStep(
                    step.stepNo(),
                    step.skillCode(),
                    step.stepName(),
                    step.purpose(),
                    step.expectedOutput(),
                    new ArrayList<>(boundSkills)));
        }
        return appliedSteps;
    }

    /**
     * 调用 QwenApiService 执行聊天请求
     * @param apiService QwenApiService 实例
     * @param apiKey API Key
     * @param model 模型名称
     * @param systemPrompt 系统提示
     * @param messages 聊天消息列表
     * @param callRegistrar Call 注册器
     * @param cancelSupplier 取消供应器
     * @return 聊天响应文本
     * @throws Exception 异常
     */
    private String invokeChat(
            QwenApiService apiService,
            String apiKey,
            String model,
            String systemPrompt,
            List<QwenApiService.ChatMessage> messages,
            Consumer<Call> callRegistrar,
            BooleanSupplier cancelSupplier) throws Exception {
        ensureNotCancelled(cancelSupplier);
        AI_DEBUG_LOGGER.logPayload("ai.agent.request", java.util.Map.of(
                "apiKey", apiKey,
                "model", model,
                "systemPrompt", systemPrompt,
                "messages", messages
        ));
        Call call = apiService.createChatCall(apiKey, model, systemPrompt, messages);
        callRegistrar.accept(call);
        try {
            String response = apiService.executeChat(call);
            AI_DEBUG_LOGGER.logPayload("ai.agent.response", java.util.Map.of(
                    "apiKey", apiKey,
                    "model", model,
                    "response", response
            ));
            return response;
        } finally {
            callRegistrar.accept(null);
        }
    }

    /**
     * 检查是否已取消执行
     * @param cancelSupplier 取消供应器
     */
    private void ensureNotCancelled(BooleanSupplier cancelSupplier) {
        if (cancelSupplier.getAsBoolean()) {
            throw new CancellationException("Agent execution canceled");
        }
    }

    /**
     * 构建上下文摘要
     * @param request AgentExecutionRequest 实例
     * @return 上下文摘要文本
     */
    private String buildContextSummary(AgentExecutionRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.sceneName() != null && !request.sceneName().isBlank()) {
            builder.append("场景：").append(request.sceneName());
            if (request.subCategory() != null && !request.subCategory().isBlank()) {
                builder.append("/").append(request.subCategory());
            }
            builder.append("\n");
        }
        if (request.useFileContext() && request.fileContext() != null && !request.fileContext().isBlank()) {
            builder.append("文件上下文已启用\n");
        }
        if (request.useChatHistory() && request.historySummary() != null && !request.historySummary().isBlank()) {
            builder.append("历史对话已启用\n");
        }
        builder.append("策略：").append(request.strategy()).append("，最大步骤：").append(request.maxSteps());
        return builder.toString().trim();
    }

    /**
     * 读取Agent计划JSON
     */
    private AgentPlan readPlanJson(String planJson) throws IOException {
        AgentPlan plan = OBJECT_MAPPER.readValue(planJson, AgentPlan.class);
        List<AgentPlanStep> normalizedSteps = new ArrayList<>();
        for (AgentPlanStep step : plan.steps()) {
            normalizedSteps.add(new AgentPlanStep(
                    step.stepNo(),
                    step.skillCode(),
                    step.stepName(),
                    step.purpose(),
                    step.expectedOutput(),
                    step.boundSkills() == null ? List.of() : step.boundSkills()));
        }
        List<String> riskNotes = plan.riskNotes() == null ? List.of() : plan.riskNotes();
        return new AgentPlan(
                plan.agentCode(),
                plan.agentName(),
                plan.goalSummary(),
                plan.planSummary(),
                normalizedSteps,
                riskNotes,
                plan.requireConfirmation());
    }

    /**
     * 读取已完成步骤结果
     */
    private List<AgentStepResult> loadCompletedStepResults(String runId) throws SQLException {
        List<AgentStepResult> results = new ArrayList<>();
        List<AiAgentStepLog> stepLogs = runLogDao.findStepLogs(runId);
        for (AiAgentStepLog stepLog : stepLogs) {
            if ("COMPLETED".equalsIgnoreCase(stepLog.getStatus())) {
                results.add(new AgentStepResult(stepLog.getStepNo(), stepLog.getStepName(), defaultText(stepLog.getStepOutput()), List.of()));
            }
        }
        return results;
    }

    /**
     * 恢复时优先使用历史计划，无法使用时按当前配置重建
     */
    private AgentPlan resolvePlanForResume(AiAgentRunRecord sourceRecord, AgentExecutionRequest request, boolean deriveNewRun) throws Exception {
        AgentPlan storedPlan = null;
        if (!deriveNewRun && sourceRecord.getPlanJson() != null && !sourceRecord.getPlanJson().isBlank()) {
            try {
                storedPlan = readPlanJson(sourceRecord.getPlanJson());
            } catch (Exception ignored) {
                storedPlan = null;
            }
        }
        if (storedPlan != null) {
            return storedPlan;
        }

        List<AgentPlanStep> steps = buildTemplateSteps(
                resolveTemplateCode(request.agentDefinition()),
                request.agentDefinition().getId(),
                request.strategy(),
                request.maxSteps());

        List<String> riskNotes = new ArrayList<>();
        if (sourceRecord.getRecentError() != null && !sourceRecord.getRecentError().isBlank()) {
            riskNotes.add("上次最近错误：" + abbreviate(sourceRecord.getRecentError(), 160));
        }
        if (deriveNewRun) {
            riskNotes.add("当前恢复已检测到参数修改，系统已按当前 Agent 配置重建固定步骤模板。 ");
        } else {
            riskNotes.add("历史计划快照不可直接使用，系统已按当前 Agent 配置重建固定步骤模板。 ");
        }

        return new AgentPlan(
                request.agentDefinition().getAgentCode(),
                defaultText(request.agentDefinition().getAgentName()),
                abbreviate(defaultText(request.userInput()), 120),
                deriveNewRun
                        ? "这是一次从历史运行派生的新恢复任务，执行计划已按当前参数重新生成。"
                        : "这是一次从历史运行恢复的任务，因历史计划缺失或损坏，系统已按当前参数重建执行步骤。",
                steps,
                riskNotes,
                request.confirmBeforeRun());
    }

    /**
     * 将 AgentPlan 转换为 JSON 字符串
     * @param plan AgentPlan 实例
     * @return JSON 字符串
     */
    private String writePlanJson(AgentPlan plan) {
        try {
            return OBJECT_MAPPER.writeValueAsString(plan);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 将请求快照转换为JSON
     */
    private String writeRequestJson(AgentExecutionRequest request) {
        if (request == null) {
            return null;
        }
        AgentDefinitionSnapshot definitionSnapshot = new AgentDefinitionSnapshot(
                request.agentDefinition().getAgentCode(),
                request.agentDefinition().getAgentName(),
                request.agentDefinition().getDescription(),
                request.agentDefinition().getPlannerPrompt(),
                request.agentDefinition().getExecutionPrompt(),
                resolveTemplateCode(request.agentDefinition()),
                request.agentDefinition().isFinalSummaryEnabled());
        AgentRequestSnapshot snapshot = new AgentRequestSnapshot(
                definitionSnapshot,
                request.apiKey(),
                request.model(),
                request.userInput(),
                request.sceneName(),
                request.subCategory(),
                request.scenePrompt(),
                request.fileContext(),
                request.historySummary(),
                request.confirmBeforeRun(),
                request.useFileContext(),
                request.useChatHistory(),
                request.strategy(),
                request.maxSteps());
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * Planner 応答から JSON ノードを安定的に抽出する
     */
    private JsonNode resolvePlannerResponseJson(String response) throws IOException {
        if (response == null || response.isBlank()) {
            throw new IOException("规划阶段返回为空");
        }

        String directCandidate = stripCodeFence(response).trim();
        JsonNode directNode = tryReadJson(directCandidate);
        if (isPlannerJsonNode(directNode)) {
            return directNode;
        }

        if (directNode != null && directNode.has("choices")) {
            JsonNode contentNode = directNode.path("choices").path(0).path("message").path("content");
            if (contentNode != null && !contentNode.isMissingNode()) {
                JsonNode nested = tryReadJson(stripCodeFence(contentNode.asText()));
                if (isPlannerJsonNode(nested)) {
                    return nested;
                }
            }
        }

        String extracted = extractFirstJsonObject(directCandidate);
        if (extracted != null) {
            JsonNode extractedNode = tryReadJson(extracted);
            if (isPlannerJsonNode(extractedNode)) {
                return extractedNode;
            }
        }

        throw new IOException("规划阶段返回无法解析为目标 JSON");
    }

    /**
     * 文字列がコードフェンスを含む場合は内容だけを返す
     */
    private String stripCodeFence(String rawText) {
        if (rawText == null) {
            return "";
        }
        String trimmed = rawText.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewLine = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewLine >= 0 && lastFence > firstNewLine) {
            return trimmed.substring(firstNewLine + 1, lastFence).trim();
        }
        return trimmed;
    }

    /**
     * 文字列を JSON として読み取り、失敗時は null を返す
     */
    private JsonNode tryReadJson(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(candidate);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Planner の期待構造を満たす JSON か判定する
     */
    private boolean isPlannerJsonNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        return node.has("goalSummary") || node.has("planSummary") || node.has("riskNotes");
    }

    /**
     * テキスト中から最初の JSON オブジェクト文字列を抽出する
     */
    private String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                if (depth > 0) {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从 JSON 节点中读取文本字段，若不存在或为空则返回默认值
     * @param root JSON 根节点
     * @param fieldName 字段名称
     * @param defaultValue 默认值
     * @return 字段文本或默认值
     */
    private String readText(JsonNode root, String fieldName, String defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null) {
            return defaultValue;
        }
        String text = node.asText().trim();
        return text.isBlank() ? defaultValue : text;
    }

    /**
     * 对文本进行折叠处理，保留开头和结尾部分，中间内容用提示替代
     * @param content 原始文本
     * @param maxLength 最大长度
     * @return 折叠后的文本
     */
    private String abbreviate(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace("\r", "").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        int headLength = Math.max(1, maxLength * 2 / 3);
        int tailLength = Math.max(1, maxLength - headLength - 24);
        return normalized.substring(0, headLength)
                + "\n\n...（中间内容已折叠）...\n\n"
                + normalized.substring(normalized.length() - tailLength);
    }

    private String resolveTemplateCode(AiAgentDefinition definition) {
        if (definition == null || definition.getTemplateCode() == null || definition.getTemplateCode().isBlank()) {
            return definition != null ? definition.getAgentCode() : "default";
        }
        return definition.getTemplateCode();
    }

    private String resolveSkillSystemPrompt(AgentBoundSkill skill, String fallbackPrompt) {
        if (skill.systemPrompt() != null && !skill.systemPrompt().isBlank()) {
            return skill.systemPrompt();
        }
        return fallbackPrompt;
    }

    private String joinStepResults(List<AgentStepResult> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (AgentStepResult result : stepResults) {
            builder.append(result.stepNo())
                    .append(". ")
                    .append(result.stepName())
                    .append("\n")
                    .append(abbreviate(result.output(), 1200))
                    .append("\n\n");
        }
        return builder.toString().trim();
    }

    private String applyTemplate(String template, Map<String, String> values) {
        String applied = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            applied = applied.replace("{{" + entry.getKey() + "}}", defaultText(entry.getValue()));
        }
        return applied;
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }
}