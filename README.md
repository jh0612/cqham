创建项目
学习网址
https://cloud.tencent.com/developer/article/2171652
https://reiticia.github.io/lesson079.html

好的，我已经了解了现有项目的结构。现在让我整理一下需求并开始实现：

需求总结：

主页面新增按钮：点击弹出对话框，输入账号密码或 API Key

账号密码是硬编码的（比如 admin/123456），匹配则使用代码中预设的 API Key
不匹配则用户必须输入自己的 API Key
API Key 存储在代码中（如 env 文件）
新页面功能：

文件上传：支持 Excel、Word、TXT、MD
自定义输入框
场景/小分类下拉菜单（从数据库读取）
AI 返回结果显示
一键导出为 MD 文件
多轮对话（如果简单就实现）
异步调用 + 取消功能
模型可选择
技术栈：

JavaFX 21（用户说的是 26，但 pom.xml 里是 21）
JDK 21
SQLite 数据库
千问大模型 API（DashScope OpenAI 兼容接口）
文件处理：

Excel：指定 sheet，可引入 Apache POI
Word：纯文本，Apache POI
TXT/MD：直接读取
大文件需要检查大小
UI 布局：由我设计

实现计划：

数据库：

新增 ai_scene 表（场景分类）
新增 ai_scene_sub 表（小分类）
初始化默认数据
配置：

创建 .env 文件或常量类存储预设 API Key 和账号密码
UI：

主页新增按钮"AI 助手"
登录对话框 FXML
AI 助手主页面 FXML
Controller：

登录对话框 Controller
AI 助手主页面 Controller
服务层：

文件解析服务（Excel、Word、TXT、MD）
AI API 调用服务（DashScope）
依赖：

Apache POI（处理 Excel、Word）
OkHttp 或 HttpClient（调用 API）
让我开始实现：

步骤 1：更新 pom.xml 添加依赖

Apache POI（处理 Excel、Word）
OkHttp（HTTP 客户端）
JSON 处理（可以用 Jackson 或 Gson，或者用 Java 自带的）
步骤 2：创建配置文件

创建 AppConfig.java 存储预设的账号密码和 API Key
步骤 3：更新数据库

在 DBUtil.java 中添加场景表初始化
步骤 4：创建实体类

AiScene.java
AiSceneSub.java
步骤 5：创建 DAO

AiSceneDao.java
步骤 6：创建服务类

FileParserService.java（文件解析）
AiApiService.java（AI API 调用）
步骤 7：创建 UI

更新 HomeView.fxml 添加按钮
创建 AiLoginDialog.fxml 和 Controller
创建 AiAssistantView.fxml 和 Controller
步骤 8：更新 PagePath 和 module-info

让我开始实现。

好的，需求已经明确！让我开始实现。我会按照以下步骤进行：

添加依赖（Apache POI、OkHttp）
创建配置文件（存储预设账号密码和 API Key）
更新数据库（场景分类表）
创建实体类和 DAO
创建文件解析服务
创建 AI API 调用服务
创建登录对话框
创建 AI 助手主页面
更新主页添加按钮


https://reiticia.github.io/lesson073.html#%E9%BC%A0%E6%A0%87%E7%82%B9%E5%87%BB%E4%BA%8B%E4%BB%B6
