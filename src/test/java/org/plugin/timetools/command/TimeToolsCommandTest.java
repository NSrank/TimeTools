package org.plugin.timetools.command;

import org.junit.jupiter.api.Test;
import org.plugin.timetools.manager.TaskManager;
import org.plugin.timetools.scheduler.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeToolsCommand测试类
 */
public class TimeToolsCommandTest {
    
    private final Logger logger = LoggerFactory.getLogger(TimeToolsCommandTest.class);
    
    @Test
    public void testReconstructCreateArgs() throws Exception {
        // 创建一个TimeToolsCommand实例用于测试
        TimeToolsCommand command = new TimeToolsCommand(null, null, logger);
        
        // 使用反射访问私有方法
        Method method = TimeToolsCommand.class.getDeclaredMethod("reconstructCreateArgs", String[].class);
        method.setAccessible(true);
        
        // 测试被空格分割的参数
        String[] splitArgs = {"create", "{/velocity", "plugins}", "E", "Eve,1m"};
        String[] result = (String[]) method.invoke(command, (Object) splitArgs);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("create", result[0]);
        assertEquals("{/velocity plugins}", result[1]);
        assertEquals("E", result[2]);
        assertEquals("Eve,1m", result[3]);
    }
    
    @Test
    public void testReconstructCreateArgsMultipleSpaces() throws Exception {
        TimeToolsCommand command = new TimeToolsCommand(null, null, logger);
        
        Method method = TimeToolsCommand.class.getDeclaredMethod("reconstructCreateArgs", String[].class);
        method.setAccessible(true);
        
        // 测试多个空格的情况
        String[] splitArgs = {"create", "{/say", "Hello", "World}", "14:00", "Eve"};
        String[] result = (String[]) method.invoke(command, (Object) splitArgs);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("create", result[0]);
        assertEquals("{/say Hello World}", result[1]);
        assertEquals("14:00", result[2]);
        assertEquals("Eve", result[3]);
    }
    
    @Test
    public void testReconstructCreateArgsComplexCommand() throws Exception {
        TimeToolsCommand command = new TimeToolsCommand(null, null, logger);
        
        Method method = TimeToolsCommand.class.getDeclaredMethod("reconstructCreateArgs", String[].class);
        method.setAccessible(true);
        
        // 测试复杂命令的情况
        String[] splitArgs = {"create", "{/velocity", "plugins,say", "Hello", "World,All}", "09:00", "Eve"};
        String[] result = (String[]) method.invoke(command, (Object) splitArgs);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("create", result[0]);
        assertEquals("{/velocity plugins,say Hello World,All}", result[1]);
        assertEquals("09:00", result[2]);
        assertEquals("Eve", result[3]);
    }
    
    @Test
    public void testReconstructCreateArgsNoChange() throws Exception {
        TimeToolsCommand command = new TimeToolsCommand(null, null, logger);
        
        Method method = TimeToolsCommand.class.getDeclaredMethod("reconstructCreateArgs", String[].class);
        method.setAccessible(true);
        
        // 测试不需要重组的情况
        String[] normalArgs = {"create", "{/say hello}", "14:00", "Eve"};
        String[] result = (String[]) method.invoke(command, (Object) normalArgs);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("create", result[0]);
        assertEquals("{/say hello}", result[1]);
        assertEquals("14:00", result[2]);
        assertEquals("Eve", result[3]);
    }
    
    @Test
    public void testReconstructCreateArgsInvalidFormat() throws Exception {
        TimeToolsCommand command = new TimeToolsCommand(null, null, logger);
        
        Method method = TimeToolsCommand.class.getDeclaredMethod("reconstructCreateArgs", String[].class);
        method.setAccessible(true);
        
        // 测试无效格式（没有{}）
        String[] invalidArgs = {"create", "velocity", "plugins", "14:00", "Eve"};
        String[] result = (String[]) method.invoke(command, (Object) invalidArgs);
        
        // 应该返回原始参数，让后续处理报错
        assertArrayEquals(invalidArgs, result);
    }
}
