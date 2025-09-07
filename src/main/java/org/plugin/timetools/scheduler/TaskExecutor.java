package org.plugin.timetools.scheduler;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import org.plugin.timetools.model.ExecutionMode;
import org.plugin.timetools.model.Task;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务执行器
 *
 * 负责具体的任务执行逻辑
 */
public class TaskExecutor {

    private final Object plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Scheduler scheduler;

    public TaskExecutor(Object plugin, ProxyServer server, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.scheduler = server.getScheduler();
    }
    
    /**
     * 执行任务
     */
    public void executeTask(Task task) {
        if (task == null || !task.isEnabled()) {
            return;
        }
        
        List<String> commands = task.getCommands();
        if (commands.isEmpty()) {
            logger.warn("任务 {} 没有要执行的命令", task.getId());
            return;
        }
        
        ExecutionMode mode = task.getExecutionMode();
        
        switch (mode) {
            case SINGLE:
                executeSingleCommand(commands.get(0));
                break;
                
            case ALL:
                executeAllCommands(commands);
                break;
                
            case ONE_BY_ONE:
                executeCommandsOneByOne(commands, task.getExecutionInterval());
                break;
                
            default:
                logger.warn("未知的执行模式: {}", mode);
        }
    }
    
    /**
     * 执行单个命令
     */
    private void executeSingleCommand(String command) {
        try {
            executeCommand(command);
            logger.debug("执行单个命令: {}", command);
        } catch (Exception e) {
            logger.error("执行单个命令失败: " + command, e);
        }
    }
    
    /**
     * 同时执行所有命令（伪同时，间隔1tick）
     */
    private void executeAllCommands(List<String> commands) {
        for (int i = 0; i < commands.size(); i++) {
            final String command = commands.get(i);
            final int delay = i; // 每个命令延迟i个tick
            
            scheduler.buildTask(plugin, () -> {
                try {
                    executeCommand(command);
                    logger.debug("执行命令 (ALL模式): {}", command);
                } catch (Exception e) {
                    logger.error("执行命令失败 (ALL模式): " + command, e);
                }
            }).delay(delay * 50, TimeUnit.MILLISECONDS).schedule(); // 1 tick = 50ms
        }
        
        logger.debug("启动 {} 个命令的同时执行", commands.size());
    }
    
    /**
     * 逐个执行命令
     */
    private void executeCommandsOneByOne(List<String> commands, int intervalTicks) {
        if (intervalTicks < 2) {
            intervalTicks = 2; // 最小间隔2ticks
        }
        
        final int finalIntervalTicks = intervalTicks;
        final AtomicInteger index = new AtomicInteger(0);
        
        // 立即执行第一个命令
        executeCommand(commands.get(0));
        logger.debug("执行命令 1/{} (OBO模式): {}", commands.size(), commands.get(0));
        
        // 如果只有一个命令，直接返回
        if (commands.size() == 1) {
            return;
        }
        
        // 调度后续命令
        scheduler.buildTask(plugin, new Runnable() {
            @Override
            public void run() {
                int currentIndex = index.incrementAndGet();
                
                if (currentIndex < commands.size()) {
                    String command = commands.get(currentIndex);
                    try {
                        executeCommand(command);
                        logger.debug("执行命令 {}/{} (OBO模式): {}", 
                                currentIndex + 1, commands.size(), command);
                    } catch (Exception e) {
                        logger.error("执行命令失败 (OBO模式): " + command, e);
                    }
                    
                    // 如果还有更多命令，继续调度
                    if (currentIndex + 1 < commands.size()) {
                        scheduler.buildTask(plugin, this)
                                .delay(finalIntervalTicks * 50, TimeUnit.MILLISECONDS)
                                .schedule();
                    }
                }
            }
        }).delay(finalIntervalTicks * 50, TimeUnit.MILLISECONDS).schedule();
        
        logger.debug("启动 {} 个命令的逐个执行，间隔 {} ticks", commands.size(), finalIntervalTicks);
    }
    
    /**
     * 执行具体的命令
     */
    private void executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            logger.warn("尝试执行空命令");
            return;
        }
        
        // 移除命令前缀的斜杠（如果有）
        String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
        
        try {
            // 在Velocity中执行命令
            server.getCommandManager().executeAsync(server.getConsoleCommandSource(), cleanCommand)
                    .thenAccept(result -> {
                        if (result) {
                            logger.debug("命令执行成功: /{}", cleanCommand);
                        } else {
                            logger.warn("命令执行失败: /{}", cleanCommand);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("命令执行异常: /" + cleanCommand, throwable);
                        return null;
                    });
                    
        } catch (Exception e) {
            logger.error("执行命令时发生异常: /" + cleanCommand, e);
        }
    }
    
    /**
     * 立即执行任务（用于测试或手动触发）
     */
    public void executeTaskImmediately(Task task) {
        if (task == null) {
            logger.warn("尝试执行空任务");
            return;
        }
        
        logger.info("立即执行任务: {}", task.getId());
        executeTask(task);
    }
    
    /**
     * 测试命令执行
     */
    public void testCommand(String command) {
        logger.info("测试执行命令: {}", command);
        executeCommand(command);
    }
    
    /**
     * 获取服务器实例
     */
    public ProxyServer getServer() {
        return server;
    }
}
