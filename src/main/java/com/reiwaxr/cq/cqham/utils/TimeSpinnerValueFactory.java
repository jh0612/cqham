package com.reiwaxr.cq.cqham.utils;

import javafx.scene.control.SpinnerValueFactory;

/**
 * @Description 自定义Spinner工厂内部类
 * @Version v1.0
 * @Author syoukou Email:reiwaxr@163.com
 * @Date 2026-06-19 0:35
 */
public class TimeSpinnerValueFactory extends SpinnerValueFactory<String> {
    @Override
    public void decrement(int steps) {
        String time = getValue();
        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int min = Integer.parseInt(hm[1]);

        min -= steps;
        if (min < 0) {
            hour--;
            min += 60;
            if (hour < 0) hour = 23;
        }
        setValue(String.format("%02d:%02d", hour, min));
    }

    /**
     * 自定义Spinner工厂内部类
     * @param steps 步长
     */
    @Override
    public void increment(int steps) {
        String time = getValue();
        String[] hm = time.split(":");
        int hour = Integer.parseInt(hm[0]);
        int min = Integer.parseInt(hm[1]);

        min += steps;
        if (min >= 60) {
            hour++;
            min -= 60;
            if (hour >= 24) hour = 0;
        }
        setValue(String.format("%02d:%02d", hour, min));
    }



}