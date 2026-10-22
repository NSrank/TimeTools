package org.plugin.timetools.manager;

import org.plugin.timetools.config.ConfigManager;
import org.plugin.timetools.model.Task;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务管理器
 * 
 * 负责任务的增删改查和持久化管理
 */
public class TaskManager {
    
    private final ConfigManager configManager;
    private final Logger logger;
    private final Map<String, Task> tasks;
    
    public TaskManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
        this.tasks = new ConcurrentHashMap<>();
        
        loadTasks();
    }
    
    /**
     * 加载所有任务
     */
    private void loadTasks() {
        try {
            List<Task> loadedTasks = configManager.loadTasks();
            for (Task task : loadedTasks) {
                tasks.put(task.getId(), task);
            }
            logger.info("成功加载 {} 个任务", tasks.size());
        } catch (Exception e) {
            logger.error("加载任务失败", e);
        }
    }
    
    /**
     * 添加任务
     */
    public String addTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("任务不能为空");
        }
        
        String taskId = task.getId();
        tasks.put(taskId, task);
        
        // 保存到文件
        saveAllTasks();
        
        logger.info("添加任务: {}", taskId);
        return taskId;
    }
    
    /**
     * 删除任务
     */
    public boolean removeTask(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return false;
        }
        
        Task removedTask = tasks.remove(taskId);
        if (removedTask != null) {
            // 保存到文件
            saveAllTasks();
            logger.info("删除任务: {}", taskId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取任务
     */
    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public Collection<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * 获取所有启用的任务
     */
    public Collection<Task> getEnabledTasks() {
        return tasks.values().stream()
                .filter(Task::isEnabled)
                .toList();
    }
    
    /**
     * 启用任务
     */
    public boolean enableTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setEnabled(true);
            saveAllTasks();
            logger.info("启用任务: {}", taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 禁用任务
     */
    public boolean disableTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setEnabled(false);
            saveAllTasks();
            logger.info("禁用任务: {}", taskId);
            return true;
        }
        return false;
    }
    
    /**
     * 检查任务是否存在
     */
    public boolean taskExists(String taskId) {
        return tasks.containsKey(taskId);
    }
    
    /**
     * 获取任务数量
     */
    public int getTaskCount() {
        return tasks.size();
    }
    
    /**
     * 获取启用的任务数量
     */
    public int getEnabledTaskCount() {
        return (int) tasks.values().stream()
                .filter(Task::isEnabled)
                .count();
    }
    
    /**
     * 更新任务的最后执行时间
     */
    public void updateLastExecutionTime(String taskId, long executionTime) {
        Task task = tasks.get(taskId);
        if (task != null) {
            task.setLastExecutionTime(executionTime);
            // 这里不立即保存，而是定期保存以提高性能
        }
    }
    
    /**
     * 保存所有任务到文件
     */
    public void saveAllTasks() {
        try {
            configManager.saveTasks(tasks.values());
            logger.debug("保存 {} 个任务到文件", tasks.size());
        } catch (Exception e) {
            logger.error("保存任务失败", e);
        }
    }
    
    /**
     * 获取任务列表（分页）
     */
    public List<Task> getTasksPaginated(int page, int pageSize) {
        List<Task> allTasks = new ArrayList<>(tasks.values());
        
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allTasks.size());
        
        if (startIndex >= allTasks.size()) {
            return new ArrayList<>();
        }
        
        return allTasks.subList(startIndex, endIndex);
    }
    
    /**
     * 获取总页数
     */
    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) tasks.size() / pageSize);
    }
    
    /**
     * 搜索任务
     */
    public List<Task> searchTasks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(tasks.values());
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return tasks.values().stream()
                .filter(task -> 
                    task.getId().toLowerCase().contains(lowerKeyword) ||
                    task.getCommands().stream().anyMatch(cmd -> 
                        cmd.toLowerCase().contains(lowerKeyword))
                )
                .toList();
    }
    
    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        tasks.clear();
        saveAllTasks();
        logger.info("清空所有任务");
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
