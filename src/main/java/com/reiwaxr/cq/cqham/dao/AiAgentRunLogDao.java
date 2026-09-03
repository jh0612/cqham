package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.AiAgentRunRecord;
import com.reiwaxr.cq.cqham.entity.AiAgentStepLog;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent実行ログDAO
 */
public class AiAgentRunLogDao {

    /**
     * 実行ログを作成する
     * @param runId 実行ID
     * @param agentCode Agentコード
     * @param userGoal ユーザー目標
     * @param contextSummary コンテキスト概要
     * @param planJson 計画JSON
     * @param status 状態
     * @throws SQLException SQL例外
     */
    public void insertRunLog(String runId, String agentCode, String userGoal, String contextSummary, String planJson,
                             String requestJson, String status, int completedStepCount, int currentStepNo,
                             String resumeSourceRunId, String resumeMode) throws SQLException {
        String sql = "INSERT INTO ai_agent_run_log(run_id, agent_code, user_goal, context_summary, plan_json, request_json, status, completed_step_count, current_step_no, resume_source_run_id, resume_mode, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, runId);
            pstmt.setString(2, agentCode);
            pstmt.setString(3, userGoal);
            pstmt.setString(4, contextSummary);
            pstmt.setString(5, planJson);
            pstmt.setString(6, requestJson);
            pstmt.setString(7, status);
            pstmt.setInt(8, completedStepCount);
            pstmt.setInt(9, currentStepNo);
            pstmt.setString(10, resumeSourceRunId);
            pstmt.setString(11, resumeMode);
            pstmt.executeUpdate();
        }
    }

    /**
     * 実行ログ状態を更新する
     * @param runId 実行ID
     * @param status 状態
     * @param finalResult 最終結果
     * @param errorMessage エラー内容
     * @param planJson 計画JSON
     * @throws SQLException SQL例外
     */
    public void updateRunLog(String runId, String status, String finalResult, String errorMessage, String planJson,
                             String requestJson, int completedStepCount, int currentStepNo) throws SQLException {
        String sql = "UPDATE ai_agent_run_log SET status = ?, final_result = ?, error_message = ?, plan_json = ?, request_json = COALESCE(?, request_json), completed_step_count = ?, current_step_no = ?, update_time = CURRENT_TIMESTAMP WHERE run_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, finalResult);
            pstmt.setString(3, errorMessage);
            pstmt.setString(4, planJson);
            pstmt.setString(5, requestJson);
            pstmt.setInt(6, completedStepCount);
            pstmt.setInt(7, currentStepNo);
            pstmt.setString(8, runId);
            pstmt.executeUpdate();
        }
    }

    /**
     * 统计可恢复运行数量
     */
    public int countRecoverableRuns() throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM ai_agent_run_log WHERE status IN ('WAITING_CONFIRMATION', 'PLANNED', 'RUNNING', 'FAILED', 'CANCELLED')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("cnt") : 0;
        }
    }

    /**
     * 获取最近可恢复运行
     */
    public List<AiAgentRunRecord> findRecoverableRuns(int limit) throws SQLException {
        List<AiAgentRunRecord> records = new ArrayList<>();
        String sql = "SELECT r.*, "
                + "(SELECT step_name FROM ai_agent_step_log s WHERE s.run_id = r.run_id ORDER BY step_no DESC, id DESC LIMIT 1) AS current_step_name, "
                + "(SELECT COALESCE(error_message, '') FROM ai_agent_step_log s WHERE s.run_id = r.run_id AND COALESCE(error_message, '') <> '' ORDER BY step_no DESC, id DESC LIMIT 1) AS recent_error "
                + "FROM ai_agent_run_log r "
                + "WHERE r.status IN ('WAITING_CONFIRMATION', 'PLANNED', 'RUNNING', 'FAILED', 'CANCELLED') "
                + "ORDER BY r.update_time DESC, r.id DESC LIMIT ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRunRow(rs));
                }
            }
        }
        return records;
    }

    /**
     * 按运行ID获取记录
     */
    public AiAgentRunRecord findByRunId(String runId) throws SQLException {
        String sql = "SELECT r.*, "
                + "(SELECT step_name FROM ai_agent_step_log s WHERE s.run_id = r.run_id ORDER BY step_no DESC, id DESC LIMIT 1) AS current_step_name, "
                + "(SELECT COALESCE(error_message, '') FROM ai_agent_step_log s WHERE s.run_id = r.run_id AND COALESCE(error_message, '') <> '' ORDER BY step_no DESC, id DESC LIMIT 1) AS recent_error "
                + "FROM ai_agent_run_log r WHERE r.run_id = ? LIMIT 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRunRow(rs) : null;
            }
        }
    }

    /**
     * 获取运行步骤日志
     */
    public List<AiAgentStepLog> findStepLogs(String runId) throws SQLException {
        List<AiAgentStepLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ai_agent_step_log WHERE run_id = ? ORDER BY step_no, id";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, runId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapStepRow(rs));
                }
            }
        }
        return logs;
    }

    /**
     * 删除运行记录及其步骤日志
     */
    public void deleteRunLog(String runId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteSteps = conn.prepareStatement("DELETE FROM ai_agent_step_log WHERE run_id = ?");
                 PreparedStatement deleteRun = conn.prepareStatement("DELETE FROM ai_agent_run_log WHERE run_id = ?")) {
                deleteSteps.setString(1, runId);
                deleteSteps.executeUpdate();

                deleteRun.setString(1, runId);
                deleteRun.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * ステップログを作成する
     * @param runId 実行ID
     * @param stepNo ステップ番号
     * @param stepName ステップ名
     * @param skillCode 擬似スキルコード
     * @param stepInput 入力
     * @param status 状態
     * @return ステップログID
     * @throws SQLException SQL例外
     */
    public long insertStepLog(String runId, int stepNo, String stepName, String skillCode, String stepInput, String status) throws SQLException {
        String sql = "INSERT INTO ai_agent_step_log(run_id, step_no, step_name, skill_code, step_input, status, update_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, runId);
            pstmt.setInt(2, stepNo);
            pstmt.setString(3, stepName);
            pstmt.setString(4, skillCode);
            pstmt.setString(5, stepInput);
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        }
    }

    /**
     * ステップログを更新する
     * @param id ステップログID
     * @param status 状態
     * @param stepOutput 出力
     * @param errorMessage エラー内容
     * @throws SQLException SQL例外
     */
    public void updateStepLog(long id, String status, String stepOutput, String errorMessage) throws SQLException {
        String sql = "UPDATE ai_agent_step_log SET status = ?, step_output = ?, error_message = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, stepOutput);
            pstmt.setString(3, errorMessage);
            pstmt.setLong(4, id);
            pstmt.executeUpdate();
        }
    }

    private AiAgentRunRecord mapRunRow(ResultSet rs) throws SQLException {
        AiAgentRunRecord record = new AiAgentRunRecord();
        record.setId(rs.getInt("id"));
        record.setRunId(rs.getString("run_id"));
        record.setAgentCode(rs.getString("agent_code"));
        record.setUserGoal(rs.getString("user_goal"));
        record.setContextSummary(rs.getString("context_summary"));
        record.setPlanJson(rs.getString("plan_json"));
        record.setRequestJson(rs.getString("request_json"));
        record.setStatus(rs.getString("status"));
        record.setFinalResult(rs.getString("final_result"));
        record.setErrorMessage(rs.getString("error_message"));
        record.setCompletedStepCount(rs.getInt("completed_step_count"));
        record.setCurrentStepNo(rs.getInt("current_step_no"));
        record.setResumeSourceRunId(rs.getString("resume_source_run_id"));
        record.setResumeMode(rs.getString("resume_mode"));
        record.setCurrentStepName(rs.getString("current_step_name"));
        record.setRecentError(rs.getString("recent_error"));
        record.setCreateTime(rs.getString("create_time"));
        record.setUpdateTime(rs.getString("update_time"));
        return record;
    }

    private AiAgentStepLog mapStepRow(ResultSet rs) throws SQLException {
        AiAgentStepLog log = new AiAgentStepLog();
        log.setId(rs.getLong("id"));
        log.setRunId(rs.getString("run_id"));
        log.setStepNo(rs.getInt("step_no"));
        log.setStepName(rs.getString("step_name"));
        log.setSkillCode(rs.getString("skill_code"));
        log.setStepInput(rs.getString("step_input"));
        log.setStepOutput(rs.getString("step_output"));
        log.setStatus(rs.getString("status"));
        log.setErrorMessage(rs.getString("error_message"));
        return log;
    }
}