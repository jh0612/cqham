package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.Vocabulary;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VocabularyDao {
    // 新增词汇
    public void addVocab(Vocabulary vocab) throws SQLException {
        String sql = "INSERT INTO vocabulary(source_text, target_text, tag_name, delete_flg) VALUES (?,?,?,0)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vocab.getSourceText());
            pstmt.setString(2, vocab.getTargetText());
            pstmt.setString(3, vocab.getTagName());
            pstmt.executeUpdate();
        }
    }
    // 根据分类查询词汇
    public List<Vocabulary> listByTag(String tagName) throws SQLException {
        List<Vocabulary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM vocabulary WHERE delete_flg = 0");
        if (tagName != null && !tagName.isBlank()) {
            sql.append(" AND tag_name = ?");
        }
        sql.append(" ORDER BY create_time DESC");
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            if (tagName != null && !tagName.isBlank()) {
                pstmt.setString(1, tagName);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(convertRow(rs));
            }

        }
        return list;
    }
    // 删除词汇
    public void logicalDeleteById(Integer id) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            purgeDeletedOlderThanOneMonth(conn);
            String sql = "UPDATE vocabulary SET delete_flg = 1, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND delete_flg = 0";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * 根据标签名称统计未删除的词汇数量
     * @param tagName 标签名称
     * @return 未删除的词汇数量
     * @throws SQLException
     */
    public int countActiveByTag(String tagName) throws SQLException {
        String sql = "SELECT COUNT(1) FROM vocabulary WHERE tag_name = ? AND delete_flg = 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * 重命名标签
     * @param oldTagName 旧标签名称
     * @param newTagName 新标签名称
     * @throws SQLException
     */
    public void renameTag(String oldTagName, String newTagName) throws SQLException {
        String sql = "UPDATE vocabulary SET tag_name = ? WHERE tag_name = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTagName);
            pstmt.setString(2, oldTagName);
            pstmt.executeUpdate();
        }
    }

    /**
     * 清理已删除且创建时间超过一个月的词汇
     * @param conn 数据库连接
     * @throws SQLException
     */
    private void purgeDeletedOlderThanOneMonth(Connection conn) throws SQLException {
        String sql = "DELETE FROM vocabulary WHERE delete_flg = 1 AND create_time <= datetime('now', '-1 month')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    /**
     * 将ResultSet中的一行数据转换为Vocabulary对象
     * @param rs ResultSet
     * @return Vocabulary对象
     * @throws SQLException
     */
    private Vocabulary convertRow(ResultSet rs) throws SQLException {
        Vocabulary vocab = new Vocabulary();
        vocab.setId(rs.getInt("id"));
        vocab.setSourceText(rs.getString("source_text"));
        vocab.setTargetText(rs.getString("target_text"));
        vocab.setTagName(readTagName(rs));
        vocab.setDeleteFlg(rs.getInt("delete_flg"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            LocalDateTime createTimeAtz = createTime.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
            vocab.setCreateTime(createTimeAtz);
        }
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            vocab.setDeletedAt(deletedAt.toLocalDateTime());
        }
        return vocab;
    }

    /**
     * 读取标签名称，如果标签名称为空，则尝试读取word_type字段
     * @param rs ResultSet
     * @return 标签名称
     * @throws SQLException
     */
    private String readTagName(ResultSet rs) throws SQLException {
        String tagName = rs.getString("tag_name");
        if (tagName == null || tagName.isBlank()) {
            try {
                tagName = rs.getString("word_type");
            } catch (SQLException ignore) {
                return "";
            }
        }
        return tagName;
    }
}
