package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiAgentDefinition;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent定義DAO
 */
public class AiAgentDefinitionDao {

    /**
     * 有効なAgent定義を取得する
     * @return Agent定義一覧
     * @throws SQLException SQL例外
     */
    public List<AiAgentDefinition> findEnabledDefinitions() throws SQLException {
        List<AiAgentDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM ai_agent_definition WHERE enabled = 1 AND deleted_flag = 0 ORDER BY sort_order, agent_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                definitions.add(mapRow(rs));
            }
        }
        return definitions;
    }

    /**
     * 获取全部未删除的Agent定义
     */
    public List<AiAgentDefinition> findAllActiveDefinitions() throws SQLException {
        List<AiAgentDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM ai_agent_definition WHERE deleted_flag = 0 ORDER BY sort_order, agent_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                definitions.add(mapRow(rs));
            }
        }
        return definitions;
    }

    /**
     * Agent定義をコードで取得する
     * @param agentCode Agentコード
     * @return Agent定義
     * @throws SQLException SQL例外
     */
    public AiAgentDefinition findByAgentCode(String agentCode) throws SQLException {
        String sql = "SELECT * FROM ai_agent_definition WHERE agent_code = ? AND deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, agentCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 根据ID获取Agent定义
     */
    public AiAgentDefinition findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM ai_agent_definition WHERE id = ? AND deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 插入自定义Agent定义
     */
    public int insert(AiAgentDefinition definition) throws SQLException {
        String sql = "INSERT INTO ai_agent_definition(agent_code, agent_name, description, planner_prompt, execution_prompt, template_code, final_summary_enabled, sort_order, enabled, built_in_flag, deleted_flag, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillMutationParams(pstmt, definition);
            pstmt.setInt(9, definition.isEnabled() ? 1 : 0);
            pstmt.setInt(10, definition.isBuiltIn() ? 1 : 0);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /**
     * 更新自定义Agent定义
     */
    public void update(AiAgentDefinition definition) throws SQLException {
        String sql = "UPDATE ai_agent_definition SET agent_name = ?, description = ?, planner_prompt = ?, execution_prompt = ?, template_code = ?, final_summary_enabled = ?, sort_order = ?, enabled = ?, update_time = CURRENT_TIMESTAMP WHERE id = ? AND deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, definition.getAgentName());
            pstmt.setString(2, definition.getDescription());
            pstmt.setString(3, definition.getPlannerPrompt());
            pstmt.setString(4, definition.getExecutionPrompt());
            pstmt.setString(5, definition.getTemplateCode());
            pstmt.setInt(6, definition.isFinalSummaryEnabled() ? 1 : 0);
            pstmt.setInt(7, safeSortOrder(definition.getSortOrder()));
            pstmt.setInt(8, definition.isEnabled() ? 1 : 0);
            pstmt.setInt(9, definition.getId());
            pstmt.executeUpdate();
        }
    }

    /**
     * 逻辑删除Agent
     */
    public void logicalDelete(int id) throws SQLException {
        String sql = "UPDATE ai_agent_definition SET deleted_flag = 1, enabled = 0, update_time = CURRENT_TIMESTAMP WHERE id = ? AND built_in_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * 获取下一个排序值
     */
    public int findNextSortOrder() throws SQLException {
        String sql = "SELECT COALESCE(MAX(sort_order), 0) + 1 AS next_sort FROM ai_agent_definition WHERE deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("next_sort") : 1;
        }
    }

    /**
     * 是否存在同名Agent
     */
    public boolean existsByName(String agentName, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM ai_agent_definition WHERE agent_name = ? AND deleted_flag = 0 AND (? IS NULL OR id <> ?) LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, agentName);
            if (excludeId == null) {
                pstmt.setObject(2, null);
                pstmt.setObject(3, null);
            } else {
                pstmt.setInt(2, excludeId);
                pstmt.setInt(3, excludeId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void fillMutationParams(PreparedStatement pstmt, AiAgentDefinition definition) throws SQLException {
        pstmt.setString(1, definition.getAgentCode());
        pstmt.setString(2, definition.getAgentName());
        pstmt.setString(3, definition.getDescription());
        pstmt.setString(4, definition.getPlannerPrompt());
        pstmt.setString(5, definition.getExecutionPrompt());
        pstmt.setString(6, definition.getTemplateCode());
        pstmt.setInt(7, definition.isFinalSummaryEnabled() ? 1 : 0);
        pstmt.setInt(8, safeSortOrder(definition.getSortOrder()));
    }

    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null || sortOrder <= 0 ? 1 : sortOrder;
    }

    private AiAgentDefinition mapRow(ResultSet rs) throws SQLException {
        AiAgentDefinition definition = new AiAgentDefinition();
        definition.setId(rs.getInt("id"));
        definition.setAgentCode(rs.getString("agent_code"));
        definition.setAgentName(rs.getString("agent_name"));
        definition.setDescription(rs.getString("description"));
        definition.setPlannerPrompt(rs.getString("planner_prompt"));
        definition.setExecutionPrompt(rs.getString("execution_prompt"));
        definition.setTemplateCode(rs.getString("template_code"));
        definition.setSortOrder(rs.getInt("sort_order"));
        definition.setEnabled(rs.getInt("enabled") == 1);
        definition.setFinalSummaryEnabled(rs.getInt("final_summary_enabled") == 1);
        definition.setBuiltIn(rs.getInt("built_in_flag") == 1);
        definition.setDeleted(rs.getInt("deleted_flag") == 1);
        return definition;
    }
}