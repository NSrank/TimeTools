package org.plugin.timetools.parser;

import org.plugin.timetools.model.ExecutionMode;
import org.plugin.timetools.model.Task;
import org.plugin.timetools.model.TaskScheduleType;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令解析器
 * 
 * 负责解析复杂的任务创建命令
 */
public class CommandParser {
    
    // 时间格式正则表达式
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
    
    // 时间区间格式正则表达式
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})-(\\d{1,2}):(\\d{2})$");
    
    // 间隔时间格式正则表达式
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("^(\\d+)([smh])$");
    
    // 星期映射
    private static final Map<String, DayOfWeek> DAY_MAP = new HashMap<>();
    static {
        DAY_MAP.put("MON", DayOfWeek.MONDAY);
        DAY_MAP.put("TUE", DayOfWeek.TUESDAY);
        DAY_MAP.put("WED", DayOfWeek.WEDNESDAY);
        DAY_MAP.put("THU", DayOfWeek.THURSDAY);
        DAY_MAP.put("FRI", DayOfWeek.FRIDAY);
        DAY_MAP.put("SAT", DayOfWeek.SATURDAY);
        DAY_MAP.put("SUN", DayOfWeek.SUNDAY);
    }
    
    /**
     * 解析任务创建命令
     * 
     * 支持的格式：
     * 1. /timetools create {/command} 4:00 Eve
     * 2. /timetools create {/command} E Eve,1m
     * 3. /timetools create {/command} 4:00-5:00,E Eve,1m
     * 4. /timetools create {/command1},{/command2},{/command3},Obo,3 4:00 Eve
     */
    public static Task parseCreateCommand(String[] args) throws ParseException {
        if (args.length < 4) {
            throw new ParseException("参数不足，请使用 /timetools help 查看帮助");
        }
        
        // 解析命令部分
        String commandPart = args[1];
        ParsedCommands parsedCommands = parseCommands(commandPart);
        
        // 解析时间部分
        String timePart = args[2];
        ParsedTime parsedTime = parseTime(timePart);
        
        // 解析星期部分
        String dayPart = args[3];
        ParsedDays parsedDays = parseDays(dayPart);
        
        // 验证组合的有效性
        validateCombination(parsedTime, parsedDays);
        
        // 创建任务
        return createTask(parsedCommands, parsedTime, parsedDays);
    }
    
    /**
     * 解析命令部分
     */
    private static ParsedCommands parseCommands(String commandPart) throws ParseException {
        if (!commandPart.startsWith("{") || !commandPart.endsWith("}")) {
            throw new ParseException("命令格式错误，必须用{}包围");
        }

        String content = commandPart.substring(1, commandPart.length() - 1);

        // 智能分割：考虑命令中可能包含空格
        List<String> parts = smartSplit(content);

        List<String> commands = new ArrayList<>();
        ExecutionMode executionMode = ExecutionMode.SINGLE;
        int executionInterval = 0;

        for (String part : parts) {
            part = part.trim();

            if (part.equals("Obo")) {
                executionMode = ExecutionMode.ONE_BY_ONE;
            } else if (part.equals("All")) {
                executionMode = ExecutionMode.ALL;
            } else if (part.matches("\\d+")) {
                executionInterval = Integer.parseInt(part);
                if (executionInterval < 2 || executionInterval > 120) {
                    throw new ParseException("执行间隔必须在2-120ticks之间");
                }
            } else if (part.startsWith("/") || part.contains(" ")) {
                // 允许命令包含空格
                commands.add(part);
            } else if (!part.isEmpty()) {
                throw new ParseException("无效的命令部分: " + part);
            }
        }

        if (commands.isEmpty()) {
            throw new ParseException("至少需要一个命令");
        }

        // 如果是多命令但没有指定执行模式，默认为ALL
        if (commands.size() > 1 && executionMode == ExecutionMode.SINGLE) {
            executionMode = ExecutionMode.ALL;
        }

        return new ParsedCommands(commands, executionMode, executionInterval);
    }

    /**
     * 智能分割命令字符串，考虑命令中可能包含空格
     */
    private static List<String> smartSplit(String content) {
        List<String> parts = new ArrayList<>();

        // 更简单的方法：使用正则表达式来识别模式
        // 先处理特殊情况：如果没有逗号，直接返回
        if (!content.contains(",")) {
            parts.add(content.trim());
            return parts;
        }

        // 按逗号分割，但要考虑命令中的空格
        String[] rawParts = content.split(",");
        List<String> commands = new ArrayList<>();
        List<String> keywords = new ArrayList<>();

        for (String part : rawParts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (isKeyword(part)) {
                keywords.add(part);
            } else {
                commands.add(part);
            }
        }

        // 添加所有命令
        parts.addAll(commands);
        // 添加所有关键字
        parts.addAll(keywords);

        return parts;
    }

    /**
     * 检查是否是关键字
     */
    private static boolean isKeyword(String part) {
        return part.equals("Obo") ||
               part.equals("All") ||
               part.matches("\\d+");
    }
    
    /**
     * 解析时间部分
     */
    private static ParsedTime parseTime(String timePart) throws ParseException {
        boolean hasInterval = timePart.contains("E");
        boolean hasTimeRange = timePart.contains("-") && !timePart.equals("E");
        
        if (timePart.equals("E")) {
            // 纯间隔模式
            return new ParsedTime(TaskScheduleType.INTERVAL, null, null, true, 0, null);
        }
        
        if (hasTimeRange && hasInterval) {
            // 时间区间 + 间隔模式
            String[] parts = timePart.split(",");
            if (parts.length != 2 || !parts[1].equals("E")) {
                throw new ParseException("时间区间间隔格式错误");
            }
            
            String timeRangePart = parts[0];
            Matcher matcher = TIME_RANGE_PATTERN.matcher(timeRangePart);
            if (!matcher.matches()) {
                throw new ParseException("时间区间格式错误，应为 HH:MM-HH:MM");
            }
            
            LocalTime startTime = parseTimeString(matcher.group(1), matcher.group(2));
            LocalTime endTime = parseTimeString(matcher.group(3), matcher.group(4));
            
            if (!endTime.isAfter(startTime)) {
                throw new ParseException("结束时间必须晚于开始时间");
            }
            
            return new ParsedTime(TaskScheduleType.TIME_RANGE_WITH_INTERVAL, startTime, endTime, true, 0, null);
            
        } else if (hasTimeRange) {
            // 纯时间区间模式
            Matcher matcher = TIME_RANGE_PATTERN.matcher(timePart);
            if (!matcher.matches()) {
                throw new ParseException("时间区间格式错误，应为 HH:MM-HH:MM");
            }
            
            LocalTime startTime = parseTimeString(matcher.group(1), matcher.group(2));
            LocalTime endTime = parseTimeString(matcher.group(3), matcher.group(4));
            
            if (!endTime.isAfter(startTime)) {
                throw new ParseException("结束时间必须晚于开始时间");
            }
            
            return new ParsedTime(TaskScheduleType.TIME_RANGE, startTime, endTime, false, 0, null);
            
        } else {
            // 固定时间模式
            Matcher matcher = TIME_PATTERN.matcher(timePart);
            if (!matcher.matches()) {
                throw new ParseException("时间格式错误，应为 HH:MM");
            }
            
            LocalTime time = parseTimeString(matcher.group(1), matcher.group(2));
            return new ParsedTime(TaskScheduleType.FIXED_TIME, time, null, false, 0, null);
        }
    }
    
    /**
     * 解析星期部分
     */
    private static ParsedDays parseDays(String dayPart) throws ParseException {
        String[] parts = dayPart.split(",");
        
        boolean everyDay = false;
        Set<DayOfWeek> daysOfWeek = new HashSet<>();
        long intervalTicks = 0;
        String intervalUnit = null;
        
        for (String part : parts) {
            part = part.trim().toUpperCase();
            
            if (part.equals("EVE")) {
                everyDay = true;
            } else if (DAY_MAP.containsKey(part)) {
                daysOfWeek.add(DAY_MAP.get(part));
            } else if (INTERVAL_PATTERN.matcher(part.toLowerCase()).matches()) {
                Matcher matcher = INTERVAL_PATTERN.matcher(part.toLowerCase());
                if (matcher.matches()) {
                    int value = Integer.parseInt(matcher.group(1));
                    String unit = matcher.group(2);
                    
                    intervalUnit = unit;
                    switch (unit) {
                        case "s":
                            intervalTicks = value * 20L; // 1秒 = 20ticks
                            break;
                        case "m":
                            intervalTicks = value * 20L * 60L; // 1分钟 = 1200ticks
                            break;
                        case "h":
                            intervalTicks = value * 20L * 60L * 60L; // 1小时 = 72000ticks
                            break;
                        default:
                            throw new ParseException("无效的时间单位: " + unit);
                    }
                }
            } else {
                throw new ParseException("无效的星期或间隔: " + part);
            }
        }
        
        return new ParsedDays(everyDay, daysOfWeek, intervalTicks, intervalUnit);
    }
    
    /**
     * 解析时间字符串
     */
    private static LocalTime parseTimeString(String hour, String minute) throws ParseException {
        try {
            int h = Integer.parseInt(hour);
            int m = Integer.parseInt(minute);
            
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                throw new ParseException("时间值超出范围");
            }
            
            return LocalTime.of(h, m);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new ParseException("时间格式错误");
        }
    }
    
    /**
     * 验证参数组合的有效性
     */
    private static void validateCombination(ParsedTime parsedTime, ParsedDays parsedDays) throws ParseException {
        // E模式必须搭配时间区间或间隔时间
        if (parsedTime.hasInterval && parsedTime.scheduleType == TaskScheduleType.INTERVAL) {
            if (parsedDays.intervalTicks <= 0) {
                throw new ParseException("E模式必须指定间隔时间");
            }
        }
        
        // 时间区间间隔模式必须有间隔时间
        if (parsedTime.scheduleType == TaskScheduleType.TIME_RANGE_WITH_INTERVAL) {
            if (parsedDays.intervalTicks <= 0) {
                throw new ParseException("时间区间间隔模式必须指定间隔时间");
            }
        }
    }
    
    /**
     * 创建任务对象
     */
    private static Task createTask(ParsedCommands commands, ParsedTime time, ParsedDays days) {
        return new Task(
                null, // ID将自动生成
                commands.commands,
                time.scheduleType,
                time.startTime,
                time.endTime,
                days.daysOfWeek,
                days.everyDay,
                days.intervalTicks,
                days.intervalUnit,
                commands.executionMode,
                commands.executionInterval,
                true // 默认启用
        );
    }
    
    // 内部类用于存储解析结果
    private static class ParsedCommands {
        final List<String> commands;
        final ExecutionMode executionMode;
        final int executionInterval;
        
        ParsedCommands(List<String> commands, ExecutionMode executionMode, int executionInterval) {
            this.commands = commands;
            this.executionMode = executionMode;
            this.executionInterval = executionInterval;
        }
    }
    
    private static class ParsedTime {
        final TaskScheduleType scheduleType;
        final LocalTime startTime;
        final LocalTime endTime;
        final boolean hasInterval;
        final long intervalTicks;
        final String intervalUnit;
        
        ParsedTime(TaskScheduleType scheduleType, LocalTime startTime, LocalTime endTime,
                  boolean hasInterval, long intervalTicks, String intervalUnit) {
            this.scheduleType = scheduleType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.hasInterval = hasInterval;
            this.intervalTicks = intervalTicks;
            this.intervalUnit = intervalUnit;
        }
    }
    
    private static class ParsedDays {
        final boolean everyDay;
        final Set<DayOfWeek> daysOfWeek;
        final long intervalTicks;
        final String intervalUnit;
        
        ParsedDays(boolean everyDay, Set<DayOfWeek> daysOfWeek, long intervalTicks, String intervalUnit) {
            this.everyDay = everyDay;
            this.daysOfWeek = daysOfWeek;
            this.intervalTicks = intervalTicks;
            this.intervalUnit = intervalUnit;
        }
    }
    
    /**
     * 解析异常类
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
}
