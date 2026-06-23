package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FileBlobDAO {
    // 保存文件二进制
    public void saveFile(String fileName, String suffix, byte[] bytes) {
        String sql = "INSERT INTO file_blob(file_name, file_suffix, file_bytes, file_size) VALUES (?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);
            pstmt.setString(2, suffix);
            pstmt.setBytes(3, bytes);
            pstmt.setInt(4, bytes.length);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 根据ID读取二进制字节，还原文件
    public byte[] getFileBytes(Integer fileId) {
        String sql = "SELECT file_bytes FROM file_blob WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("file_bytes");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
