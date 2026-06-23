package com.reiwaxr.cq.cqham.dao;

import com.reiwaxr.cq.cqham.entity.RadioLog;
import com.reiwaxr.cq.cqham.utils.DBUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RadioLogDAO {

    /**
     * 0.批量插入多条通联记录
     * @param logList 通联记录集合
     */
    public void batchInsert(List<RadioLog> logList) {
        if (logList == null || logList.isEmpty()) {
            return;
        }
        String sql = """
            INSERT INTO radio_log(
                call_sign, qth, frequency, mode, connect_date, connect_time,
                device, weather, signal_report, name, power, qsl_status, address, remark
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 关闭自动提交，批量事务
            conn.setAutoCommit(false);

            for (RadioLog log : logList) {
                pstmt.setString(1, log.getCallSign());
                pstmt.setString(2, log.getQth());
                pstmt.setString(3, log.getFrequency());
                pstmt.setString(4, log.getMode());
                pstmt.setString(5, log.getConnectDate() == null ? null : log.getConnectDate().toString());
                pstmt.setString(6, log.getConnectTime());
                pstmt.setString(7, log.getDevice());
                pstmt.setString(8, log.getWeather());
                pstmt.setString(9, log.getSignalReport());
                pstmt.setString(10, log.getName());
                pstmt.setString(11, log.getPower());
                pstmt.setString(12, log.getQslStatus());
                pstmt.setString(13, log.getAddress());
                pstmt.setString(14, log.getRemark());

                pstmt.addBatch(); // 加入批处理队列
            }

            pstmt.executeBatch(); // 一次性执行所有SQL
            conn.commit(); // 统一提交事务

        } catch (SQLException e) {
            e.printStackTrace();
            // 出错回滚，避免部分插入
            try (Connection conn = DBUtil.getConnection()) {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // 1. 新增一条通联记录
    public void insert(RadioLog log) {
        String sql = """
                INSERT INTO radio_log(
                    call_sign, qth, frequency, mode, connect_date, connect_time,
                    device, weather, signal_report, name, power, qsl_status, address, remark
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, log.getCallSign());
            pstmt.setString(2, log.getQth());
            pstmt.setString(3, log.getFrequency());
            pstmt.setString(4, log.getMode());
            // LocalDate 转字符串存入
            pstmt.setString(5, log.getConnectDate() == null ? null : log.getConnectDate().toString());
            pstmt.setString(6, log.getConnectTime());
            pstmt.setString(7, log.getDevice());
            pstmt.setString(8, log.getWeather());
            pstmt.setString(9, log.getSignalReport());
            pstmt.setString(10, log.getName());
            pstmt.setString(11, log.getPower());
            pstmt.setString(12, log.getQslStatus());
            pstmt.setString(13, log.getAddress());
            pstmt.setString(14, log.getRemark());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 2. 根据ID单条查询
    public RadioLog findById(Long id) {
        String sql = "SELECT * FROM radio_log WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return convertRsToEntity(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 3. 查询全部记录（倒序，最新在前）
    public List<RadioLog> findAll() {
        List<RadioLog> list = new ArrayList<>();
        String sql = "SELECT * FROM radio_log ORDER BY connect_date DESC, connect_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(convertRsToEntity(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 4. 修改记录
    public void update(RadioLog log) {
        String sql = """
                UPDATE radio_log SET
                    call_sign=?, qth=?, frequency=?, mode=?, connect_date=?, connect_time=?,
                    device=?, weather=?, signal_report=?, name=?, power=?, qsl_status=?, address=?, remark=?
                WHERE id=?
                """;
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, log.getCallSign());
            pstmt.setString(2, log.getQth());
            pstmt.setString(3, log.getFrequency());
            pstmt.setString(4, log.getMode());
            pstmt.setString(5, log.getConnectDate() == null ? null : log.getConnectDate().toString());
            pstmt.setString(6, log.getConnectTime());
            pstmt.setString(7, log.getDevice());
            pstmt.setString(8, log.getWeather());
            pstmt.setString(9, log.getSignalReport());
            pstmt.setString(10, log.getName());
            pstmt.setString(11, log.getPower());
            pstmt.setString(12, log.getQslStatus());
            pstmt.setString(13, log.getAddress());
            pstmt.setString(14, log.getRemark());
            pstmt.setLong(15, log.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 5. 根据ID删除
    public void deleteById(Long id) {
        String sql = "DELETE FROM radio_log WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ResultSet 转 RadioLog 实体（抽取公共转换方法）
    private RadioLog convertRsToEntity(ResultSet rs) throws SQLException {
        RadioLog log = new RadioLog();
        log.setId(rs.getLong("id"));
        log.setCallSign(rs.getString("call_sign"));
        log.setQth(rs.getString("qth"));
        log.setFrequency(rs.getString("frequency"));
        log.setMode(rs.getString("mode"));

        // 字符串转回 LocalDate
        String dateStr = rs.getString("connect_date");
        if (dateStr != null && !dateStr.isBlank()) {
            log.setConnectDate(LocalDate.parse(dateStr));
        }

        log.setConnectTime(rs.getString("connect_time"));
        log.setDevice(rs.getString("device"));
        log.setWeather(rs.getString("weather"));
        log.setSignalReport(rs.getString("signal_report"));
        log.setName(rs.getString("name"));
        log.setPower(rs.getString("power"));
        log.setQslStatus(rs.getString("qsl_status"));
        log.setAddress(rs.getString("address"));
        log.setRemark(rs.getString("remark"));
        return log;
    }

    // ===================== 分页多条件组合查询 =====================
    /**
     * 多条件复合分页查询
     * @param callSigns 呼号数组 精确匹配
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @param weather 天气
     * @param freqs 频率数组
     * @param mode 通联模式
     * @param qslStatus QSL状态
     * @param pageIndex 当前页码 从0开始
     * @param pageSize 每页条数 固定20
     * @return 分页数据列表
     */
    public List<RadioLog> queryByCondition(
            List<String> callSigns,
            LocalDate startDate,
            LocalDate endDate,
            String weather,
            List<String> freqs,
            String mode,
            String qslStatus,
            int pageIndex,
            int pageSize
    ) {
        List<RadioLog> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM radio_log WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // 1.呼号精确多值
        if(callSigns != null && !callSigns.isEmpty()){
            String placeHolder = String.join(",", Collections.nCopies(callSigns.size(), "?"));
            sql.append(" AND call_sign IN (").append(placeHolder).append(") ");
            params.addAll(callSigns);
        }
        // 2.日期区间
        if(startDate != null){
            sql.append(" AND connect_date >= ? ");
            params.add(startDate.toString());
        }
        if(endDate != null){
            sql.append(" AND connect_date <= ? ");
            params.add(endDate.toString());
        }
        //3.天气
        if(weather != null && !weather.isBlank()){
            sql.append(" AND weather = ? ");
            params.add(weather);
        }
        //4.频率多值
        if(freqs != null && !freqs.isEmpty()){
            String placeHolder = String.join(",", Collections.nCopies(freqs.size(), "?"));
            sql.append(" AND frequency IN (").append(placeHolder).append(") ");
            params.addAll(freqs);
        }
        //5.通联模式
        if(mode != null && !mode.isBlank()){
            sql.append(" AND mode = ? ");
            params.add(mode);
        }
        //6.QSL状态
        if(qslStatus != null && !qslStatus.isBlank()){
            sql.append(" AND qsl_status = ? ");
            params.add(qslStatus);
        }
        // 倒序分页
        sql.append(" ORDER BY connect_date DESC LIMIT ?,? ");
        params.add(pageIndex * pageSize);
        params.add(pageSize);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for(int i=0;i<params.size();i++){
                pstmt.setObject(i+1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                result.add(convertRsToEntity(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 统计符合条件总条数
     */
    public int countByCondition(
            List<String> callSigns,
            LocalDate startDate,
            LocalDate endDate,
            String weather,
            List<String> freqs,
            String mode,
            String qslStatus
    ){
        int total = 0;
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM radio_log WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if(callSigns != null && !callSigns.isEmpty()){
            String placeHolder = String.join(",", Collections.nCopies(callSigns.size(), "?"));
            sql.append(" AND call_sign IN (").append(placeHolder).append(") ");
            params.addAll(callSigns);
        }
        if(startDate != null){
            sql.append(" AND connect_date >= ? ");
            params.add(startDate.toString());
        }
        if(endDate != null){
            sql.append(" AND connect_date <= ? ");
            params.add(endDate.toString());
        }
        if(weather != null && !weather.isBlank()){
            sql.append(" AND weather = ? ");
            params.add(weather);
        }
        if(freqs != null && !freqs.isEmpty()){
            String placeHolder = String.join(",", Collections.nCopies(freqs.size(), "?"));
            sql.append(" AND frequency IN (").append(placeHolder).append(") ");
            params.addAll(freqs);
        }
        if(mode != null && !mode.isBlank()){
            sql.append(" AND mode = ? ");
            params.add(mode);
        }
        if(qslStatus != null && !qslStatus.isBlank()){
            sql.append(" AND qsl_status = ? ");
            params.add(qslStatus);
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for(int i=0;i<params.size();i++){
                pstmt.setObject(i+1, params.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) total = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    /**
     * 初始化加载：读取最新140条数据（7页，每页20）
     */
    public List<RadioLog> initLoadTop140() {
        String sql = "SELECT * FROM radio_log ORDER BY connect_date DESC LIMIT 140";
        List<RadioLog> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while(rs.next()) list.add(convertRsToEntity(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}