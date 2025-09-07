package org.plugin.timetools;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import org.plugin.timetools.command.TimeToolsCommand;
import org.plugin.timetools.config.ConfigManager;
import org.plugin.timetools.manager.TaskManager;
import org.plugin.timetools.scheduler.TaskScheduler;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "timetools",
        name = "TimeTools",
        version = "1.0-SNAPSHOT",
        description = "定时执行命令的Velocity插件",
        authors = {"NSrank", "Augment"}
)
public class TimeTools {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private ConfigManager configManager;
    private TaskManager taskManager;
    private TaskScheduler taskScheduler;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        logger.info("===================================");
        logger.info("TimeTools v1.0 已加载");
        logger.info("作者：NSrank & Augment");
        logger.info("===================================");
        
        try {
            // 初始化配置管理器
            this.configManager = new ConfigManager(dataDirectory, logger);

            // 初始化任务管理器
            this.taskManager = new TaskManager(configManager, logger);

            // 初始化任务调度器
            this.taskScheduler = new TaskScheduler(this, server, taskManager, logger);

            // 注册命令
            CommandManager commandManager = server.getCommandManager();
            commandManager.register(commandManager.metaBuilder("timetools")
                    .plugin(this)
                    .build(), new TimeToolsCommand(taskManager, taskScheduler, logger));

            // 启动任务调度器
            taskScheduler.start();

            logger.info("TimeTools插件初始化完成！");
        } catch (Exception e) {
            logger.error("TimeTools插件初始化失败", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("TimeTools插件正在关闭...");

        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }

        if (taskManager != null) {
            taskManager.saveAllTasks();
        }

        logger.info("TimeTools插件已关闭");
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
