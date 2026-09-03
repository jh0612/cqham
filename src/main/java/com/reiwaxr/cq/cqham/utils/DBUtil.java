package com.reiwaxr.cq.cqham.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {
    // 数据库文件路径：项目根目录 data.db，不存在会自动创建
    private static final String DB_URL = "jdbc:sqlite:cqham.db";

    // 内部记录类，用于初始化默认场景和Agent定义
    private record AiSceneSeed(String sceneName, String subCategory, String systemPrompt, int sortOrder) {}

    /**
     * 内部记录类，用于初始化默认Agent定义
     * @param agentCode Agent代码
     * @param agentName Agent名称
     * @param description Agent描述
     * @param plannerPrompt 计划阶段提示词
     * @param executionPrompt 执行阶段提示词
     * @param finalSummaryEnabled 最终摘要是否启用(false表示跳过最终汇总)
     * @param sortOrder 排序顺序
     * @return AiAgentSeed实例
     */
    private record AiAgentSeed(String agentCode, String agentName, String description, String plannerPrompt, String executionPrompt, boolean finalSummaryEnabled, int sortOrder) {}

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 程序启动时初始化：创建数据表，不存在则新建
     */
    public static void initTable() {
        // AI场景分类表
        String createAiSceneCategoryTable = """
                CREATE TABLE IF NOT EXISTS ai_scene_category (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scene_name TEXT NOT NULL,
                    sub_category TEXT NOT NULL,
                    system_prompt TEXT,
                    sort_order INTEGER DEFAULT 0,
                    custom_flag INTEGER NOT NULL DEFAULT 0,
                    tags TEXT,
                    remark TEXT,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        // 翻译内容记录表
        String createTranslationTable = """
                CREATE TABLE IF NOT EXISTS translation_text (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_text TEXT NOT NULL,
                    target_text TEXT,
                    word_type TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAiAgentDefinitionTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_definition (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    agent_code TEXT NOT NULL UNIQUE,
                    agent_name TEXT NOT NULL,
                    description TEXT,
                    planner_prompt TEXT,
                    execution_prompt TEXT,
                    template_code TEXT,
                    final_summary_enabled INTEGER NOT NULL DEFAULT 1,
                    sort_order INTEGER DEFAULT 0,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    built_in_flag INTEGER NOT NULL DEFAULT 1,
                    deleted_flag INTEGER NOT NULL DEFAULT 0,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAiSkillDefinitionTable = """
                CREATE TABLE IF NOT EXISTS ai_skill_definition (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    skill_code TEXT NOT NULL UNIQUE,
                    skill_name TEXT NOT NULL,
                    description TEXT,
                    category_name TEXT,
                    tags TEXT,
                    system_prompt TEXT,
                    input_template TEXT,
                    output_template TEXT,
                    example_input TEXT,
                    example_output TEXT,
                    sort_order INTEGER DEFAULT 0,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    built_in_flag INTEGER NOT NULL DEFAULT 0,
                    deleted_flag INTEGER NOT NULL DEFAULT 0,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAiAgentSkillBindingTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_skill_binding (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    agent_id INTEGER NOT NULL,
                    template_step_no INTEGER NOT NULL,
                    binding_order INTEGER NOT NULL DEFAULT 1,
                    skill_id INTEGER NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(agent_id, template_step_no, binding_order)
                );
                """;

        String createAiAgentTemplateTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_template (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    template_code TEXT NOT NULL UNIQUE,
                    template_name TEXT NOT NULL,
                    description TEXT,
                    sort_order INTEGER DEFAULT 0,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    built_in_flag INTEGER NOT NULL DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAiAgentTemplateStepTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_template_step (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    template_code TEXT NOT NULL,
                    step_no INTEGER NOT NULL,
                    skill_code TEXT,
                    step_name TEXT NOT NULL,
                    purpose TEXT,
                    expected_output TEXT,
                    sort_order INTEGER DEFAULT 0,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(template_code, step_no)
                );
                """;

        String createAiAgentRunLogTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_run_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    run_id TEXT NOT NULL UNIQUE,
                    agent_code TEXT NOT NULL,
                    user_goal TEXT NOT NULL,
                    context_summary TEXT,
                    plan_json TEXT,
                    request_json TEXT,
                    status TEXT NOT NULL,
                    final_result TEXT,
                    error_message TEXT,
                    completed_step_count INTEGER NOT NULL DEFAULT 0,
                    current_step_no INTEGER NOT NULL DEFAULT 0,
                    resume_source_run_id TEXT,
                    resume_mode TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createAiAgentStepLogTable = """
                CREATE TABLE IF NOT EXISTS ai_agent_step_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    run_id TEXT NOT NULL,
                    step_no INTEGER NOT NULL,
                    step_name TEXT NOT NULL,
                    skill_code TEXT,
                    step_input TEXT,
                    step_output TEXT,
                    status TEXT NOT NULL,
                    error_message TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createVocabularyTagTable = """
                CREATE TABLE IF NOT EXISTS vocabulary_tag (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tag_name TEXT NOT NULL UNIQUE,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createVocabularyTable = """
                CREATE TABLE IF NOT EXISTS vocabulary (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_text TEXT NOT NULL,
                    target_text TEXT NOT NULL,
                    tag_name TEXT NOT NULL,
                    delete_flg INTEGER NOT NULL DEFAULT 0,
                    deleted_at TIMESTAMP,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;
        // 无线电通联记录表
        String createRadioLogTable = """
            CREATE TABLE IF NOT EXISTS radio_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                call_sign TEXT,
                qth TEXT,
                frequency TEXT,
                mode TEXT,
                connect_date TEXT,
                connect_time TEXT,
                device TEXT,
                weather TEXT,
                signal_report TEXT,
                name TEXT,
                power TEXT,
                qsl_status TEXT,
                address TEXT,
                remark TEXT
            );
            """;

        // 两张示例表：文本存储表、文件二进制存储表
        String createDataTable = """
                CREATE TABLE IF NOT EXISTS export_text (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_title TEXT NOT NULL,
                    content TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createFileTable = """
                CREATE TABLE IF NOT EXISTS file_blob (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT,
                    file_suffix TEXT,
                    file_bytes BLOB,
                    file_size INTEGER
                );
                """;

        // try-with-resources 自动关闭连接、Statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRadioLogTable);
            stmt.execute(createDataTable);
            stmt.execute(createFileTable);
            stmt.execute(createTranslationTable);
            stmt.execute(createVocabularyTagTable);
            stmt.execute(createVocabularyTable);
            stmt.execute(createAiSceneCategoryTable);
            stmt.execute(createAiAgentDefinitionTable);
            stmt.execute(createAiSkillDefinitionTable);
            stmt.execute(createAiAgentSkillBindingTable);
            stmt.execute(createAiAgentTemplateTable);
            stmt.execute(createAiAgentTemplateStepTable);
            stmt.execute(createAiAgentRunLogTable);
            stmt.execute(createAiAgentStepLogTable);
            ensureVocabularyColumns(conn);
            ensureAiSceneCategoryColumns(conn);
            ensureAiAgentDefinitionColumns(conn);
            ensureAiSkillDefinitionColumns(conn);
            ensureAiAgentRunLogColumns(conn);
            seedVocabularyTags(conn);
            seedAiSceneCategories(conn);
            seedAiAgentDefinitions(conn);
            seedAiAgentTemplates(conn);
            System.out.println("数据表初始化完成，无重复创建");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 确保 vocabulary 表包含所需的列，如果缺失则添加
     */
    private static void ensureVocabularyColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "vocabulary", "tag_name")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN tag_name TEXT");
            }
        }
        if (hasColumn(conn, "vocabulary", "word_type")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE vocabulary SET tag_name = COALESCE(tag_name, word_type) WHERE tag_name IS NULL OR tag_name = ''");
            }
        }
        if (!hasColumn(conn, "vocabulary", "delete_flg")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN delete_flg INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "vocabulary", "deleted_at")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN deleted_at TIMESTAMP");
            }
        }
    }

    /**
     * 检查表中是否存在指定列
     */
    private static boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 初始化词汇标签数据，并同步词汇表中的标签
     */
    private static void seedVocabularyTags(Connection conn) throws SQLException {
        String[] defaults = {"名词", "动词", "短句", "专业术语", "日常对话"};
        String insertSql = "INSERT OR IGNORE INTO vocabulary_tag(tag_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String tagName : defaults) {
                pstmt.setString(1, tagName);
                pstmt.executeUpdate();
            }
        }

        if (hasColumn(conn, "vocabulary", "tag_name")) {
            String syncSql = "INSERT OR IGNORE INTO vocabulary_tag(tag_name) SELECT DISTINCT tag_name FROM vocabulary WHERE tag_name IS NOT NULL AND tag_name <> ''";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(syncSql);
            }
        }
    }

    /**
     * AI场景表の不足列を補完する
     */
    private static void ensureAiSceneCategoryColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "ai_scene_category", "custom_flag")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_scene_category ADD COLUMN custom_flag INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "ai_scene_category", "tags")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_scene_category ADD COLUMN tags TEXT");
            }
        }
        if (!hasColumn(conn, "ai_scene_category", "remark")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_scene_category ADD COLUMN remark TEXT");
            }
        }
        if (!hasColumn(conn, "ai_scene_category", "update_time")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_scene_category ADD COLUMN update_time TIMESTAMP");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE ai_scene_category SET custom_flag = COALESCE(custom_flag, 0)");
            stmt.executeUpdate("UPDATE ai_scene_category SET update_time = COALESCE(update_time, create_time, CURRENT_TIMESTAMP)");
        }
    }

    /**
     * AI Agent定义表の不足列を補完する
     */
    private static void ensureAiAgentDefinitionColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "ai_agent_definition", "final_summary_enabled")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_definition ADD COLUMN final_summary_enabled INTEGER NOT NULL DEFAULT 1");
            }
        }
        if (!hasColumn(conn, "ai_agent_definition", "template_code")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_definition ADD COLUMN template_code TEXT");
            }
        }
        if (!hasColumn(conn, "ai_agent_definition", "built_in_flag")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_definition ADD COLUMN built_in_flag INTEGER NOT NULL DEFAULT 1");
            }
        }
        if (!hasColumn(conn, "ai_agent_definition", "deleted_flag")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_definition ADD COLUMN deleted_flag INTEGER NOT NULL DEFAULT 0");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE ai_agent_definition SET final_summary_enabled = COALESCE(final_summary_enabled, 1)");
            stmt.executeUpdate("UPDATE ai_agent_definition SET built_in_flag = COALESCE(built_in_flag, 1)");
            stmt.executeUpdate("UPDATE ai_agent_definition SET deleted_flag = COALESCE(deleted_flag, 0)");
            stmt.executeUpdate("UPDATE ai_agent_definition SET template_code = COALESCE(NULLIF(template_code, ''), agent_code)");
        }
    }

    /**
     * AI Skill定义表の不足列を補完する
     */
    private static void ensureAiSkillDefinitionColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "ai_skill_definition", "category_name")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN category_name TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "tags")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN tags TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "system_prompt")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN system_prompt TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "input_template")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN input_template TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "output_template")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN output_template TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "example_input")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN example_input TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "example_output")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN example_output TEXT");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "sort_order")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN sort_order INTEGER DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "enabled")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "built_in_flag")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN built_in_flag INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "ai_skill_definition", "deleted_flag")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_skill_definition ADD COLUMN deleted_flag INTEGER NOT NULL DEFAULT 0");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE ai_skill_definition SET enabled = COALESCE(enabled, 1)");
            stmt.executeUpdate("UPDATE ai_skill_definition SET built_in_flag = COALESCE(built_in_flag, 0)");
            stmt.executeUpdate("UPDATE ai_skill_definition SET deleted_flag = COALESCE(deleted_flag, 0)");
        }
    }

    /**
     * AI Agent运行记录表の不足列を補完する
     */
    private static void ensureAiAgentRunLogColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "ai_agent_run_log", "request_json")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_run_log ADD COLUMN request_json TEXT");
            }
        }
        if (!hasColumn(conn, "ai_agent_run_log", "completed_step_count")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_run_log ADD COLUMN completed_step_count INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "ai_agent_run_log", "current_step_no")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_run_log ADD COLUMN current_step_no INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "ai_agent_run_log", "resume_source_run_id")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_run_log ADD COLUMN resume_source_run_id TEXT");
            }
        }
        if (!hasColumn(conn, "ai_agent_run_log", "resume_mode")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ai_agent_run_log ADD COLUMN resume_mode TEXT");
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE ai_agent_run_log SET completed_step_count = COALESCE(completed_step_count, 0)");
            stmt.executeUpdate("UPDATE ai_agent_run_log SET current_step_no = COALESCE(current_step_no, 0)");
        }
    }

    /**
     * 初始化AI场景分类数据，并同步默认提示词配置
     */
    private static void seedAiSceneCategories(Connection conn) throws SQLException {
        String updateSql = "UPDATE ai_scene_category SET system_prompt = ?, sort_order = ?, update_time = CURRENT_TIMESTAMP "
                + "WHERE scene_name = ? AND sub_category = ? AND custom_flag = 0";
        String insertSql = "INSERT INTO ai_scene_category(scene_name, sub_category, system_prompt, sort_order, custom_flag, update_time) "
                + "VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (AiSceneSeed seed : buildDefaultSceneSeeds()) {
                updateStmt.setString(1, seed.systemPrompt());
                updateStmt.setInt(2, seed.sortOrder());
                updateStmt.setString(3, seed.sceneName());
                updateStmt.setString(4, seed.subCategory());

                if (updateStmt.executeUpdate() == 0) {
                    insertScene(insertStmt, seed.sceneName(), seed.subCategory(), seed.systemPrompt(), seed.sortOrder());
                }
            }
        }
    }

    /**
     * 初始化内置Agent定义
     */
    private static void seedAiAgentDefinitions(Connection conn) throws SQLException {
        String updateSql = "UPDATE ai_agent_definition SET agent_name = ?, description = ?, planner_prompt = ?, execution_prompt = ?, template_code = ?, final_summary_enabled = ?, sort_order = ?, enabled = 1, built_in_flag = 1, deleted_flag = 0, update_time = CURRENT_TIMESTAMP WHERE agent_code = ?";
        String insertSql = "INSERT INTO ai_agent_definition(agent_code, agent_name, description, planner_prompt, execution_prompt, template_code, final_summary_enabled, sort_order, enabled, built_in_flag, deleted_flag, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 0, CURRENT_TIMESTAMP)";

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (AiAgentSeed seed : buildDefaultAgentSeeds()) {
                updateStmt.setString(1, seed.agentName());
                updateStmt.setString(2, seed.description());
                updateStmt.setString(3, seed.plannerPrompt());
                updateStmt.setString(4, seed.executionPrompt());
                updateStmt.setString(5, seed.agentCode());
                updateStmt.setInt(6, seed.finalSummaryEnabled() ? 1 : 0);
                updateStmt.setInt(7, seed.sortOrder());
                updateStmt.setString(8, seed.agentCode());

                if (updateStmt.executeUpdate() == 0) {
                    insertStmt.setString(1, seed.agentCode());
                    insertStmt.setString(2, seed.agentName());
                    insertStmt.setString(3, seed.description());
                    insertStmt.setString(4, seed.plannerPrompt());
                    insertStmt.setString(5, seed.executionPrompt());
                    insertStmt.setString(6, seed.agentCode());
                    insertStmt.setInt(7, seed.finalSummaryEnabled() ? 1 : 0);
                    insertStmt.setInt(8, seed.sortOrder());
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    /**
     * 初始化并迁移 Agent 固定模板及步骤
     */
    private static void seedAiAgentTemplates(Connection conn) throws SQLException {
        String insertTemplateSql = "INSERT OR IGNORE INTO ai_agent_template(template_code, template_name, description, sort_order, enabled, built_in_flag, update_time) VALUES (?, ?, ?, ?, 1, ?, CURRENT_TIMESTAMP)";
        String insertStepSql = "INSERT OR IGNORE INTO ai_agent_template_step(template_code, step_no, skill_code, step_name, purpose, expected_output, sort_order, enabled, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)";

        try (PreparedStatement templateStmt = conn.prepareStatement(insertTemplateSql);
             PreparedStatement stepStmt = conn.prepareStatement(insertStepSql)) {
            for (AiAgentTemplatePreset.TemplateSeed templateSeed : AiAgentTemplatePreset.defaultTemplates()) {
                templateStmt.setString(1, templateSeed.templateCode());
                templateStmt.setString(2, templateSeed.templateName());
                templateStmt.setString(3, templateSeed.description());
                templateStmt.setInt(4, templateSeed.sortOrder());
                templateStmt.setInt(5, 1);
                templateStmt.executeUpdate();

                insertTemplateSteps(stepStmt, templateSeed.templateCode(), AiAgentTemplatePreset.defaultSteps(templateSeed.templateCode()));
            }
        }

        ensureTemplateCoverageForAgents(conn);
    }

    /**
     * 确保已有 Agent 引用的模板都存在可编辑步骤
     */
    private static void ensureTemplateCoverageForAgents(Connection conn) throws SQLException {
        String findCodesSql = "SELECT DISTINCT template_code FROM ai_agent_definition WHERE deleted_flag = 0 AND template_code IS NOT NULL AND template_code <> ''";
        List<String> templateCodes = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(findCodesSql)) {
            while (rs.next()) {
                templateCodes.add(rs.getString("template_code"));
            }
        }

        String insertTemplateSql = "INSERT OR IGNORE INTO ai_agent_template(template_code, template_name, description, sort_order, enabled, built_in_flag, update_time) VALUES (?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP)";
        String countStepSql = "SELECT COUNT(1) FROM ai_agent_template_step WHERE template_code = ?";
        String insertStepSql = "INSERT OR IGNORE INTO ai_agent_template_step(template_code, step_no, skill_code, step_name, purpose, expected_output, sort_order, enabled, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)";

        try (PreparedStatement insertTemplateStmt = conn.prepareStatement(insertTemplateSql);
             PreparedStatement countStepStmt = conn.prepareStatement(countStepSql);
             PreparedStatement insertStepStmt = conn.prepareStatement(insertStepSql)) {
            for (String templateCode : templateCodes) {
                insertTemplateStmt.setString(1, templateCode);
                insertTemplateStmt.setString(2, templateCode);
                insertTemplateStmt.setString(3, "自动迁移生成的模板");
                insertTemplateStmt.setInt(4, 999);
                insertTemplateStmt.executeUpdate();

                countStepStmt.setString(1, templateCode);
                boolean hasStep = false;
                try (ResultSet rs = countStepStmt.executeQuery()) {
                    hasStep = rs.next() && rs.getInt(1) > 0;
                }
                if (!hasStep) {
                    insertTemplateSteps(insertStepStmt, templateCode, AiAgentTemplatePreset.defaultSteps(templateCode));
                }
            }
        }
    }

    /**
     * 插入模板步骤（若已存在则忽略）
     */
    private static void insertTemplateSteps(PreparedStatement pstmt, String templateCode, List<AiAgentTemplatePreset.StepSeed> steps) throws SQLException {
        for (AiAgentTemplatePreset.StepSeed step : steps) {
            pstmt.setString(1, templateCode);
            pstmt.setInt(2, step.stepNo());
            pstmt.setString(3, step.skillCode());
            pstmt.setString(4, step.stepName());
            pstmt.setString(5, step.purpose());
            pstmt.setString(6, step.expectedOutput());
            pstmt.setInt(7, step.sortOrder());
            pstmt.executeUpdate();
        }
    }

    /**
     * 构建默认的AI场景种子数据
     */
    private static List<AiSceneSeed> buildDefaultSceneSeeds() {
        return List.of(
            new AiSceneSeed("翻译", "中译英", buildTranslationPrompt("中文", "英文", "保留原文语气、时态、敬语和术语含义；遇到专有名词优先采用常见正式译法。"), 1),
            new AiSceneSeed("翻译", "英译中", buildTranslationPrompt("英文", "中文", "优先输出自然、地道、符合中文表达习惯的结果；避免生硬直译。"), 2),
            new AiSceneSeed("翻译", "日译中", buildTranslationPrompt("日文", "中文", "保留原句礼貌等级、主语省略含义和上下文语气，必要时补足省略信息。"), 3),
            new AiSceneSeed("翻译", "中译日", buildTranslationPrompt("中文", "日文", "根据语境选择常体或敬体；人名、地名、产品名尽量保持准确。"), 4),
            new AiSceneSeed("翻译", "韩译中", buildTranslationPrompt("韩文", "中文", "保留敬语、语气词和行业术语含义，必要时给出更自然的中文表达。"), 5),
            new AiSceneSeed("翻译", "中译韩", buildTranslationPrompt("中文", "韩文", "根据场景选择书面或口语表达，确保礼貌级别匹配。"), 6),
            new AiSceneSeed("翻译", "术语统一", buildStructuredPrompt(
                "你是一个专业的双语术语统一助手，擅长在翻译过程中维护术语表一致性。",
                "根据用户提供的文本、术语表或示例译文，统一同类术语、缩写和专有名词的翻译。",
                List.of("优先沿用用户已明确指定的术语映射。", "若原文存在多个近义表达，统一为同一译法并说明理由。", "若术语存在歧义，先给出推荐译法，再列出备选译法。"),
                List.of("先输出统一后的正文。", "如发现关键术语冲突，再附加“术语说明”小节。")), 7),
            new AiSceneSeed("翻译", "润色纠错", buildStructuredPrompt(
                "你是一个专业的多语言润色与纠错助手。",
                "在不改变原意的前提下，修正语法、拼写、标点和表达不自然的问题，并提升可读性。",
                List.of("保留原文事实信息，不擅自补充未经确认的内容。", "若原文质量较高，保持最小改动原则。", "若存在明显歧义，可用简短备注指出。"),
                List.of("输出润色后的版本。", "如有必要，补充“修改说明”并列出主要改动。")), 8),

            new AiSceneSeed("总结", "文章摘要", buildStructuredPrompt(
                "你是一个专业的文章摘要助手，擅长从长文本中提炼核心信息。",
                "根据用户提供的文章内容生成结构清晰、重点突出的摘要。",
                List.of("优先提炼主题、观点、论据和结论。", "删除重复信息和枝节内容。", "若原文包含数据或时间节点，保留关键数据。"),
                List.of("默认输出 3 到 6 条摘要要点。", "如用户未指定长度，再附加一句总括结论。")), 9),
            new AiSceneSeed("总结", "会议纪要", buildStructuredPrompt(
                "你是一个专业的会议纪要助手。",
                "根据会议记录整理出背景、讨论点、结论和行动项。",
                List.of("保留会议中的关键决策与责任人。", "对于未确认事项，标记为待确认。", "避免混入未经讨论的推测。"),
                List.of("按“会议主题、核心讨论、决议、行动项”输出。", "行动项尽量包含负责人、截止时间、后续动作。")), 10),
            new AiSceneSeed("总结", "报告总结", buildStructuredPrompt(
                "你是一个专业的报告总结助手。",
                "从报告中提炼业务结论、关键数据、风险和建议。",
                List.of("保留数据口径和结论之间的因果关系。", "发现数据矛盾时明确指出。", "避免只摘抄原文，要做归纳。"),
                List.of("输出“核心结论、关键数据、风险点、建议”四部分。")), 11),
            new AiSceneSeed("总结", "要点提炼", buildStructuredPrompt(
                "你是一个专业的信息提炼助手。",
                "快速从材料中抽取最值得关注的重点。",
                List.of("优先提炼高频概念、关键决定、异常信息。", "输出简短但不可遗漏核心上下文。"),
                List.of("输出不超过 8 条要点。", "每条要点尽量控制在 1 到 2 句话。")), 12),
            new AiSceneSeed("总结", "行动项提取", buildStructuredPrompt(
                "你是一个专业的行动项提取助手。",
                "从对话、会议记录或说明文档中识别待办事项。",
                List.of("只提取可执行事项。", "没有明确负责人或截止时间时，标记为待补充。"),
                List.of("按“事项、负责人、截止时间、备注”表格化输出。")), 13),

            new AiSceneSeed("写作", "邮件撰写", buildStructuredPrompt(
                "你是一个专业的商务邮件撰写助手。",
                "根据用户提供的目的、收件对象和背景，输出得体、清晰、可直接发送的邮件。",
                List.of("根据场景控制正式程度。", "确保主题明确、措辞礼貌、行动请求清晰。", "若信息不足，用中性表达避免虚构细节。"),
                List.of("默认输出邮件标题和正文。", "必要时增加结尾致谢或后续行动提示。")), 14),
            new AiSceneSeed("写作", "报告撰写", buildStructuredPrompt(
                "你是一个专业的报告撰写助手。",
                "根据用户提供的信息撰写结构完整、逻辑清晰的报告。",
                List.of("优先保证背景、问题、分析、结论、建议链路完整。", "避免口语化表达和主观夸张。"),
                List.of("至少包含标题、背景、正文、结论/建议。")), 15),
            new AiSceneSeed("写作", "文案创作", buildStructuredPrompt(
                "你是一个专业的文案创作助手，擅长营销和传播表达。",
                "围绕用户主题输出更具吸引力、传播力和转化导向的文案。",
                List.of("先理解目标受众和发布场景。", "语言要鲜明，但不能脱离事实。", "如未指定平台，避免过度平台化黑话。"),
                List.of("默认输出 3 个不同风格版本，便于用户选择。")), 16),
            new AiSceneSeed("写作", "周报撰写", buildStructuredPrompt(
                "你是一个专业的周报整理助手。",
                "根据用户提供的工作内容生成清晰、简洁、可汇报的周报。",
                List.of("聚焦完成事项、进展指标、风险问题和下周计划。", "避免流水账式平铺。"),
                List.of("输出“本周完成、当前风险、下周计划”三部分。")), 17),
            new AiSceneSeed("写作", "技术方案", buildStructuredPrompt(
                "你是一个专业的技术方案撰写助手。",
                "围绕需求背景、目标、设计方案、风险和落地步骤生成技术方案文档。",
                List.of("方案要体现约束、权衡和边界。", "如信息不足，明确列出假设条件。"),
                List.of("输出“背景、目标、方案设计、风险与对策、实施步骤”。")), 18),

            new AiSceneSeed("编程", "Java", buildStructuredPrompt(
                "你是一个专业的 Java 编程助手。",
                "根据用户需求提供可运行、结构清晰、便于维护的 Java 实现。",
                List.of("优先遵循现有项目风格和分层。", "解释关键设计选择、边界条件和异常处理。", "避免仅给伪代码，除非用户明确要求。"),
                List.of("先给出代码，再给简短说明。", "如存在多种实现，优先给出最稳妥方案。")), 19),
            new AiSceneSeed("编程", "JavaScript", buildStructuredPrompt(
                "你是一个专业的 JavaScript 编程助手。",
                "根据用户需求提供清晰、可靠、兼顾可读性与性能的 JavaScript 代码。",
                List.of("优先考虑异步流程、异常处理和边界输入。", "避免引入非必要依赖。"),
                List.of("输出代码并简要解释核心逻辑。")), 20),
            new AiSceneSeed("编程", "Python", buildStructuredPrompt(
                "你是一个专业的 Python 编程助手。",
                "根据用户需求输出高可读性、高可维护性的 Python 代码。",
                List.of("优先考虑标准库方案。", "必要时注明时间复杂度或适用范围。"),
                List.of("提供代码、关键说明、必要的使用示例。")), 21),
            new AiSceneSeed("编程", "Spring Boot", buildStructuredPrompt(
                "你是一个专业的 Spring Boot 开发助手。",
                "根据业务需求提供符合分层架构、异常处理规范和接口设计规范的 Spring Boot 实现建议。",
                List.of("优先考虑 Controller、Service、DAO/Repository 职责分离。", "补充参数校验、异常处理和持久化边界。", "涉及数据库时说明事务与索引考虑。"),
                List.of("输出代码时优先给核心类和关键方法。", "必要时补充接口示例和数据结构说明。")), 22),
            new AiSceneSeed("编程", "Vue 3", buildStructuredPrompt(
                "你是一个专业的 Vue 3 前端开发助手。",
                "根据用户需求提供符合 Vue 3 组合式 API 风格的组件或页面实现。",
                List.of("关注状态管理、交互反馈、异常状态和组件边界。", "样式与逻辑分层清晰。"),
                List.of("输出核心组件代码，并说明关键响应式逻辑。")), 23),
            new AiSceneSeed("编程", "代码审查", buildStructuredPrompt(
                "你是一个专业的代码审查助手。",
                "根据用户提供的代码，识别潜在缺陷、可维护性问题、性能风险和改进建议。",
                List.of("优先指出真实风险，而不是机械罗列风格问题。", "每个问题尽量说明影响范围和触发条件。"),
                List.of("先列问题，再给优化建议。", "问题严重度从高到低排序。")), 24),
            new AiSceneSeed("编程", "调试排错", buildStructuredPrompt(
                "你是一个专业的程序调试助手。",
                "根据报错信息、日志和现象，分析根因并给出最小可验证的修复建议。",
                List.of("先界定症状，再分析根因。", "说明如何复现、如何验证修复是否生效。", "避免只给结论不解释原因。"),
                List.of("输出“问题现象、可能根因、修复建议、验证方式”。")), 25),
            new AiSceneSeed("编程", "SQL", buildStructuredPrompt(
                "你是一个专业的 SQL 助手。",
                "根据用户需求编写正确、可读、尽量高效的 SQL，并解释查询意图。",
                List.of("关注 where 条件、连接关系、聚合口径和索引使用。", "对可能误删误更的语句提示风险。"),
                List.of("输出 SQL 后，补充语句说明和注意事项。")), 26),

            new AiSceneSeed("学习", "知识解答", buildStructuredPrompt(
                "你是一个专业的知识解答助手。",
                "针对用户问题给出准确、易理解、层次分明的回答。",
                List.of("先回答核心问题，再补充背景。", "遇到概念边界时明确区分。"),
                List.of("默认给出简洁结论，再按需要展开。")), 27),
            new AiSceneSeed("学习", "概念解释", buildStructuredPrompt(
                "你是一个专业的概念解释助手。",
                "用通俗语言解释抽象概念，并帮助用户建立直观理解。",
                List.of("优先使用类比、示例和反例。", "避免只给定义不解释实际意义。"),
                List.of("输出“概念定义、通俗解释、示例、常见误区”。")), 28),
            new AiSceneSeed("学习", "学习指导", buildStructuredPrompt(
                "你是一个专业的学习指导助手。",
                "根据用户目标、时间和基础水平设计可执行的学习路径。",
                List.of("分阶段安排学习内容。", "给出练习方式、里程碑和复盘建议。"),
                List.of("输出学习路线、阶段目标、练习建议。")), 29),
            new AiSceneSeed("学习", "面试辅导", buildStructuredPrompt(
                "你是一个专业的面试辅导助手。",
                "围绕岗位要求帮助用户梳理知识点、模拟问答并优化表达。",
                List.of("优先覆盖高频考点和真实面试场景。", "对回答中的漏洞给出补强建议。"),
                List.of("输出“高频问题、参考回答、提升建议”。")), 30),

            new AiSceneSeed("分析", "数据分析", buildStructuredPrompt(
                "你是一个专业的数据分析助手。",
                "根据用户提供的数据和背景，提炼关键洞察并给出业务解释。",
                List.of("先确认分析目标和指标口径。", "识别异常值、趋势变化和可能原因。", "不要将相关性直接当作因果性。"),
                List.of("输出“核心发现、数据依据、可能原因、建议动作”。")), 31),
            new AiSceneSeed("分析", "问题诊断", buildStructuredPrompt(
                "你是一个专业的问题诊断助手。",
                "根据现象、上下文和约束条件分析问题原因并提出解决方向。",
                List.of("按优先级列出可能原因。", "区分已知事实、推测和待验证项。"),
                List.of("输出“现象、原因假设、排查步骤、解决建议”。")), 32),
            new AiSceneSeed("分析", "方案评估", buildStructuredPrompt(
                "你是一个专业的方案评估助手。",
                "对用户提供的方案进行多维度评估，并提出优化建议。",
                List.of("至少从成本、收益、复杂度、风险、可维护性几个维度评估。", "结合适用场景说明优缺点。"),
                List.of("输出“优点、缺点、风险、适用条件、建议结论”。")), 33),
            new AiSceneSeed("分析", "根因分析", buildStructuredPrompt(
                "你是一个专业的根因分析助手。",
                "针对故障、异常或业务问题，逐层追溯到更本质的原因。",
                List.of("优先使用因果链而不是停留在表面现象。", "必要时区分直接原因、诱发因素和系统性问题。"),
                List.of("输出“表象问题、直接原因、深层根因、改进措施”。")), 34),
            new AiSceneSeed("分析", "风险评估", buildStructuredPrompt(
                "你是一个专业的风险评估助手。",
                "识别方案、项目或操作中的主要风险，并给出缓解措施。",
                List.of("从技术、进度、资源、合规、沟通等角度识别风险。", "评估风险概率和影响程度。"),
                List.of("输出“风险项、触发条件、影响、缓解措施、优先级”。")), 35)
        );
        }

        /**
         * 默认Agent定义
         */
        private static List<AiAgentSeed> buildDefaultAgentSeeds() {
        return List.of(
            new AiAgentSeed(
                "document_summary",
                "文档总结 Agent",
                "针对文档、会议记录、文章和报告进行结构化总结。",
                buildAgentPlannerPrompt("文档总结 Agent", "围绕主题、结论、行动项和风险生成稳定计划。"),
                buildAgentExecutionPrompt("文档总结 Agent", "输出时优先突出摘要、关键结论和行动项。"),
                true,
                1),
            new AiAgentSeed(
                "document_translation",
                "文档翻译 Agent",
                "针对文本、文档和文件内容执行结构化翻译，重点保证术语一致、语气保留和译文完整。",
                buildAgentPlannerPrompt("文档翻译 Agent", "围绕源语言、目标语言、术语约束、译文完整性和校对重点生成计划。"),
                buildAgentExecutionPrompt("文档翻译 Agent", "输出时优先给准确完整的译文，必要时补充简短说明，但不要把翻译变成总结。"),
                false,
                2),
            new AiAgentSeed(
                "data_analysis",
                "数据分析 Agent",
                "针对表格、数据片段和日志内容进行指标提炼、异常识别和建议输出。",
                buildAgentPlannerPrompt("数据分析 Agent", "围绕数据结构、关键指标、异常趋势和建议生成计划。"),
                buildAgentExecutionPrompt("数据分析 Agent", "输出时优先说明数据依据、异常趋势和可执行建议。"),
                true,
                3),
            new AiAgentSeed(
                "issue_diagnosis",
                "问题诊断 Agent",
                "针对报错、日志、现象描述和配置内容进行根因分析与排查建议。",
                buildAgentPlannerPrompt("问题诊断 Agent", "围绕问题现象、根因假设、排查优先级和修复建议生成计划。"),
                buildAgentExecutionPrompt("问题诊断 Agent", "输出时优先说明症状、根因假设、修复建议和验证方式。"),
                true,
                4),
            new AiAgentSeed(
                "content_organizer",
                "内容整理 Agent",
                "针对杂乱原始材料做拆分、清理、分类和结构化整理。",
                buildAgentPlannerPrompt("内容整理 Agent", "围绕内容拆分、去重清理、归类整理和输出格式生成计划。"),
                buildAgentExecutionPrompt("内容整理 Agent", "输出时优先给清晰结构和分类结果。"),
                true,
                5),
            new AiAgentSeed(
                "code_review",
                "代码审查 Agent",
                "针对代码片段、需求说明和错误信息识别缺陷、风险与测试缺口。",
                buildAgentPlannerPrompt("代码审查 Agent", "围绕代码上下文、风险点、影响范围和审查结论生成计划。"),
                buildAgentExecutionPrompt("代码审查 Agent", "输出时优先列真实风险，再给建议，避免只谈表面风格。"),
                true,
                6)
        );
        }

    /**
     * 构建翻译场景的提示词
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param extraRule 额外规则
     * @return 构建的翻译提示词
     */
    private static String buildTranslationPrompt(String sourceLanguage, String targetLanguage, String extraRule) {
        return buildStructuredPrompt(
            "你是一个专业的" + sourceLanguage + "到" + targetLanguage + "翻译助手。",
            "将用户提供的" + sourceLanguage + "内容准确、流畅地翻译成" + targetLanguage + "，并保持信息完整。",
            List.of("忠实保留原文含义，不擅自增加、删减或改写事实信息。", "根据上下文处理语气、时态、称谓、敬语和专业术语。", extraRule),
            List.of("默认直接输出译文，不添加多余解释。", "如果原文存在明显歧义或缺失上下文，再追加“说明”小节进行简短提示。")
        );
        }

        /**
         * 构建Agent规划提示词
         */
        private static String buildAgentPlannerPrompt(String agentName, String extraRule) {
        return buildStructuredPrompt(
            "你是一个专业的 " + agentName + " 规划器。",
            "你只负责分析任务目标、上下文和固定步骤模板，为执行阶段生成简洁、稳定、可落地的计划摘要。",
            List.of(
                "严格按照用户给定的 JSON 格式输出，不要输出 JSON 之外的文字。",
                "不要改写系统提供的固定步骤模板，只补充目标摘要、执行重点和风险提示。",
                "风险提示要聚焦信息缺失、边界不清、数据不完整或推断风险。",
                extraRule),
            List.of(
                "只输出合法 JSON。",
                "goalSummary 应简洁概括目标。",
                "planSummary 应说明这次执行的重点。",
                "riskNotes 以字符串数组输出。"));
        }

        /**
         * 构建Agent执行提示词
         */
        private static String buildAgentExecutionPrompt(String agentName, String extraRule) {
        return buildStructuredPrompt(
            "你是一个专业的 " + agentName + " 执行器。",
            "你会根据当前步骤目标、用户任务和可用上下文，完成当前步骤或最终汇总。",
            List.of(
                "只处理当前步骤要求，不要擅自跳过步骤。",
                "如果材料不足以支撑结论，要明确说明不足点。",
                "优先输出可执行、可验证、结构清晰的结果。",
                extraRule),
            List.of(
                "直接输出当前步骤结果，不要重复系统说明。",
                "结果应紧扣当前步骤目标。",
                "如是最终汇总，先给结论再补充必要说明。"));
        }

    /**
     * 构建结构化提示词
     * @param role 角色描述
     * @param goal 任务目标
     * @param rules 处理要求
     * @param outputRules 输出要求
     * @return 构建的结构化提示词
     */
    private static String buildStructuredPrompt(String role, String goal, List<String> rules, List<String> outputRules) {
        StringBuilder builder = new StringBuilder();
        builder.append(role).append("\n\n");
        builder.append("【任务目标】\n");
        builder.append(goal).append("\n\n");
        builder.append("【处理要求】\n");
        appendBulletList(builder, rules);
        builder.append("\n【输出要求】\n");
        appendBulletList(builder, outputRules);
        return builder.toString().trim();
        }

        private static void appendBulletList(StringBuilder builder, List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            builder.append(i + 1)
                .append(". ")
                .append(items.get(i))
                .append("\n");
        }
        }

    /**
     * 辅助方法：插入场景分类数据
     */
    private static void insertScene(PreparedStatement pstmt, String sceneName, String subCategory, String systemPrompt, int sortOrder) throws SQLException {
        pstmt.setString(1, sceneName);
        pstmt.setString(2, subCategory);
        pstmt.setString(3, systemPrompt);
        pstmt.setInt(4, sortOrder);
        pstmt.executeUpdate();
    }
         
}