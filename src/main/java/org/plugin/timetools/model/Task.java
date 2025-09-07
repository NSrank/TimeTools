package org.plugin.timetools.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 任务模型类
 * 
 * 表示一个定时任务，包含任务的所有配置信息
 */
public class Task {
    
    private final String id;
    private final List<String> commands;
    private final TaskScheduleType scheduleType;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Set<DayOfWeek> daysOfWeek;
    private final boolean everyDay;
    private final long intervalTicks;
    private final String intervalUnit;
    private final ExecutionMode executionMode;
    private final int executionInterval;
    private boolean enabled;
    private long lastExecutionTime;
    
    /**
     * 构造函数
     */
    public Task(String id, List<String> commands, TaskScheduleType scheduleType,
                LocalTime startTime, LocalTime endTime, Set<DayOfWeek> daysOfWeek,
                boolean everyDay, long intervalTicks, String intervalUnit,
                ExecutionMode executionMode, int executionInterval, boolean enabled) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.commands = commands;
        this.scheduleType = scheduleType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysOfWeek = daysOfWeek;
        this.everyDay = everyDay;
        this.intervalTicks = intervalTicks;
        this.intervalUnit = intervalUnit;
        this.executionMode = executionMode;
        this.executionInterval = executionInterval;
        this.enabled = enabled;
        this.lastExecutionTime = 0;
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public TaskScheduleType getScheduleType() {
        return scheduleType;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
    
    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }
    
    public boolean isEveryDay() {
        return everyDay;
    }
    
    public long getIntervalTicks() {
        return intervalTicks;
    }
    
    public String getIntervalUnit() {
        return intervalUnit;
    }
    
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }
    
    public int getExecutionInterval() {
        return executionInterval;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public long getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    // Setters
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setLastExecutionTime(long lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }
    
    /**
     * 检查任务是否应该在指定时间执行
     */
    public boolean shouldExecuteAt(LocalTime time, DayOfWeek dayOfWeek) {
        if (!enabled) {
            return false;
        }
        
        // 检查星期
        if (!everyDay && !daysOfWeek.contains(dayOfWeek)) {
            return false;
        }
        
        // 检查时间
        switch (scheduleType) {
            case FIXED_TIME:
                return time.equals(startTime);
            case TIME_RANGE:
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            case INTERVAL:
                // 间隔执行的逻辑在调度器中处理
                return true;
            case TIME_RANGE_WITH_INTERVAL:
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task{id='").append(id).append("'");
        sb.append(", commands=").append(commands);
        sb.append(", scheduleType=").append(scheduleType);
        sb.append(", startTime=").append(startTime);
        if (endTime != null) {
            sb.append(", endTime=").append(endTime);
        }
        sb.append(", daysOfWeek=").append(daysOfWeek);
        sb.append(", everyDay=").append(everyDay);
        if (intervalTicks > 0) {
            sb.append(", intervalTicks=").append(intervalTicks);
            sb.append(", intervalUnit='").append(intervalUnit).append("'");
        }
        sb.append(", executionMode=").append(executionMode);
        if (executionInterval > 0) {
            sb.append(", executionInterval=").append(executionInterval);
        }
        sb.append(", enabled=").append(enabled);
        sb.append('}');
        return sb.toString();
    }
}
