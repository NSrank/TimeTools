# TimeTools - Velocity定时任务插件

TimeTools是一款功能强大的Velocity代理服务器插件，专门用于定时执行指定命令。支持多种复杂的调度模式，包括固定时间执行、时间区间执行、间隔执行等。
 **注意**：本插件由 AI 开发。

## 功能特性

- **多种调度模式**：支持固定时间、时间区间、间隔执行、时间区间内间隔执行
- **灵活的执行模式**：支持单个命令、多命令同时执行、多命令顺序执行
- **星期设置**：支持指定特定星期或每天执行
- **持久化存储**：任务配置自动保存到YAML文件
- **实时管理**：支持任务的启用/禁用、删除、查看等操作
- **权限控制**：基于Velocity权限系统的访问控制

## 安装要求

- Velocity 3.3.0+
- Java 17+

## 安装方法

1. 下载TimeTools插件jar文件
2. 将jar文件放入Velocity服务器的`plugins`目录
3. 重启Velocity服务器
4. 插件将自动创建配置文件

## 命令用法

### 基本命令格式

```
/timetools <子命令> [参数...]
```

### 子命令列表

| 命令 | 描述 | 用法 |
|------|------|------|
| `create` | 创建新任务 | `/timetools create {命令} 时间 星期` |
| `list` | 列出所有任务或查看特定任务详情 | `/timetools list [任务ID]` |
| `delete` | 删除任务（需要二次确认） | `/timetools delete <任务ID>` |
| `enable` | 启用任务 | `/timetools enable <任务ID>` |
| `disable` | 禁用任务 | `/timetools disable <任务ID>` |
| `help` | 显示帮助信息 | `/timetools help` |
| `reload` | 重载配置 | `/timetools reload` |
| `info` | 显示插件信息 | `/timetools info` |

## 创建任务详细说明

### 命令格式

```
/timetools create {命令部分} 时间部分 星期部分
```

### 命令部分格式

#### 单个命令
```
{/command}
```

#### 多个命令同时执行
```
{/command1},{/command2},{/command3},All
```

#### 多个命令顺序执行
```
{/command1},{/command2},{/command3},Obo,间隔ticks
```

- `Obo`：One by one，表示顺序执行
- `All`：表示同时执行（伪同时，间隔1tick）
- 间隔ticks：2-120之间的数字，表示顺序执行时的间隔

### 时间部分格式

#### 固定时间
```
HH:MM
```
例如：`14:00`（每天下午2点执行）

#### 时间区间
```
HH:MM-HH:MM
```
例如：`09:00-17:00`（每天上午9点到下午5点之间执行）

#### 间隔执行
```
E
```
必须配合星期部分的间隔时间使用

#### 时间区间内间隔执行
```
HH:MM-HH:MM,E
```
例如：`09:00-17:00,E`（上午9点到下午5点之间按间隔执行）

### 星期部分格式

#### 每天执行
```
Eve
```

#### 特定星期
```
Mon,Tue,Wed,Thu,Fri,Sat,Sun
```
可以组合使用，例如：`Mon,Wed,Fri`

#### 间隔时间
```
数字+单位
```
- 单位：`s`（秒）、`m`（分钟）、`h`（小时）
- 例如：`1m`（每分钟）、`30s`（每30秒）、`2h`（每2小时）

#### 组合使用
```
Eve,1m
```
表示每天每分钟执行

## 使用示例

### 1. 每天固定时间执行单个命令
```
/timetools create {/say 服务器定时公告} 14:00 Eve
```
每天下午2点执行say命令

### 2. 每分钟执行命令
```
/timetools create {/list} E Eve,1m
```
每天每分钟执行list命令

### 3. 时间区间内间隔执行
```
/timetools create {/weather clear} 09:00-17:00,E Eve,30m
```
每天上午9点到下午5点之间每30分钟清理天气

### 4. 多命令顺序执行
```
/timetools create {/say 准备重启},{/save-all},{/restart},Obo,60 23:55 Eve
```
每天晚上11:55分顺序执行三个命令，间隔60ticks

### 5. 工作日执行
```
/timetools create {/backup} 02:00 Mon,Tue,Wed,Thu,Fri
```
工作日凌晨2点执行备份

## 权限

- `timetools.admin`：管理员权限，可以使用所有TimeTools命令
- 控制台默认拥有所有权限

## 配置文件

插件会在`plugins/timetools/`目录下创建以下文件：

- `config.yml`：主配置文件（首次运行时从插件内置模板复制）
- `tasks.yml`：任务数据文件（自动生成和维护）

插件内置资源文件：

- `velocity-plugin.json`：Velocity插件描述文件
- `config.yml`：默认配置模板
- `tasks-example.yml`：任务配置示例文件
- `build.properties`：构建信息文件

### config.yml 配置项

```yaml
# 调试模式
debug: false

# 检查间隔（秒）
check-interval-seconds: 60

# 最大执行线程数
max-execution-threads: 5

# 消息配置
messages:
  task-created: "§a任务创建成功！任务ID: {id}"
  task-deleted: "§a任务删除成功！任务ID: {id}"
  task-enabled: "§a任务已启用！任务ID: {id}"
  task-disabled: "§c任务已禁用！任务ID: {id}"
  task-not-found: "§c未找到任务ID: {id}"
  invalid-command: "§c无效的命令格式！请使用 /timetools help 查看帮助"
  no-permission: "§c你没有权限执行此命令！"
```

## 注意事项

1. **时间精度**：插件的时间检查精度为分钟级别
2. **命令执行**：所有命令都以控制台身份执行
3. **任务ID**：每个任务都有唯一的UUID作为标识
4. **数据持久化**：任务数据会自动保存，服务器重启后自动恢复
5. **性能考虑**：间隔任务使用独立的调度器，不会影响主调度器性能

## 故障排除

### 常见问题

1. **任务不执行**
   - 检查任务是否启用：`/timetools list <任务ID>`
   - 检查时间格式是否正确
   - 查看控制台日志

2. **命令执行失败**
   - 确认命令在控制台中可以正常执行
   - 检查命令语法是否正确

3. **配置丢失**
   - 检查`plugins/timetools/tasks.yml`文件是否存在
   - 确认文件权限正确

### 日志查看

插件会在控制台输出详细的执行日志，包括：
- 任务创建/删除/修改
- 任务执行状态
- 错误信息

## 开发信息

- **版本**：1.0-SNAPSHOT
- **兼容性**：Velocity 3.3.0+
- **开发语言**：Java 17
- **构建工具**：Maven

## 更新日志

### v1.0 (2025-09-07)

#### 🎉 首次发布
- **完整的定时任务系统**：支持固定时间、时间区间、间隔执行、时间区间内间隔执行等多种调度模式
- **灵活的执行模式**：支持单个命令、多命令同时执行(All)、多命令顺序执行(Obo)
- **智能命令解析**：支持包含空格的命令，如 `/velocity plugins`、`say Hello World` 等
- **完善的任务管理**：创建、删除、启用、禁用、查看任务详情等功能
- **Tab补全支持**：为所有命令提供智能补全建议
- **数据持久化**：任务配置自动保存到YAML文件，服务器重启后自动恢复
- **权限控制**：基于Velocity权限系统的访问控制

#### ✨ 核心功能
- **多种时间格式支持**：
  - 固定时间：`14:00`
  - 时间区间：`09:00-17:00`
  - 间隔执行：`E` + 间隔时间
  - 时间区间内间隔：`4:00-5:00,E` + 间隔时间
- **星期设置**：支持 `Eve`(每天)、`Mon,Tue,Wed,Thu,Fri`(工作日) 等
- **间隔时间单位**：支持秒(`s`)、分钟(`m`)、小时(`h`)
- **执行间隔控制**：Obo模式支持2-120ticks的自定义间隔

#### 🔧 技术特性
- **Java 17** 语法和特性
- **Velocity 3.3.0+** 兼容
- **模块化架构**设计，高内聚低耦合
- **完善的错误处理**和日志系统
- **单元测试覆盖**：20个测试用例，100%通过率

#### 🐛 已修复的问题
- **命令空格识别**：修复了包含空格的命令无法正确解析的问题
- **任务保存异常**：修复了任务保存时的空指针异常
- **调度器注册**：修复了插件实例注册问题，确保调度器正常工作

#### 📝 使用示例
```bash
# 每天固定时间执行
/timetools create {/say 服务器公告} 14:00 Eve

# 包含空格的命令
/timetools create {/velocity plugins} E Eve,1m

# 多命令顺序执行
/timetools create {/say 准备重启,/save-all,/restart,Obo,60} 23:55 Eve

# 时间区间内间隔执行
/timetools create {/weather clear} 09:00-17:00,E Eve,30m
```

---

## 许可证

本项目采用 MIT 许可证开源。

### MIT License

```
MIT License

Copyright (c) 2025 NSrank & Augment

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## English Documentation

# TimeTools - Velocity Scheduled Task Plugin

TimeTools is a powerful Velocity proxy server plugin designed for executing scheduled commands. It supports various complex scheduling modes including fixed time execution, time range execution, interval execution, and more.

## Features

- **Multiple Scheduling Modes**: Fixed time, time range, interval execution, time range with interval
- **Flexible Execution Modes**: Single command, multiple commands simultaneously, multiple commands sequentially
- **Day Settings**: Support for specific days of the week or daily execution
- **Persistent Storage**: Task configurations automatically saved to YAML files
- **Real-time Management**: Support for enabling/disabling, deleting, and viewing tasks
- **Permission Control**: Access control based on Velocity permission system

## Requirements

- Velocity 3.3.0+
- Java 17+

## Installation

1. Download the TimeTools plugin jar file
2. Place the jar file in the Velocity server's `plugins` directory
3. Restart the Velocity server
4. The plugin will automatically create configuration files

For detailed usage instructions, please refer to the Chinese documentation above.
