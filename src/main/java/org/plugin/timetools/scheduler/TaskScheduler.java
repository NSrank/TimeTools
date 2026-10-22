package org.plugin.timetools.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import org.plugin.timetools.manager.TaskManager;
import org.plugin.timetools.model.Task;
import org.plugin.timetools.model.TaskScheduleType;
import org.slf4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度器
 *
 * 负责任务的调度和执行
 */
public class TaskScheduler {

    private final Object plugin;
    private final ProxyServer server;
    private final TaskManager taskManager;
    private final Logger logger;
    private final Scheduler scheduler;
    private final TaskExecutor taskExecutor;

    private ScheduledTask mainSchedulerTask;
    private final ConcurrentHashMap<String, ScheduledTask> intervalTasks;
    private boolean running;

    public TaskScheduler(Object plugin, ProxyServer server, TaskManager taskManager, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.taskManager = taskManager;
        this.logger = logger;
        this.scheduler = server.getScheduler();
        this.taskExecutor = new TaskExecutor(plugin, server, logger);
        this.intervalTasks = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    /**
     * 启动调度器
     */
    public void start() {
        if (running) {
            logger.warn("任务调度器已经在运行中");
            return;
        }
        
        running = true;
        
        // 启动主调度器，每分钟检查一次
        mainSchedulerTask = scheduler.buildTask(plugin, this::checkAndExecuteTasks)
                .repeat(1, TimeUnit.MINUTES)
                .schedule();
        
        // 启动间隔任务
        startIntervalTasks();
        
        logger.info("任务调度器已启动");
    }
    
    /**
     * 停止调度器
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // 停止主调度器
        if (mainSchedulerTask != null) {
            mainSchedulerTask.cancel();
        }
        
        // 停止所有间隔任务
        stopAllIntervalTasks();
        
        logger.info("任务调度器已停止");
    }
    
    /**
     * 检查并执行任务
     */
    private void checkAndExecuteTasks() {
        if (!running) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // 只检查分钟级别的精度
        LocalTime checkTime = LocalTime.of(currentTime.getHour(), currentTime.getMinute());
        
        Collection<Task> enabledTasks = taskManager.getEnabledTasks();
        
        for (Task task : enabledTasks) {
            try {
                if (shouldExecuteTask(task, checkTime, currentDay)) {
                    executeTask(task);
                }
            } catch (Exception e) {
                logger.error("检查任务执行条件时发生错误: " + task.getId(), e);
            }
        }
    }
    
    /**
     * 判断任务是否应该执行
     */
    private boolean shouldExecuteTask(Task task, LocalTime currentTime, DayOfWeek currentDay) {
        if (!task.isEnabled()) {
            return false;
        }
        
        // 检查星期
        if (!task.isEveryDay() && !task.getDaysOfWeek().contains(currentDay)) {
            return false;
        }
        
        // 根据调度类型检查时间
        switch (task.getScheduleType()) {
            case FIXED_TIME:
                return currentTime.equals(task.getStartTime());
                
            case TIME_RANGE:
                return !currentTime.isBefore(task.getStartTime()) && 
                       !currentTime.isAfter(task.getEndTime());
                       
            case INTERVAL:
                // 间隔任务由单独的调度器处理
                return false;
                
            case TIME_RANGE_WITH_INTERVAL:
                // 时间区间内的间隔任务由单独的调度器处理
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * 执行任务
     */
    private void executeTask(Task task) {
        try {
            taskExecutor.executeTask(task);
            
            // 更新最后执行时间
            taskManager.updateLastExecutionTime(task.getId(), System.currentTimeMillis());
            
            logger.info("执行任务: {} - {}", task.getId(), task.getCommands());
            
        } catch (Exception e) {
            logger.error("执行任务失败: " + task.getId(), e);
        }
    }
    
    /**
     * 启动间隔任务
     */
    private void startIntervalTasks() {
        Collection<Task> enabledTasks = taskManager.getEnabledTasks();
        
        for (Task task : enabledTasks) {
            if (task.getScheduleType() == TaskScheduleType.INTERVAL ||
                task.getScheduleType() == TaskScheduleType.TIME_RANGE_WITH_INTERVAL) {
                startIntervalTask(task);
            }
        }
    }
    
    /**
     * 启动单个间隔任务
     */
    public void startIntervalTask(Task task) {
        if (task.getIntervalTicks() <= 0) {
            return;
        }
        
        // 停止已存在的任务
        stopIntervalTask(task.getId());
        
        // 计算间隔时间（毫秒）
        long intervalMs = task.getIntervalTicks() * 50; // 1 tick = 50ms
        
        ScheduledTask scheduledTask = scheduler.buildTask(plugin, () -> {
            try {
                if (shouldExecuteIntervalTask(task)) {
                    executeTask(task);
                }
            } catch (Exception e) {
                logger.error("执行间隔任务失败: " + task.getId(), e);
            }
        }).repeat(intervalMs, TimeUnit.MILLISECONDS).schedule();
        
        intervalTasks.put(task.getId(), scheduledTask);
        logger.debug("启动间隔任务: {} - 间隔: {}ms", task.getId(), intervalMs);
    }
    
    /**
     * 停止间隔任务
     */
    public void stopIntervalTask(String taskId) {
        ScheduledTask scheduledTask = intervalTasks.remove(taskId);
        if (scheduledTask != null) {
            scheduledTask.cancel();
            logger.debug("停止间隔任务: {}", taskId);
        }
    }
    
    /**
     * 停止所有间隔任务
     */
    private void stopAllIntervalTasks() {
        for (ScheduledTask task : intervalTasks.values()) {
            task.cancel();
        }
        intervalTasks.clear();
        logger.debug("停止所有间隔任务");
    }
    
    /**
     * 判断间隔任务是否应该执行
     */
    private boolean shouldExecuteIntervalTask(Task task) {
        if (!task.isEnabled()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // 检查星期
        if (!task.isEveryDay() && !task.getDaysOfWeek().contains(currentDay)) {
            return false;
        }
        
        // 如果是时间区间内的间隔任务，检查当前时间是否在区间内
        if (task.getScheduleType() == TaskScheduleType.TIME_RANGE_WITH_INTERVAL) {
            LocalTime currentTime = now.toLocalTime();
            return !currentTime.isBefore(task.getStartTime()) && 
                   !currentTime.isAfter(task.getEndTime());
        }
        
        return true;
    }
    
    /**
     * 重新加载任务调度
     */
    public void reloadTasks() {
        logger.info("重新加载任务调度");
        
        // 停止所有间隔任务
        stopAllIntervalTasks();
        
        // 重新启动间隔任务
        startIntervalTasks();
    }
    
    /**
     * 添加新任务到调度器
     */
    public void addTask(Task task) {
        if (task.getScheduleType() == TaskScheduleType.INTERVAL ||
            task.getScheduleType() == TaskScheduleType.TIME_RANGE_WITH_INTERVAL) {
            startIntervalTask(task);
        }
    }
    
    /**
     * 从调度器移除任务
     */
    public void removeTask(String taskId) {
        stopIntervalTask(taskId);
    }
    
    /**
     * 获取运行状态
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 获取活跃的间隔任务数量
     */
    public int getActiveIntervalTaskCount() {
        return intervalTasks.size();
    }
}
