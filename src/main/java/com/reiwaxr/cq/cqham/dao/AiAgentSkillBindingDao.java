package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiAgentSkillBinding;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent Skill绑定DAO
 */
public class AiAgentSkillBindingDao {

    public List<AiAgentSkillBinding> findBindingsByAgentId(int agentId) throws SQLException {
        List<AiAgentSkillBinding> bindings = new ArrayList<>();
        String sql = "SELECT b.id, b.agent_id, b.template_step_no, b.binding_order, b.skill_id, b.enabled, s.skill_code, s.skill_name "
                + "FROM ai_agent_skill_binding b "
                + "JOIN ai_skill_definition s ON s.id = b.skill_id "
                + "WHERE b.agent_id = ? AND b.enabled = 1 AND s.deleted_flag = 0 AND s.enabled = 1 "
                + "ORDER BY b.template_step_no, b.binding_order";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, agentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bindings.add(mapRow(rs));
                }
            }
        }
        return bindings;
    }

    public void replaceBindings(int agentId, List<AiAgentSkillBinding> bindings) throws SQLException {
        String deleteSql = "DELETE FROM ai_agent_skill_binding WHERE agent_id = ?";
        String insertSql = "INSERT INTO ai_agent_skill_binding(agent_id, template_step_no, binding_order, skill_id, enabled, update_time) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            try {
                deleteStmt.setInt(1, agentId);
                deleteStmt.executeUpdate();
                for (AiAgentSkillBinding binding : bindings) {
                    insertStmt.setInt(1, agentId);
                    insertStmt.setInt(2, binding.getTemplateStepNo());
                    insertStmt.setInt(3, binding.getBindingOrder());
                    insertStmt.setInt(4, binding.getSkillId());
                    insertStmt.setInt(5, binding.isEnabled() ? 1 : 0);
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

    public void deleteBindingsByAgentId(int agentId) throws SQLException {
        String sql = "DELETE FROM ai_agent_skill_binding WHERE agent_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, agentId);
            pstmt.executeUpdate();
        }
    }

    private AiAgentSkillBinding mapRow(ResultSet rs) throws SQLException {
        AiAgentSkillBinding binding = new AiAgentSkillBinding();
        binding.setId(rs.getInt("id"));
        binding.setAgentId(rs.getInt("agent_id"));
        binding.setTemplateStepNo(rs.getInt("template_step_no"));
        binding.setBindingOrder(rs.getInt("binding_order"));
        binding.setSkillId(rs.getInt("skill_id"));
        binding.setEnabled(rs.getInt("enabled") == 1);
        binding.setSkillCode(rs.getString("skill_code"));
        binding.setSkillName(rs.getString("skill_name"));
        return binding;
    }
}