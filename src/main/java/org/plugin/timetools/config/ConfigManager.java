package org.plugin.timetools.config;

import org.plugin.timetools.model.ExecutionMode;
import org.plugin.timetools.model.Task;
import org.plugin.timetools.model.TaskScheduleType;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/**
 * 配置管理器
 * 
 * 负责配置文件的读写和任务数据的持久化
 */
public class ConfigManager {
    
    private final Path dataDirectory;
    private final Path configFile;
    private final Path tasksFile;
    private final Logger logger;
    private final Yaml yaml;
    
    private Map<String, Object> config;
    
    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.configFile = dataDirectory.resolve("config.yml");
        this.tasksFile = dataDirectory.resolve("tasks.yml");
        this.logger = logger;
        
        // 配置YAML格式
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
        
        initializeConfig();
    }
    
    /**
     * 初始化配置
     */
    private void initializeConfig() {
        try {
            // 创建数据目录
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            // 加载或创建配置文件
            loadConfig();
            
        } catch (IOException e) {
            logger.error("初始化配置失败", e);
        }
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() throws IOException {
        if (!Files.exists(configFile)) {
            createDefaultConfig();
        }
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            config = yaml.load(inputStream);
            if (config == null) {
                config = new HashMap<>();
            }
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() throws IOException {
        // 尝试从resources复制默认配置
        try (InputStream defaultConfigStream = getClass().getResourceAsStream("/config.yml")) {
            if (defaultConfigStream != null) {
                Files.copy(defaultConfigStream, configFile);
                logger.info("已从默认模板创建配置文件");
            } else {
                // 如果resources中没有配置文件，则创建基本配置
                createBasicConfig();
            }
        } catch (IOException e) {
            logger.warn("无法复制默认配置文件，创建基本配置", e);
            createBasicConfig();
        }
    }

    /**
     * 创建基本配置文件
     */
    private void createBasicConfig() throws IOException {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("debug", false);
        defaultConfig.put("check-interval-seconds", 60);
        defaultConfig.put("max-execution-threads", 5);

        Map<String, String> messages = new HashMap<>();
        messages.put("task-created", "§a任务创建成功！任务ID: {id}");
        messages.put("task-deleted", "§a任务删除成功！任务ID: {id}");
        messages.put("task-enabled", "§a任务已启用！任务ID: {id}");
        messages.put("task-disabled", "§c任务已禁用！任务ID: {id}");
        messages.put("task-not-found", "§c未找到任务ID: {id}");
        messages.put("invalid-command", "§c无效的命令格式！请使用 /timetools help 查看帮助");
        messages.put("no-permission", "§c你没有权限执行此命令！");

        defaultConfig.put("messages", messages);

        try (OutputStream outputStream = Files.newOutputStream(configFile)) {
            yaml.dump(defaultConfig, new OutputStreamWriter(outputStream, "UTF-8"));
        }

        config = defaultConfig;
        logger.info("已创建基本配置文件");
    }
    
    /**
     * 获取配置值
     */
    public Object getConfig(String key) {
        return getConfig(key, null);
    }
    
    public Object getConfig(String key, Object defaultValue) {
        String[] keys = key.split("\\.");
        Object current = config;
        
        for (String k : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(k);
            } else {
                return defaultValue;
            }
        }
        
        return current != null ? current : defaultValue;
    }
    
    /**
     * 获取消息
     */
    public String getMessage(String key) {
        return getMessage(key, new HashMap<>());
    }
    
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = (String) getConfig("messages." + key, "§c消息未找到: " + key);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    /**
     * 保存任务到文件
     */
    public void saveTasks(Collection<Task> tasks) {
        try {
            List<Map<String, Object>> taskList = new ArrayList<>();
            
            for (Task task : tasks) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("commands", task.getCommands());
                taskMap.put("scheduleType", task.getScheduleType().name());
                if (task.getStartTime() != null) {
                    taskMap.put("startTime", task.getStartTime().toString());
                } else {
                    taskMap.put("startTime", null);
                }
                if (task.getEndTime() != null) {
                    taskMap.put("endTime", task.getEndTime().toString());
                } else {
                    taskMap.put("endTime", null);
                }
                
                List<String> daysOfWeek = new ArrayList<>();
                for (DayOfWeek day : task.getDaysOfWeek()) {
                    daysOfWeek.add(day.name());
                }
                taskMap.put("daysOfWeek", daysOfWeek);
                taskMap.put("everyDay", task.isEveryDay());
                taskMap.put("intervalTicks", task.getIntervalTicks());
                taskMap.put("intervalUnit", task.getIntervalUnit());
                taskMap.put("executionMode", task.getExecutionMode().name());
                taskMap.put("executionInterval", task.getExecutionInterval());
                taskMap.put("enabled", task.isEnabled());
                taskMap.put("lastExecutionTime", task.getLastExecutionTime());
                
                taskList.add(taskMap);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("tasks", taskList);
            
            try (OutputStream outputStream = Files.newOutputStream(tasksFile)) {
                yaml.dump(data, new OutputStreamWriter(outputStream, "UTF-8"));
            }
            
        } catch (IOException e) {
            logger.error("保存任务失败", e);
        }
    }
    
    /**
     * 从文件加载任务
     */
    @SuppressWarnings("unchecked")
    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        
        if (!Files.exists(tasksFile)) {
            return tasks;
        }
        
        try (InputStream inputStream = Files.newInputStream(tasksFile)) {
            Map<String, Object> data = yaml.load(inputStream);
            if (data == null || !data.containsKey("tasks")) {
                return tasks;
            }
            
            List<Map<String, Object>> taskList = (List<Map<String, Object>>) data.get("tasks");
            
            for (Map<String, Object> taskMap : taskList) {
                try {
                    Task task = createTaskFromMap(taskMap);
                    tasks.add(task);
                } catch (Exception e) {
                    logger.error("加载任务失败: " + taskMap.get("id"), e);
                }
            }
            
        } catch (IOException e) {
            logger.error("加载任务文件失败", e);
        }
        
        return tasks;
    }
    
    /**
     * 从Map创建Task对象
     */
    @SuppressWarnings("unchecked")
    private Task createTaskFromMap(Map<String, Object> taskMap) {
        String id = (String) taskMap.get("id");
        List<String> commands = (List<String>) taskMap.get("commands");
        TaskScheduleType scheduleType = TaskScheduleType.valueOf((String) taskMap.get("scheduleType"));
        LocalTime startTime = null;
        if (taskMap.get("startTime") != null) {
            startTime = LocalTime.parse((String) taskMap.get("startTime"));
        }
        LocalTime endTime = null;
        if (taskMap.containsKey("endTime") && taskMap.get("endTime") != null) {
            endTime = LocalTime.parse((String) taskMap.get("endTime"));
        }
        
        Set<DayOfWeek> daysOfWeek = new HashSet<>();
        List<String> daysList = (List<String>) taskMap.get("daysOfWeek");
        for (String day : daysList) {
            daysOfWeek.add(DayOfWeek.valueOf(day));
        }
        
        boolean everyDay = (Boolean) taskMap.get("everyDay");
        long intervalTicks = ((Number) taskMap.get("intervalTicks")).longValue();
        String intervalUnit = (String) taskMap.get("intervalUnit");
        ExecutionMode executionMode = ExecutionMode.valueOf((String) taskMap.get("executionMode"));
        int executionInterval = ((Number) taskMap.get("executionInterval")).intValue();
        boolean enabled = (Boolean) taskMap.get("enabled");
        
        Task task = new Task(id, commands, scheduleType, startTime, endTime, daysOfWeek,
                everyDay, intervalTicks, intervalUnit, executionMode, executionInterval, enabled);
        
        if (taskMap.containsKey("lastExecutionTime")) {
            task.setLastExecutionTime(((Number) taskMap.get("lastExecutionTime")).longValue());
        }
        
        return task;
    }
}
