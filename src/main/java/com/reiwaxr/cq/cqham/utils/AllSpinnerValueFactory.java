package com.reiwaxr.cq.cqham.utils;

import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.text.DecimalFormat;

/**
 * @Description 封装微调器组件工具类
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-19 0:27
 */
public class AllSpinnerValueFactory {

    /**
     * 频率微调器固定显示格式私有方法
     */
    public static SpinnerValueFactory.DoubleSpinnerValueFactory getDoubleSpinnerValueFactory() {
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0000, 999.9999, 438.500, 0.125);
        DecimalFormat df = new DecimalFormat("000.0000");
        factory.setConverter(new StringConverter<>() {
            // 数值转界面文本：强制4位小数
            @Override
            public String toString(Double value) {
                if (value == null) return "0.0000";
                return df.format(value);
            }

            // 输入文本转回数值
            @Override
            public Double fromString(String s) {
                try {
                    return df.parse(s).doubleValue();
                } catch (Exception e) {
                    // 输入非法时恢复当前值
                    return factory.getValue();
                }
            }
        });
        return factory;
    }
}
