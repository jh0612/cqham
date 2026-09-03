package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiAgentTemplate;
import com.reiwaxr.cq.cqham.entity.AiAgentTemplateStep;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent固定模板DAO
 */
public class AiAgentTemplateDao {

    /**
     * 获取所有启用模板
     */
    public List<AiAgentTemplate> findEnabledTemplates() throws SQLException {
        List<AiAgentTemplate> templates = new ArrayList<>();
        String sql = "SELECT * FROM ai_agent_template WHERE enabled = 1 ORDER BY sort_order, template_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                templates.add(mapTemplate(rs));
            }
        }
        return templates;
    }

    /**
     * 根据模板代码查询模板
     */
    public AiAgentTemplate findByTemplateCode(String templateCode) throws SQLException {
        String sql = "SELECT * FROM ai_agent_template WHERE template_code = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, templateCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapTemplate(rs);
                }
            }
        }
        return null;
    }

    /**
     * 插入自定义模板
     */
    public int insertTemplate(AiAgentTemplate template) throws SQLException {
        String sql = "INSERT INTO ai_agent_template(template_code, template_name, description, sort_order, enabled, built_in_flag, update_time) VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, template.getTemplateCode());
            pstmt.setString(2, template.getTemplateName());
            pstmt.setString(3, template.getDescription());
            pstmt.setInt(4, safeInt(template.getSortOrder(), 999));
            pstmt.setInt(5, template.isEnabled() ? 1 : 0);
            return pstmt.executeUpdate();
        }
    }

    /**
     * 查询模板下一个排序值
     */
    public int findNextSortOrder() throws SQLException {
        String sql = "SELECT COALESCE(MAX(sort_order), 0) + 1 AS next_sort FROM ai_agent_template";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("next_sort") : 1;
        }
    }

    /**
     * 根据模板代码读取步骤
     */
    public List<AiAgentTemplateStep> findEnabledStepsByTemplateCode(String templateCode) throws SQLException {
        List<AiAgentTemplateStep> steps = new ArrayList<>();
        String sql = "SELECT * FROM ai_agent_template_step WHERE template_code = ? AND enabled = 1 ORDER BY step_no, sort_order";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, templateCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    steps.add(mapStep(rs));
                }
            }
        }
        return steps;
    }

    /**
     * 替换模板步骤
     */
    public void replaceTemplateSteps(String templateCode, List<AiAgentTemplateStep> steps) throws SQLException {
        String ensureTemplateSql = "INSERT OR IGNORE INTO ai_agent_template(template_code, template_name, description, sort_order, enabled, built_in_flag, update_time) VALUES (?, ?, ?, ?, 1, 0, CURRENT_TIMESTAMP)";
        String deleteSql = "DELETE FROM ai_agent_template_step WHERE template_code = ?";
        String insertSql = "INSERT INTO ai_agent_template_step(template_code, step_no, skill_code, step_name, purpose, expected_output, sort_order, enabled, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ensureStmt = conn.prepareStatement(ensureTemplateSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            try {
                ensureStmt.setString(1, templateCode);
                ensureStmt.setString(2, templateCode);
                ensureStmt.setString(3, "自动创建的模板");
                ensureStmt.setInt(4, 999);
                ensureStmt.executeUpdate();

                deleteStmt.setString(1, templateCode);
                deleteStmt.executeUpdate();

                for (AiAgentTemplateStep step : steps) {
                    insertStmt.setString(1, templateCode);
                    insertStmt.setInt(2, safeInt(step.getStepNo(), 1));
                    insertStmt.setString(3, step.getSkillCode());
                    insertStmt.setString(4, step.getStepName());
                    insertStmt.setString(5, step.getPurpose());
                    insertStmt.setString(6, step.getExpectedOutput());
                    insertStmt.setInt(7, safeInt(step.getSortOrder(), safeInt(step.getStepNo(), 1)));
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private AiAgentTemplate mapTemplate(ResultSet rs) throws SQLException {
        AiAgentTemplate template = new AiAgentTemplate();
        template.setId(rs.getInt("id"));
        template.setTemplateCode(rs.getString("template_code"));
        template.setTemplateName(rs.getString("template_name"));
        template.setDescription(rs.getString("description"));
        template.setSortOrder(rs.getInt("sort_order"));
        template.setEnabled(rs.getInt("enabled") == 1);
        template.setBuiltIn(rs.getInt("built_in_flag") == 1);
        return template;
    }

    private AiAgentTemplateStep mapStep(ResultSet rs) throws SQLException {
        AiAgentTemplateStep step = new AiAgentTemplateStep();
        step.setId(rs.getInt("id"));
        step.setTemplateCode(rs.getString("template_code"));
        step.setStepNo(rs.getInt("step_no"));
        step.setSkillCode(rs.getString("skill_code"));
        step.setStepName(rs.getString("step_name"));
        step.setPurpose(rs.getString("purpose"));
        step.setExpectedOutput(rs.getString("expected_output"));
        step.setSortOrder(rs.getInt("sort_order"));
        step.setEnabled(rs.getInt("enabled") == 1);
        return step;
    }
}
