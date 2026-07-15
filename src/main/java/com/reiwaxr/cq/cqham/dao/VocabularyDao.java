package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.Vocabulary;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VocabularyDao {
    // 新增词汇
    public void addVocab(Vocabulary vocab) throws SQLException {
        String sql = "INSERT INTO vocabulary(source_text, target_text, word_type) VALUES (?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vocab.getSourceText());
            pstmt.setString(2, vocab.getTargetText());
            pstmt.setString(3, vocab.getWordType());
            pstmt.executeUpdate();
        }
    }

    // 根据分类查询词汇
    public List<Vocabulary> listByType(String type) throws SQLException {
        List<Vocabulary> list = new ArrayList<>();
        String sql = "SELECT * FROM vocabulary WHERE word_type = ? ORDER BY create_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Vocabulary v = new Vocabulary();
                v.setId(rs.getInt("id"));
                v.setSourceText(rs.getString("source_text"));
                v.setTargetText(rs.getString("target_text"));
                v.setWordType(rs.getString("word_type"));
                list.add(v);
            }
        }
        return list;
    }

    // 删除词汇
    public void deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM vocabulary WHERE id=?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
