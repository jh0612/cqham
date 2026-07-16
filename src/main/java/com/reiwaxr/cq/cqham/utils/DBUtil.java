package com.reiwaxr.cq.cqham.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {
    // 数据库文件路径：项目根目录 data.db，不存在会自动创建
    private static final String DB_URL = "jdbc:sqlite:cqham.db";

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 程序启动时初始化：创建数据表，不存在则新建
     */
    public static void initTable() {

        // 翻译内容记录表
        String createTranslationTable = """
                CREATE TABLE IF NOT EXISTS translation_text (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_text TEXT NOT NULL,
                    target_text TEXT,
                    word_type TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createVocabularyTagTable = """
                CREATE TABLE IF NOT EXISTS vocabulary_tag (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tag_name TEXT NOT NULL UNIQUE,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createVocabularyTable = """
                CREATE TABLE IF NOT EXISTS vocabulary (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_text TEXT NOT NULL,
                    target_text TEXT NOT NULL,
                    tag_name TEXT NOT NULL,
                    delete_flg INTEGER NOT NULL DEFAULT 0,
                    deleted_at TIMESTAMP,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;
        // 无线电通联记录表
        String createRadioLogTable = """
            CREATE TABLE IF NOT EXISTS radio_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                call_sign TEXT,
                qth TEXT,
                frequency TEXT,
                mode TEXT,
                connect_date TEXT,
                connect_time TEXT,
                device TEXT,
                weather TEXT,
                signal_report TEXT,
                name TEXT,
                power TEXT,
                qsl_status TEXT,
                address TEXT,
                remark TEXT
            );
            """;

        // 两张示例表：文本存储表、文件二进制存储表
        String createDataTable = """
                CREATE TABLE IF NOT EXISTS export_text (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_title TEXT NOT NULL,
                    content TEXT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createFileTable = """
                CREATE TABLE IF NOT EXISTS file_blob (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT,
                    file_suffix TEXT,
                    file_bytes BLOB,
                    file_size INTEGER
                );
                """;

        // try-with-resources 自动关闭连接、Statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createRadioLogTable);
            stmt.execute(createDataTable);
            stmt.execute(createFileTable);
            stmt.execute(createTranslationTable);
            stmt.execute(createVocabularyTagTable);
            stmt.execute(createVocabularyTable);
            ensureVocabularyColumns(conn);
            seedVocabularyTags(conn);
            System.out.println("数据表初始化完成，无重复创建");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ensureVocabularyColumns(Connection conn) throws SQLException {
        if (!hasColumn(conn, "vocabulary", "tag_name")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN tag_name TEXT");
            }
        }
        if (hasColumn(conn, "vocabulary", "word_type")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE vocabulary SET tag_name = COALESCE(tag_name, word_type) WHERE tag_name IS NULL OR tag_name = ''");
            }
        }
        if (!hasColumn(conn, "vocabulary", "delete_flg")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN delete_flg INTEGER NOT NULL DEFAULT 0");
            }
        }
        if (!hasColumn(conn, "vocabulary", "deleted_at")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE vocabulary ADD COLUMN deleted_at TIMESTAMP");
            }
        }
    }

    private static boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void seedVocabularyTags(Connection conn) throws SQLException {
        String[] defaults = {"名词", "动词", "短句", "专业术语", "日常对话"};
        String insertSql = "INSERT OR IGNORE INTO vocabulary_tag(tag_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String tagName : defaults) {
                pstmt.setString(1, tagName);
                pstmt.executeUpdate();
            }
        }

        if (hasColumn(conn, "vocabulary", "tag_name")) {
            String syncSql = "INSERT OR IGNORE INTO vocabulary_tag(tag_name) SELECT DISTINCT tag_name FROM vocabulary WHERE tag_name IS NOT NULL AND tag_name <> ''";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(syncSql);
            }
        }
    }
}