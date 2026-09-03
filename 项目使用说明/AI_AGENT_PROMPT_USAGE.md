# AI Agent Prompt 使用说明

本说明对应以下改造：
- Planner Prompt 与业务生成 Prompt 已拆分
- Planner 阶段 JSON 解析容错已增强
- 日志已落到项目目录 `.cqham/ai-debug`

## 1. 改造点

代码位置：
- `src/main/java/com/reiwaxr/cq/cqham/service/AiAgentService.java`

主要变化：
1. Planner 使用独立系统提示词，不再混入场景长文本规则。
2. Planner 用户输入只保留任务摘要、场景名、必要上下文摘要、固定步骤模板。
3. Planner JSON 解析支持：
- 代码块包裹（```json ... ```）
- 文本中夹杂 JSON
- 外层包裹 `choices[0].message.content` 的场景
4. riskNotes 支持数组或单字符串回退处理。

## 2. 如何替换 Prompt

1. 打开 `AI_PROMPT_TEMPLATE.md`。
2. 将「Planner Prompt」替换到 Agent 的 Planner Prompt 字段。
3. 将「Execution Prompt」替换到 Agent 的 Execution Prompt 字段。
4. 场景 Prompt 中删除 JSON 输出约束，仅保留业务规则。
5. 若有输入完整性校验需求，新增一个 Validator Skill，使用模板中的第 3 段。

## 3. 推荐运行流程

1. 先由 Validator 校验输入字段完整性。
2. 完整后进入 Planner，生成固定 JSON 计划。
3. 再按 Execution Prompt 生成最终业务内容。

## 4. 如何看日志

日志目录：
- `.cqham/ai-debug`

建议重点看：
1. `ai.agent.request-*.json`：Agent 发出的 systemPrompt/messages
2. `ai.chat.request-*.json`：实际发给模型的请求体
3. `ai.chat.response-*.json`：模型原始返回
4. `ai.agent.response-*.json`：Agent 层收到的 content

## 5. 常见问题排查

1. Planner 仍然输出非 JSON：
- 检查 Planner Prompt 是否仍混入业务 Markdown 规则。
- 检查场景 Prompt 是否含“严格输出 JSON”。

2. 最终文案仍像计划而不是结果：
- 检查 Execution Prompt 是否误放为 Planner Prompt。
- 检查是否把固定步骤模板放到了最终生成阶段的 systemPrompt。

3. 内容过长或偏题：
- 缩短场景 Prompt，避免重复规则。
- 将强约束保留在 Planner/Validator，Execution 只保留交付目标与格式。

## 6. 最小维护建议

1. 一个阶段只保留一种输出契约：
- Planner = JSON
- Execution = 文案

2. 不要在单个 prompt 同时要求：
- “严格 JSON”
- “输出 Markdown 表格与长文档”

3. 每次改 prompt 后，先跑一个小样例并检查 `.cqham/ai-debug` 四类日志是否一致。
