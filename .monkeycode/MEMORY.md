# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [代码结构|代码模式|代码生成|构建方法|测试方法|依赖关系|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

[IP搜索按钮位置调整]
- Date: 2026-05-09
- Context: 用户要求将IP搜索按钮从创建连接对话框移动到主界面顶部工具栏
- Category: 代码结构
- Instructions:
  - IP搜索功能已从 ConnectOperateDialog 移至 MainActivity 顶部工具栏
  - 主界面工具栏按钮顺序：设置 -> IP搜索(放大镜) -> 连接AndroidTV文字 -> 关机
  - 放大镜图标使用 ic_search.xml，IP扫描图标 ic_scan.xml 不再用于此场景

[项目技术栈]
- Date: 2026-05-09
- Context: Agent 在执行代码修改任务时发现
- Category: 代码结构
- Instructions:
  - 项目为 Android 原生应用，使用 Java 编写
  - UI 框架：AppCompat + Material Design + ConstraintLayout
  - 所有图标为自定义 Vector Drawable，存放在 app/src/main/res/drawable/
  - 数据库使用 SQLite，通过 DBManager 管理
  - 构建工具：Gradle
