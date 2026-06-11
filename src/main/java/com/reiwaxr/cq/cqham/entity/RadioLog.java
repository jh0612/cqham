package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description 无线电通联日志实体
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-11 23:30
 */
@Getter
@Setter
public class RadioLog {
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
    /* 通联时间 */
    private LocalDateTime connectTime;
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
    /* 备注 */
    private String remark;

    // 无参、有参构造
    public RadioLog() {}

    public RadioLog(String callSign, LocalDateTime connectTime, String device, String frequency, Long id, String mode, String name, String power, String qslStatus, String qth, String remark, String signalReport, String weather) {
        this.callSign = callSign;
        this.connectTime = connectTime;
        this.device = device;
        this.frequency = frequency;
        this.id = id;
        this.mode = mode;
        this.name = name;
        this.power = power;
        this.qslStatus = qslStatus;
        this.qth = qth;
        this.remark = remark;
        this.signalReport = signalReport;
        this.weather = weather;
    }
}
