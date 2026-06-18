package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Description 无线电通联日志实体
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-11 23:30
 */
@Getter
@Setter
public class RadioLog {

    // 统一格式化器，静态常量只创建一次
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /* 主键ID */
    private Long id;
    /* 呼号 */
    private String callSign;
    /* QTH位置 */
    private String qth;
    /* 频率 */
    private String frequency;
    /* 通联模式 */
    private String mode;
    /* 通联日期 */
    private LocalDate connectDate;
    /* 通联时间 */
    private String connectTime;
    /* 对方设备 */
    private String device;
    /* 天气 */
    private String weather;
    /* 信号报告 */
    private String signalReport;
    /* 姓名 */
    private String name;
    /* 功率 */
    private String power;
    /* QSL状态 */
    private String qslStatus;
    /* 住址 */
    private String address;
    /* 备注 */
    private String remark;

    // 无参、有参构造
    public RadioLog() {}

    public RadioLog(Long id, String callSign, String qth, String frequency, String mode, LocalDate connectDate, String device, String weather, String signalReport, String name, String power, String qslStatus, String address, String remark) {
        this.id = id;
        this.callSign = callSign;
        this.qth = qth;
        this.frequency = frequency;
        this.mode = mode;
        this.connectDate = connectDate;
        this.device = device;
        this.weather = weather;
        this.signalReport = signalReport;
        this.name = name;
        this.power = power;
        this.qslStatus = qslStatus;
        this.address = address;
        this.remark = remark;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public String getQth() {
        return qth;
    }

    public void setQth(String qth) {
        this.qth = qth;
    }

    public String getFrequency() {
        return frequency;
    }

    public LocalDate getConnectDate() {
        return connectDate;
    }

    public void setConnectDate(LocalDate connectDate) {
        this.connectDate = connectDate;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getSignalReport() {
        return signalReport;
    }

    public void setSignalReport(String signalReport) {
        this.signalReport = signalReport;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getQslStatus() {
        return qslStatus;
    }

    public void setQslStatus(String qslStatus) {
        this.qslStatus = qslStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
