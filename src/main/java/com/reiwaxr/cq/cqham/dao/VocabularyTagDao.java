package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.VocabularyTag;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class VocabularyTagDao {
    /**
     * 查询所有词汇标签
     * @return List<VocabularyTag> 
     * @throws SQLException
     */
    public List<VocabularyTag> findAll() throws SQLException {
        List<VocabularyTag> result = new ArrayList<>();
        String sql = "SELECT * FROM vocabulary_tag ORDER BY create_time ASC, id ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(convertRow(rs));
            }
        }
        return result;
    }

    /**
     * 查询所有词汇标签名称
     * @return List<String> 
     * @throws SQLException
     */
    public List<String> findAllTagNames() throws SQLException {
        List<String> result = new ArrayList<>();
        for (VocabularyTag tag : findAll()) {
            result.add(tag.getTagName());
        }
        return result;
    }

    /**
     * 插入新的词汇标签
     * @param tagName 词汇标签名称
     * @throws SQLException
     */
    public void insert(String tagName) throws SQLException {
        String sql = "INSERT INTO vocabulary_tag(tag_name) VALUES (?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            pstmt.executeUpdate();
        }
    }

    /**
     * 重命名词汇标签
     * @param oldTagName 旧的词汇标签名称
     * @param newTagName 新的词汇标签名称
     * @throws SQLException
     */
    public void rename(String oldTagName, String newTagName) throws SQLException {
        String sql = "UPDATE vocabulary_tag SET tag_name = ?, update_time = CURRENT_TIMESTAMP WHERE tag_name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTagName);
            pstmt.setString(2, oldTagName);
            pstmt.executeUpdate();
        }
    }

    /**
     * 删除词汇标签
     * @param tagName 词汇标签名称
     * @throws SQLException
     */
    public void delete(String tagName) throws SQLException {
        String sql = "DELETE FROM vocabulary_tag WHERE tag_name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            pstmt.executeUpdate();
        }
    }

    /**
     * 将ResultSet中的一行数据转换为VocabularyTag对象
     * @param rs ResultSet
     * @return VocabularyTag
     * @throws SQLException
     */
    private VocabularyTag convertRow(ResultSet rs) throws SQLException {
        VocabularyTag tag = new VocabularyTag();
        tag.setId(rs.getInt("id"));
        tag.setTagName(rs.getString("tag_name"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            tag.setCreateTime(createTime.toLocalDateTime());
        }
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            tag.setUpdateTime(updateTime.toLocalDateTime());
        }
        return tag;
    }
}