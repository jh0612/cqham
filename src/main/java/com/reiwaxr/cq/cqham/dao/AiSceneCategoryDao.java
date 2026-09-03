package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiSceneCategory;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AI场景分类DAO
 */
public class AiSceneCategoryDao {

    /**
     * 查询所有场景名称（去重）
     */
    public List<String> findAllSceneNames() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT scene_name, MIN(custom_flag) AS custom_flag, MIN(sort_order) AS min_sort_order "
                + "FROM ai_scene_category GROUP BY scene_name ORDER BY custom_flag, min_sort_order, scene_name";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("scene_name"));
            }
        }
        return list;
    }

    /**
     * 根据场景名称查询子分类
     * @param sceneName 场景名称
     * @return AiSceneCategory列表
     */
    public List<AiSceneCategory> findBySceneName(String sceneName) throws SQLException {
        List<AiSceneCategory> list = new ArrayList<>();
        String sql = "SELECT * FROM ai_scene_category WHERE scene_name = ? ORDER BY sort_order, sub_category";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sceneName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 根据ID查询
     * @param id ID
     * @return AiSceneCategory对象
     */
    public AiSceneCategory findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM ai_scene_category WHERE id = ?";
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
     * 插入一条记录
     * @param category AiSceneCategory对象
     * @throws SQLException
     */
    public void insert(AiSceneCategory category) throws SQLException {
        String sql = "INSERT INTO ai_scene_category(scene_name, sub_category, system_prompt, sort_order, custom_flag, tags, remark, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillUpsertStatement(pstmt, category);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getInt(1));
                }
            }
        }
    }

    /**
     * 更新自定义场景
     * @param category 场景实体
     * @throws SQLException SQL异常
     */
    public void update(AiSceneCategory category) throws SQLException {
        String sql = "UPDATE ai_scene_category SET scene_name = ?, sub_category = ?, system_prompt = ?, sort_order = ?, "
                + "custom_flag = ?, tags = ?, remark = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            fillUpsertStatement(pstmt, category);
            pstmt.setInt(8, category.getId());
            pstmt.executeUpdate();
        }
    }

    /**
     * 删除指定场景
     * @param id 主键ID
     * @throws SQLException SQL异常
     */
    public void deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM ai_scene_category WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * 检查场景与子分类组合是否已存在
     * @param sceneName 场景名称
     * @param subCategory 子分类名称
     * @param excludeId 排除的ID
     * @return true: 已存在
     * @throws SQLException SQL异常
     */
    public boolean existsBySceneAndSubCategory(String sceneName, String subCategory, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM ai_scene_category WHERE scene_name = ? AND sub_category = ? AND (? IS NULL OR id <> ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sceneName);
            pstmt.setString(2, subCategory);
            if (excludeId == null) {
                pstmt.setNull(3, Types.INTEGER);
                pstmt.setNull(4, Types.INTEGER);
            } else {
                pstmt.setInt(3, excludeId);
                pstmt.setInt(4, excludeId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * 查询场景下一个排序值
     * @param sceneName 场景名称
     * @return 下一个排序值
     * @throws SQLException SQL异常
     */
    public int findNextSortOrder(String sceneName) throws SQLException {
        String sql = "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM ai_scene_category WHERE scene_name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sceneName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    /**
     * 填充新增/更新共用字段
     * @param pstmt SQL语句
     * @param category 场景实体
     * @throws SQLException SQL异常
     */
    private void fillUpsertStatement(PreparedStatement pstmt, AiSceneCategory category) throws SQLException {
        pstmt.setString(1, category.getSceneName());
        pstmt.setString(2, category.getSubCategory());
        pstmt.setString(3, category.getSystemPrompt());
        pstmt.setInt(4, category.getSortOrder() != null ? category.getSortOrder() : 0);
        pstmt.setInt(5, category.isCustom() ? 1 : 0);
        pstmt.setString(6, category.getTags());
        pstmt.setString(7, category.getRemark());
    }

    /**
     * 根据ID更新一条记录
     * @param rs ResultSet
     * @return AiSceneCategory对象
     * @throws SQLException
     */
    private AiSceneCategory mapRow(ResultSet rs) throws SQLException {
        AiSceneCategory c = new AiSceneCategory();
        c.setId(rs.getInt("id"));
        c.setSceneName(rs.getString("scene_name"));
        c.setSubCategory(rs.getString("sub_category"));
        c.setSystemPrompt(rs.getString("system_prompt"));
        c.setSortOrder(rs.getInt("sort_order"));
        c.setCustom(rs.getInt("custom_flag") == 1);
        c.setTags(rs.getString("tags"));
        c.setRemark(rs.getString("remark"));
        c.setCreateTime(rs.getString("create_time"));
        c.setUpdateTime(rs.getString("update_time"));
        return c;
    }
}