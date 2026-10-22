package org.plugin.timetools.model;

/**
 * 执行模式枚举
 */
public enum ExecutionMode {
    /**
     * 单个命令执行
     */
    SINGLE,
    
    /**
     * 所有命令同时执行（伪同时，间隔1tick）
     */
    ALL,
    
    /**
     * 命令逐个执行（One by one）
     */
    ONE_BY_ONE
}
