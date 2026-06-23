package com.reiwaxr.cq.cqham.dao;


import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// 对应 export_text 表操作
public class TextDataDAO {

    // 新增一条文本记录
    public void insertText(String title, String content) {
        String sql = "INSERT INTO export_text(file_title, content) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 根据ID查询文本
    public String getContentById(Integer id) {
        String sql = "SELECT content FROM export_text WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("content");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 查询全部标题+ID（用于列表展示）
    public List<String> getAllTitle() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, file_title FROM export_text ORDER BY create_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Integer id = rs.getInt("id");
                String title = rs.getString("file_title");
                list.add(id + " - " + title);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
