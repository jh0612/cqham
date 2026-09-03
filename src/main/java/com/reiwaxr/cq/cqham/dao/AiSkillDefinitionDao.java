package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiSkillDefinition;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill定義DAO
 */
public class AiSkillDefinitionDao {

    public List<AiSkillDefinition> findAllActiveDefinitions() throws SQLException {
        List<AiSkillDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM ai_skill_definition WHERE deleted_flag = 0 ORDER BY sort_order, skill_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                definitions.add(mapRow(rs));
            }
        }
        return definitions;
    }

    public List<AiSkillDefinition> findEnabledDefinitions() throws SQLException {
        List<AiSkillDefinition> definitions = new ArrayList<>();
        String sql = "SELECT * FROM ai_skill_definition WHERE deleted_flag = 0 AND enabled = 1 ORDER BY sort_order, skill_name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                definitions.add(mapRow(rs));
            }
        }
        return definitions;
    }

    public AiSkillDefinition findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM ai_skill_definition WHERE id = ? AND deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int insert(AiSkillDefinition definition) throws SQLException {
        String sql = "INSERT INTO ai_skill_definition(skill_code, skill_name, description, category_name, tags, system_prompt, input_template, output_template, example_input, example_output, sort_order, enabled, built_in_flag, deleted_flag, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillMutationParams(pstmt, definition);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(AiSkillDefinition definition) throws SQLException {
        String sql = "UPDATE ai_skill_definition SET skill_name = ?, description = ?, category_name = ?, tags = ?, system_prompt = ?, input_template = ?, output_template = ?, example_input = ?, example_output = ?, sort_order = ?, enabled = ?, update_time = CURRENT_TIMESTAMP WHERE id = ? AND deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, definition.getSkillName());
            pstmt.setString(2, definition.getDescription());
            pstmt.setString(3, definition.getCategoryName());
            pstmt.setString(4, definition.getTags());
            pstmt.setString(5, definition.getSystemPrompt());
            pstmt.setString(6, definition.getInputTemplate());
            pstmt.setString(7, definition.getOutputTemplate());
            pstmt.setString(8, definition.getExampleInput());
            pstmt.setString(9, definition.getExampleOutput());
            pstmt.setInt(10, safeSortOrder(definition.getSortOrder()));
            pstmt.setInt(11, definition.isEnabled() ? 1 : 0);
            pstmt.setInt(12, definition.getId());
            pstmt.executeUpdate();
        }
    }

    public void logicalDelete(int id) throws SQLException {
        String sql = "UPDATE ai_skill_definition SET deleted_flag = 1, enabled = 0, update_time = CURRENT_TIMESTAMP WHERE id = ? AND built_in_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public int findNextSortOrder() throws SQLException {
        String sql = "SELECT COALESCE(MAX(sort_order), 0) + 1 AS next_sort FROM ai_skill_definition WHERE deleted_flag = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("next_sort") : 1;
        }
    }

    public boolean existsByName(String skillName, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM ai_skill_definition WHERE skill_name = ? AND deleted_flag = 0 AND (? IS NULL OR id <> ?) LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, skillName);
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

    private void fillMutationParams(PreparedStatement pstmt, AiSkillDefinition definition) throws SQLException {
        pstmt.setString(1, definition.getSkillCode());
        pstmt.setString(2, definition.getSkillName());
        pstmt.setString(3, definition.getDescription());
        pstmt.setString(4, definition.getCategoryName());
        pstmt.setString(5, definition.getTags());
        pstmt.setString(6, definition.getSystemPrompt());
        pstmt.setString(7, definition.getInputTemplate());
        pstmt.setString(8, definition.getOutputTemplate());
        pstmt.setString(9, definition.getExampleInput());
        pstmt.setString(10, definition.getExampleOutput());
        pstmt.setInt(11, safeSortOrder(definition.getSortOrder()));
        pstmt.setInt(12, definition.isEnabled() ? 1 : 0);
        pstmt.setInt(13, definition.isBuiltIn() ? 1 : 0);
    }

    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null || sortOrder <= 0 ? 1 : sortOrder;
    }

    private AiSkillDefinition mapRow(ResultSet rs) throws SQLException {
        AiSkillDefinition definition = new AiSkillDefinition();
        definition.setId(rs.getInt("id"));
        definition.setSkillCode(rs.getString("skill_code"));
        definition.setSkillName(rs.getString("skill_name"));
        definition.setDescription(rs.getString("description"));
        definition.setCategoryName(rs.getString("category_name"));
        definition.setTags(rs.getString("tags"));
        definition.setSystemPrompt(rs.getString("system_prompt"));
        definition.setInputTemplate(rs.getString("input_template"));
        definition.setOutputTemplate(rs.getString("output_template"));
        definition.setExampleInput(rs.getString("example_input"));
        definition.setExampleOutput(rs.getString("example_output"));
        definition.setSortOrder(rs.getInt("sort_order"));
        definition.setEnabled(rs.getInt("enabled") == 1);
        definition.setBuiltIn(rs.getInt("built_in_flag") == 1);
        definition.setDeleted(rs.getInt("deleted_flag") == 1);
        return definition;
    }
}