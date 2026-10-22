package org.plugin.timetools.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 插件信息工具类
 * 
 * 提供插件的版本、构建信息等
 */
public class PluginInfo {
    
    private static final String UNKNOWN = "Unknown";
    private static PluginInfo instance;
    
    private final String version;
    private final String buildTime;
    private final String gitCommit;
    private final String javaVersion;
    
    private PluginInfo() {
        Properties props = new Properties();
        
        // 尝试加载构建信息
        try (InputStream is = getClass().getResourceAsStream("/build.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // 忽略错误，使用默认值
        }
        
        this.version = props.getProperty("version", "1.0-SNAPSHOT");
        this.buildTime = props.getProperty("build.time", UNKNOWN);
        this.gitCommit = props.getProperty("git.commit", UNKNOWN);
        this.javaVersion = System.getProperty("java.version", UNKNOWN);
    }
    
    /**
     * 获取单例实例
     */
    public static PluginInfo getInstance() {
        if (instance == null) {
            instance = new PluginInfo();
        }
        return instance;
    }
    
    /**
     * 获取插件版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取构建时间
     */
    public String getBuildTime() {
        return buildTime;
    }
    
    /**
     * 获取Git提交哈希
     */
    public String getGitCommit() {
        return gitCommit;
    }
    
    /**
     * 获取Java版本
     */
    public String getJavaVersion() {
        return javaVersion;
    }
    
    /**
     * 获取完整的版本信息
     */
    public String getFullVersionInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("TimeTools v").append(version);
        
        if (!UNKNOWN.equals(buildTime)) {
            sb.append(" (").append(buildTime).append(")");
        }
        
        if (!UNKNOWN.equals(gitCommit)) {
            sb.append(" [").append(gitCommit.substring(0, Math.min(7, gitCommit.length()))).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取系统信息
     */
    public String getSystemInfo() {
        return String.format("Java %s, OS: %s %s", 
                javaVersion,
                System.getProperty("os.name", UNKNOWN),
                System.getProperty("os.version", UNKNOWN));
    }
    
    /**
     * 获取插件描述
     */
    public String getDescription() {
        return "定时执行命令的Velocity插件 - A Velocity plugin for scheduled command execution";
    }
    
    /**
     * 获取作者信息
     */
    public String getAuthors() {
        return "TimeTools Team";
    }
    
    /**
     * 获取项目URL
     */
    public String getUrl() {
        return "https://github.com/timetools/timetools";
    }

    /**
     * 获取许可证信息
     */
    public String getLicense() {
        return "MIT License";
    }

    /**
     * 获取许可证URL
     */
    public String getLicenseUrl() {
        return "https://opensource.org/licenses/MIT";
    }

    /**
     * 获取版本标签
     */
    public String getVersionTag() {
        return "v" + version;
    }
}
