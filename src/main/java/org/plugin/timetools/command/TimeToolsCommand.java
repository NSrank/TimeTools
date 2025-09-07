package org.plugin.timetools.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.plugin.timetools.manager.TaskManager;
import org.plugin.timetools.model.Task;
import org.plugin.timetools.parser.CommandParser;
import org.plugin.timetools.scheduler.TaskScheduler;
import org.plugin.timetools.util.PluginInfo;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TimeTools主命令处理器
 */
public class TimeToolsCommand implements SimpleCommand {
    
    private final TaskManager taskManager;
    private final TaskScheduler taskScheduler;
    private final Logger logger;
    
    // 待删除确认的任务ID
    private final Map<String, String> pendingDeletions = new HashMap<>();
    
    public TimeToolsCommand(TaskManager taskManager, TaskScheduler taskScheduler, Logger logger) {
        this.taskManager = taskManager;
        this.taskScheduler = taskScheduler;
        this.logger = logger;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        // 检查权限
        if (!hasPermission(source)) {
            sendMessage(source, taskManager.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length == 0) {
            showHelp(source, 1);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreate(source, args);
                break;
            case "list":
                handleList(source, args);
                break;
            case "delete":
                handleDelete(source, args);
                break;
            case "enable":
                handleEnable(source, args);
                break;
            case "disable":
                handleDisable(source, args);
                break;
            case "help":
                handleHelp(source, args);
                break;
            case "reload":
                handleReload(source);
                break;
            case "info":
                handleInfo(source, args);
                break;
            default:
                sendMessage(source, taskManager.getConfigManager().getMessage("invalid-command"));
                showHelp(source, 1);
        }
    }
    
    /**
     * 处理创建任务命令
     */
    private void handleCreate(CommandSource source, String[] args) {
        try {
            // 重新组合被空格分割的参数
            String[] reconstructedArgs = reconstructCreateArgs(args);

            Task task = CommandParser.parseCreateCommand(reconstructedArgs);
            String taskId = taskManager.addTask(task);

            // 添加到调度器
            taskScheduler.addTask(task);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-created", placeholders));

            logger.info("用户 {} 创建了任务: {}", getSourceName(source), taskId);

        } catch (CommandParser.ParseException e) {
            sendMessage(source, "§c创建任务失败: " + e.getMessage());
        } catch (Exception e) {
            sendMessage(source, "§c创建任务时发生内部错误");
            logger.error("创建任务失败", e);
        }
    }

    /**
     * 重新组合create命令的参数，处理被空格分割的情况
     */
    private String[] reconstructCreateArgs(String[] args) {
        if (args.length < 4) {
            return args; // 参数太少，直接返回让后续处理报错
        }

        List<String> reconstructed = new ArrayList<>();
        reconstructed.add(args[0]); // "create"

        // 寻找命令部分的开始和结束
        int commandStart = -1;
        int commandEnd = -1;

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("{")) {
                commandStart = i;
            }
            if (args[i].endsWith("}")) {
                commandEnd = i;
                break;
            }
        }

        if (commandStart == -1 || commandEnd == -1) {
            return args; // 没有找到完整的{}，直接返回让后续处理报错
        }

        // 重新组合命令部分
        StringBuilder commandPart = new StringBuilder();
        for (int i = commandStart; i <= commandEnd; i++) {
            if (i > commandStart) {
                commandPart.append(" ");
            }
            commandPart.append(args[i]);
        }
        reconstructed.add(commandPart.toString());

        // 添加剩余的参数（时间和星期部分）
        for (int i = commandEnd + 1; i < args.length; i++) {
            reconstructed.add(args[i]);
        }

        return reconstructed.toArray(new String[0]);
    }
    
    /**
     * 处理列表命令
     */
    private void handleList(CommandSource source, String[] args) {
        if (args.length >= 2) {
            // 显示特定任务的详细信息
            String taskId = args[1];
            Task task = taskManager.getTask(taskId);
            
            if (task == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", taskId);
                sendMessage(source, taskManager.getConfigManager().getMessage("task-not-found", placeholders));
                return;
            }
            
            showTaskDetails(source, task);
        } else {
            // 显示任务列表
            showTaskList(source, 1);
        }
    }
    
    /**
     * 处理删除命令
     */
    private void handleDelete(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendMessage(source, "§c用法: /timetools delete <任务ID>");
            return;
        }
        
        String taskId = args[1];
        String sourceName = getSourceName(source);
        
        // 检查任务是否存在
        if (!taskManager.taskExists(taskId)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-not-found", placeholders));
            return;
        }
        
        // 检查是否已经在等待确认
        if (pendingDeletions.containsKey(sourceName) && pendingDeletions.get(sourceName).equals(taskId)) {
            // 执行删除
            if (taskManager.removeTask(taskId)) {
                taskScheduler.removeTask(taskId);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("id", taskId);
                sendMessage(source, taskManager.getConfigManager().getMessage("task-deleted", placeholders));
                
                logger.info("用户 {} 删除了任务: {}", sourceName, taskId);
            } else {
                sendMessage(source, "§c删除任务失败");
            }
            
            pendingDeletions.remove(sourceName);
        } else {
            // 请求确认
            pendingDeletions.put(sourceName, taskId);
            sendMessage(source, "§e确认删除任务 " + taskId + "？再次执行相同命令以确认删除。");
        }
    }
    
    /**
     * 处理启用命令
     */
    private void handleEnable(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendMessage(source, "§c用法: /timetools enable <任务ID>");
            return;
        }
        
        String taskId = args[1];
        
        if (taskManager.enableTask(taskId)) {
            // 重新加载调度器以应用更改
            taskScheduler.reloadTasks();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-enabled", placeholders));
            
            logger.info("用户 {} 启用了任务: {}", getSourceName(source), taskId);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-not-found", placeholders));
        }
    }
    
    /**
     * 处理禁用命令
     */
    private void handleDisable(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendMessage(source, "§c用法: /timetools disable <任务ID>");
            return;
        }
        
        String taskId = args[1];
        
        if (taskManager.disableTask(taskId)) {
            // 重新加载调度器以应用更改
            taskScheduler.reloadTasks();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-disabled", placeholders));
            
            logger.info("用户 {} 禁用了任务: {}", getSourceName(source), taskId);
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", taskId);
            sendMessage(source, taskManager.getConfigManager().getMessage("task-not-found", placeholders));
        }
    }
    
    /**
     * 处理帮助命令
     */
    private void handleHelp(CommandSource source, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        showHelp(source, page);
    }
    
    /**
     * 处理重载命令
     */
    private void handleReload(CommandSource source) {
        try {
            taskScheduler.reloadTasks();
            sendMessage(source, "§a配置重载完成！");
            logger.info("用户 {} 重载了配置", getSourceName(source));
        } catch (Exception e) {
            sendMessage(source, "§c重载配置失败");
            logger.error("重载配置失败", e);
        }
    }
    
    /**
     * 处理信息命令
     */
    private void handleInfo(CommandSource source, String[] args) {
        PluginInfo info = PluginInfo.getInstance();

        sendMessage(source, "§6=== TimeTools 插件信息 ===");
        sendMessage(source, "§e版本: §f" + info.getFullVersionInfo());
        sendMessage(source, "§e描述: §f" + info.getDescription());
        sendMessage(source, "§e作者: §f" + info.getAuthors());
        sendMessage(source, "§e许可证: §f" + info.getLicense());
        sendMessage(source, "§e系统: §f" + info.getSystemInfo());
        sendMessage(source, "");
        sendMessage(source, "§6=== 运行状态 ===");
        sendMessage(source, "§e总任务数: §f" + taskManager.getTaskCount());
        sendMessage(source, "§e启用任务数: §f" + taskManager.getEnabledTaskCount());
        sendMessage(source, "§e调度器状态: §f" + (taskScheduler.isRunning() ? "§a运行中" : "§c已停止"));
        sendMessage(source, "§e活跃间隔任务: §f" + taskScheduler.getActiveIntervalTaskCount());
        sendMessage(source, "");
        sendMessage(source, "§7项目地址: " + info.getUrl());
        sendMessage(source, "§7许可证: " + info.getLicenseUrl());
    }
    
    /**
     * 显示任务列表
     */
    private void showTaskList(CommandSource source, int page) {
        Collection<Task> tasks = taskManager.getAllTasks();
        
        if (tasks.isEmpty()) {
            sendMessage(source, "§e当前没有任何任务");
            return;
        }
        
        sendMessage(source, "§6=== 任务列表 ===");
        
        int index = 1;
        for (Task task : tasks) {
            String status = task.isEnabled() ? "§a启用" : "§c禁用";
            String commands = String.join(", ", task.getCommands());
            if (commands.length() > 50) {
                commands = commands.substring(0, 47) + "...";
            }
            
            sendMessage(source, String.format("§e%d. §f%s §7- %s §7- %s", 
                    index++, task.getId(), status, commands));
        }
        
        sendMessage(source, "§7使用 /timetools list <ID> 查看任务详情");
    }
    
    /**
     * 显示任务详细信息
     */
    private void showTaskDetails(CommandSource source, Task task) {
        sendMessage(source, "§6=== 任务详情: " + task.getId() + " ===");
        sendMessage(source, "§e状态: " + (task.isEnabled() ? "§a启用" : "§c禁用"));
        sendMessage(source, "§e命令: §f" + String.join(", ", task.getCommands()));
        sendMessage(source, "§e调度类型: §f" + task.getScheduleType());
        sendMessage(source, "§e开始时间: §f" + task.getStartTime());
        if (task.getEndTime() != null) {
            sendMessage(source, "§e结束时间: §f" + task.getEndTime());
        }
        sendMessage(source, "§e执行日期: §f" + (task.isEveryDay() ? "每天" : task.getDaysOfWeek().toString()));
        sendMessage(source, "§e执行模式: §f" + task.getExecutionMode());
        if (task.getIntervalTicks() > 0) {
            sendMessage(source, "§e间隔: §f" + task.getIntervalTicks() + " ticks (" + task.getIntervalUnit() + ")");
        }
        if (task.getExecutionInterval() > 0) {
            sendMessage(source, "§e执行间隔: §f" + task.getExecutionInterval() + " ticks");
        }
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp(CommandSource source, int page) {
        sendMessage(source, "§6=== TimeTools 帮助 ===");
        sendMessage(source, "§e/timetools create {命令} 时间 星期 §7- 创建任务");
        sendMessage(source, "§e/timetools list [ID] §7- 列出任务或查看详情");
        sendMessage(source, "§e/timetools delete <ID> §7- 删除任务");
        sendMessage(source, "§e/timetools enable <ID> §7- 启用任务");
        sendMessage(source, "§e/timetools disable <ID> §7- 禁用任务");
        sendMessage(source, "§e/timetools reload §7- 重载配置");
        sendMessage(source, "§e/timetools info §7- 显示插件信息");
        sendMessage(source, "§e/timetools help §7- 显示此帮助");
        sendMessage(source, "");
        sendMessage(source, "§6示例:");
        sendMessage(source, "§7/timetools create {/say hello} 14:00 Eve");
        sendMessage(source, "§7/timetools create {/say test} E Eve,1m");
        sendMessage(source, "§7/timetools create {/cmd1},{/cmd2},Obo,5 09:00 Mon,Tue");
    }
    
    /**
     * 检查权限
     */
    private boolean hasPermission(CommandSource source) {
        // 控制台总是有权限
        if (source instanceof ConsoleCommandSource) {
            return true;
        }
        
        // 检查玩家权限
        return source.hasPermission("timetools.admin");
    }
    
    /**
     * 获取命令源名称
     */
    private String getSourceName(CommandSource source) {
        if (source instanceof ConsoleCommandSource) {
            return "Console";
        }
        return source.toString();
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(CommandSource source, String message) {
        source.sendMessage(Component.text(message).color(NamedTextColor.WHITE));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            // 没有参数时，返回所有子命令
            return Arrays.asList("create", "list", "delete", "enable", "disable", "help", "reload", "info");
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return suggestCreate(args);
            case "list":
            case "delete":
            case "enable":
            case "disable":
                return suggestTaskId(args);
            case "help":
                if (args.length == 2) {
                    return Arrays.asList("1", "2", "3");
                }
                break;
        }

        return Collections.emptyList();
    }

    /**
     * 为create命令提供补全建议
     */
    private List<String> suggestCreate(String[] args) {
        if (args.length == 2) {
            // 命令部分补全
            return Arrays.asList(
                "{/say hello}",
                "{/list}",
                "{/weather clear}",
                "{/save-all}",
                "{/cmd1,/cmd2,All}",
                "{/cmd1,/cmd2,Obo,5}"
            );
        } else if (args.length == 3) {
            // 时间部分补全
            return Arrays.asList(
                "14:00",
                "09:00-17:00",
                "E",
                "4:00-5:00,E"
            );
        } else if (args.length == 4) {
            // 星期部分补全
            return Arrays.asList(
                "Eve",
                "Mon,Tue,Wed,Thu,Fri",
                "Sat,Sun",
                "Eve,1m",
                "Eve,30s",
                "Mon,5m"
            );
        }
        return Collections.emptyList();
    }

    /**
     * 为需要任务ID的命令提供补全建议
     */
    private List<String> suggestTaskId(String[] args) {
        if (args.length == 2) {
            // 返回所有任务ID
            Collection<Task> tasks = taskManager.getAllTasks();
            return tasks.stream()
                    .map(Task::getId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
