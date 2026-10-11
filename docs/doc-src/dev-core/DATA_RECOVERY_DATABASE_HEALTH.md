# 数据救援数据库健康检查

数据救援界面运行在独立的 `:repair` 进程中。数据库健康检查是用户主动触发的救援功能，不参与应用启动，也不会在后台自动修改数据库。

## 已发布接口兼容性

以下 v1.12.1 已发布能力保持不变：

- 原始快照导出和导入
- SQL 输入、预设查询和执行结果
- 文件管理提示
- 恢复完成后启动主应用
- `com.ai.assistance.operit.action.OPEN_DATA_RECOVERY` intent action

健康检查区域追加在 SQL 执行器之后，不改变现有入口、数据路径和操作语义。

## 检查内容

`RoomDatabaseHealthManager` 在执行检查前关闭 `:repair` 进程内的 Room singleton，然后直接以只读模式打开 `app_database`。自定义 `DatabaseErrorHandler` 只记录 SQLite corruption 信号，不删除数据库文件。

检查报告包含：

- `app_database` 文件及 WAL、SHM、journal 路径类型和大小
- `PRAGMA quick_check`
- `PRAGMA user_version`
- `PRAGMA foreign_key_check`
- 主应用进程状态
- 隔离副本上的完整 Room schema 与 migration chain 校验

主应用进程确认停止、SQLite 基础检查通过后，检查器会复制 DB、WAL、SHM 和 journal，并从副本移除 `room_master_table`，强制 Room 对实际表、字段、外键和索引执行校验。旧版本数据库会在该副本上先完整运行已发布 migration chain；只有副本迁移和 schema 校验都通过，实时数据库才会开放对应修复动作。

报告分为检查通过、存在明确修复动作、需要人工救援三种状态。数据库缺失、路径类型异常、页损坏、外键违规、更高版本数据库、主进程状态不安全、Room schema 不匹配和 migration chain 无法完成时不会开放修复按钮。

## 修复规则

当前只支持两种能够明确验证结果的操作：

- `quick_check` 只报告索引条目缺失或数量错误时执行 `REINDEX`
- 数据库版本低于当前 Room 版本且其他检查通过时执行已发布 migration

修复按钮需要最近一次报告确认存在支持的操作、隔离副本通过 Room 校验，并要求用户二次确认。执行前和每个写操作前都会确认主应用进程未运行。

## 原件保全

写操作开始前，当前存在的 DB、WAL、SHM 和 journal 会写入 `backup/room_db/room_db_repair_source_<time>_<id>.zip`。保全过程不要求数据库成功打开。

修复动作失败时，界面仍展示该 ZIP 的路径。修复完成后重新执行全部检查；复检未通过时保留报告和原件，不继续执行其他修改。

## 后续范围

ObjectBox、Preferences DataStore、备份格式升级和业务配置修正不属于当前实现。后续存储类型应分别提交独立 PR，并继续使用用户主动触发、先检查、先保全、再执行明确操作的边界。
