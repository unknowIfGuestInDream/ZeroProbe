package com.tlcsdm.zeroprobe.model;

import com.tlcsdm.zeroprobe.config.I18N;

/**
 * Time ranges for chart display.
 * Each range determines how many data points are kept visible on the chart.
 */
public enum TimeRange {

    MIN_1(60, "monitor.timeRange.1min"),
    MIN_5(300, "monitor.timeRange.5min"),
    MIN_10(600, "monitor.timeRange.10min"),
    MIN_30(1800, "monitor.timeRange.30min"),
    HOUR_1(3600, "monitor.timeRange.1hour"),
    HOUR_2(7200, "monitor.timeRange.2hour"),
    HOUR_3(10800, "monitor.timeRange.3hour"),
    HOUR_6(21600, "monitor.timeRange.6hour"),
    HOUR_12(43200, "monitor.timeRange.12hour"),
    DAY_1(86400, "monitor.timeRange.1day"),
    DAY_7(604800, "monitor.timeRange.7day"),
    MONTH_1(2592000, "monitor.timeRange.1month"),
    MONTH_3(7776000, "monitor.timeRange.3month"),
    MONTH_6(15552000, "monitor.timeRange.6month");

    private final int seconds;
    private final String i18nKey;

    TimeRange(int seconds, String i18nKey) {
        this.seconds = seconds;
        this.i18nKey = i18nKey;
    }

    /**
     * Get the duration in seconds.
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Get the maximum number of data points to keep on the chart,
     * based on a 1-second polling interval.
     */
    public int getMaxDataPoints() {
        return seconds;
    }

    @Override
    public String toString() {
        return I18N.get(i18nKey);
    }
}
