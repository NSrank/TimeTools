package org.plugin.timetools.parser;

import org.junit.jupiter.api.Test;
import org.plugin.timetools.model.ExecutionMode;
import org.plugin.timetools.model.Task;
import org.plugin.timetools.model.TaskScheduleType;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandParser测试类
 */
public class CommandParserTest {
    
    @Test
    public void testParseSimpleFixedTimeCommand() throws CommandParser.ParseException {
        String[] args = {"create", "{/say hello}", "14:00", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(1, task.getCommands().size());
        assertEquals("/say hello", task.getCommands().get(0));
        assertEquals(TaskScheduleType.FIXED_TIME, task.getScheduleType());
        assertEquals(LocalTime.of(14, 0), task.getStartTime());
        assertTrue(task.isEveryDay());
        assertEquals(ExecutionMode.SINGLE, task.getExecutionMode());
    }
    
    @Test
    public void testParseMultipleCommandsAllMode() throws CommandParser.ParseException {
        String[] args = {"create", "{/say hello,/say world,All}", "09:00", "Mon,Tue"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(2, task.getCommands().size());
        assertEquals("/say hello", task.getCommands().get(0));
        assertEquals("/say world", task.getCommands().get(1));
        assertEquals(ExecutionMode.ALL, task.getExecutionMode());
        assertFalse(task.isEveryDay());
        assertTrue(task.getDaysOfWeek().contains(DayOfWeek.MONDAY));
        assertTrue(task.getDaysOfWeek().contains(DayOfWeek.TUESDAY));
    }
    
    @Test
    public void testParseOneByOneMode() throws CommandParser.ParseException {
        String[] args = {"create", "{/cmd1,/cmd2,Obo,5}", "10:30", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(2, task.getCommands().size());
        assertEquals(ExecutionMode.ONE_BY_ONE, task.getExecutionMode());
        assertEquals(5, task.getExecutionInterval());
    }
    
    @Test
    public void testParseTimeRange() throws CommandParser.ParseException {
        String[] args = {"create", "{/weather clear}", "09:00-17:00", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(TaskScheduleType.TIME_RANGE, task.getScheduleType());
        assertEquals(LocalTime.of(9, 0), task.getStartTime());
        assertEquals(LocalTime.of(17, 0), task.getEndTime());
    }
    
    @Test
    public void testParseIntervalMode() throws CommandParser.ParseException {
        String[] args = {"create", "{/list}", "E", "Eve,1m"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(TaskScheduleType.INTERVAL, task.getScheduleType());
        assertEquals(1200L, task.getIntervalTicks()); // 1分钟 = 1200ticks
        assertEquals("m", task.getIntervalUnit());
    }
    
    @Test
    public void testParseTimeRangeWithInterval() throws CommandParser.ParseException {
        String[] args = {"create", "{/backup}", "02:00-04:00,E", "Eve,30m"};
        Task task = CommandParser.parseCreateCommand(args);
        
        assertNotNull(task);
        assertEquals(TaskScheduleType.TIME_RANGE_WITH_INTERVAL, task.getScheduleType());
        assertEquals(LocalTime.of(2, 0), task.getStartTime());
        assertEquals(LocalTime.of(4, 0), task.getEndTime());
        assertEquals(36000L, task.getIntervalTicks()); // 30分钟 = 36000ticks
    }
    
    @Test
    public void testInvalidTimeFormat() {
        String[] args = {"create", "{/say hello}", "25:00", "Eve"};
        assertThrows(CommandParser.ParseException.class, () -> {
            CommandParser.parseCreateCommand(args);
        });
    }
    
    @Test
    public void testInvalidExecutionInterval() {
        String[] args = {"create", "{/cmd1,/cmd2,Obo,150}", "10:00", "Eve"};
        assertThrows(CommandParser.ParseException.class, () -> {
            CommandParser.parseCreateCommand(args);
        });
    }
    
    @Test
    public void testInvalidDayOfWeek() {
        String[] args = {"create", "{/say hello}", "14:00", "InvalidDay"};
        assertThrows(CommandParser.ParseException.class, () -> {
            CommandParser.parseCreateCommand(args);
        });
    }
    
    @Test
    public void testEmptyCommand() {
        String[] args = {"create", "{}", "14:00", "Eve"};
        assertThrows(CommandParser.ParseException.class, () -> {
            CommandParser.parseCreateCommand(args);
        });
    }
    
    @Test
    public void testInsufficientArguments() {
        String[] args = {"create", "{/say hello}"};
        assertThrows(CommandParser.ParseException.class, () -> {
            CommandParser.parseCreateCommand(args);
        });
    }

    @Test
    public void testCommandWithSpaces() throws CommandParser.ParseException {
        String[] args = {"create", "{velocity plugins}", "14:00", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);

        assertNotNull(task);
        assertEquals(1, task.getCommands().size());
        assertEquals("velocity plugins", task.getCommands().get(0));
        assertEquals(TaskScheduleType.FIXED_TIME, task.getScheduleType());
        assertEquals(LocalTime.of(14, 0), task.getStartTime());
        assertTrue(task.isEveryDay());
    }

    @Test
    public void testMultipleCommandsWithSpaces() throws CommandParser.ParseException {
        String[] args = {"create", "{velocity plugins,say Hello World,All}", "09:00", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);

        assertNotNull(task);
        assertEquals(2, task.getCommands().size());
        assertEquals("velocity plugins", task.getCommands().get(0));
        assertEquals("say Hello World", task.getCommands().get(1));
        assertEquals(ExecutionMode.ALL, task.getExecutionMode());
    }

    @Test
    public void testSlashCommandWithSpaces() throws CommandParser.ParseException {
        String[] args = {"create", "{/velocity plugins}", "E", "Eve,1m"};
        Task task = CommandParser.parseCreateCommand(args);

        assertNotNull(task);
        assertEquals(1, task.getCommands().size());
        assertEquals("/velocity plugins", task.getCommands().get(0));
        assertEquals(TaskScheduleType.INTERVAL, task.getScheduleType());
        assertEquals(1200L, task.getIntervalTicks()); // 1分钟 = 1200ticks
        assertEquals("m", task.getIntervalUnit());
    }

    @Test
    public void testMixedCommandsWithSpaces() throws CommandParser.ParseException {
        String[] args = {"create", "{/velocity plugins,say Hello World,/list players,Obo,5}", "14:00", "Eve"};
        Task task = CommandParser.parseCreateCommand(args);

        assertNotNull(task);
        assertEquals(3, task.getCommands().size());
        assertEquals("/velocity plugins", task.getCommands().get(0));
        assertEquals("say Hello World", task.getCommands().get(1));
        assertEquals("/list players", task.getCommands().get(2));
        assertEquals(ExecutionMode.ONE_BY_ONE, task.getExecutionMode());
        assertEquals(5, task.getExecutionInterval());
    }
}
