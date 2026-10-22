# TimeTools 更新日志

所有重要的项目更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2025-09-07

### 🎉 首次发布

#### Added (新增)
- **完整的定时任务系统**
  - 支持固定时间执行 (`14:00`)
  - 支持时间区间执行 (`09:00-17:00`)
  - 支持间隔执行 (`E` + 间隔时间)
  - 支持时间区间内间隔执行 (`4:00-5:00,E` + 间隔时间)

- **灵活的执行模式**
  - 单个命令执行 (`SINGLE`)
  - 多命令同时执行 (`ALL` - 伪同时，间隔1tick)
  - 多命令顺序执行 (`ONE_BY_ONE` - 可自定义间隔2-120ticks)

- **智能命令解析**
  - 支持包含空格的命令 (如 `/velocity plugins`, `say Hello World`)
  - 支持复杂的多命令组合
  - 自动处理Velocity命令解析器的参数分割问题

- **完善的任务管理功能**
  - `/timetools create` - 创建任务
  - `/timetools list` - 列出所有任务或查看特定任务详情
  - `/timetools delete` - 删除任务 (带二次确认)
  - `/timetools enable/disable` - 启用/禁用任务
  - `/timetools help` - 显示帮助信息
  - `/timetools reload` - 重载配置
  - `/timetools info` - 显示插件信息和运行状态

- **Tab补全支持**
  - 子命令补全
  - 命令示例补全
  - 任务ID补全
  - 时间格式补全
  - 星期设置补全

- **数据持久化**
  - YAML配置文件自动生成
  - 任务数据自动保存和恢复
  - 服务器重启后任务自动恢复运行

- **权限控制**
  - 基于Velocity权限系统
  - `timetools.admin` 管理员权限
  - 控制台默认拥有所有权限

- **完善的配置系统**
  - 可自定义的消息配置
  - 性能参数调优
  - 调试模式支持

#### Technical (技术特性)
- **Java 17** 语法和特性
- **Velocity 3.3.0+** 兼容性
- **模块化架构设计** - 高内聚低耦合
- **完善的错误处理** - 详细的异常信息和日志
- **单元测试覆盖** - 20个测试用例，100%通过率
- **Maven构建系统** - 标准化的构建流程

#### Fixed (修复)
- **命令空格识别问题** - 修复了包含空格的命令无法正确解析的问题
- **任务保存空指针异常** - 修复了保存任务时startTime为null导致的异常
- **调度器插件实例注册** - 修复了Velocity调度器要求正确插件实例的问题

#### Security (安全)
- **权限验证** - 所有管理命令都需要相应权限
- **输入验证** - 严格的命令参数验证和错误处理
- **资源保护** - 防止恶意任务消耗过多系统资源

### 使用示例

```bash
# 每天固定时间执行
/timetools create {/say 服务器公告} 14:00 Eve

# 包含空格的命令
/timetools create {/velocity plugins} E Eve,1m

# 多命令顺序执行
/timetools create {/say 准备重启,/save-all,/restart,Obo,60} 23:55 Eve

# 时间区间内间隔执行
/timetools create {/weather clear} 09:00-17:00,E Eve,30m

# 工作日备份任务
/timetools create {/save-all,/say 备份完成} 03:00 Mon,Tue,Wed,Thu,Fri
```

### 技术规格

- **最低Java版本**: Java 17
- **最低Velocity版本**: 3.3.0
- **依赖**: SnakeYAML 2.0
- **测试框架**: JUnit 5
- **构建工具**: Maven 3.6+
- **许可证**: MIT License

### 已知限制

- 时间检查精度为分钟级别
- 最大任务数量建议不超过1000个
- 执行间隔最小为2ticks，最大为120ticks
- 所有命令以控制台身份执行

---

## 版本说明

- **主版本号**: 重大功能更新或不兼容的API更改
- **次版本号**: 向后兼容的功能新增
- **修订版本号**: 向后兼容的问题修复

## 贡献

欢迎提交Issue和Pull Request来帮助改进TimeTools插件！

## 许可证

本项目采用 [MIT License](LICENSE) 开源。
