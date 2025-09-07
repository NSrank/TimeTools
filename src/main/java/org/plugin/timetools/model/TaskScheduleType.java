package org.plugin.timetools.model;

/**
 * 任务调度类型枚举
 */
public enum TaskScheduleType {
    /**
     * 固定时间执行
     * 例如：每天4:00执行
     */
    FIXED_TIME,
    
    /**
     * 时间区间执行
     * 例如：每天4:00-5:00之间执行
     */
    TIME_RANGE,
    
    /**
     * 间隔执行
     * 例如：每隔1分钟执行
     */
    INTERVAL,
    
    /**
     * 时间区间内间隔执行
     * 例如：每天4:00-5:00之间每隔1分钟执行
     */
    TIME_RANGE_WITH_INTERVAL
}
