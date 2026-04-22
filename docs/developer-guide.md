# TVBox 开发者文档

这份文档面向需要维护、排障或二次开发 TVBox 的开发者，重点描述当前仓库的构建方式、模块关系、关键运行链路，以及修改时最容易产生联动影响的区域。

## 模块结构

这是一个多模块 Android 工程，模块关系定义在 `settings.gradle`：
- `:app`：主应用模块，绝大多数业务逻辑都在这里。
- `:player`：播放器依赖与基础播放器能力封装。
- `:quickjs`：QuickJS/native 相关集成。
- `:pyramid`：Chaquopy/Python 相关支持模块。

其中：
- `:app` 依赖 `:player` 和 `:quickjs`
- `python` flavor 下会额外接入 `:pyramid`

## 构建与常用命令

### 构建
- 构建 normal debug：`./gradlew :app:assembleNormalDebug`
- 构建 normal release：`./gradlew :app:assembleNormalRelease`
- 构建 python debug：`./gradlew :app:assemblePythonDebug`
- 安装 normal debug：`./gradlew :app:installNormalDebug`
- 清理：`./gradlew clean`

### 检查
- Lint：`./gradlew :app:lintNormalDebug`
- 运行测试：`./gradlew test`
- 如果后续补充单测，可运行单个类：`./gradlew :app:testNormalDebugUnitTest --tests 'com.example.YourTest'`
- 如果后续补充仪器测试，可运行：`./gradlew :app:connectedNormalDebugAndroidTest`

说明：
- 仓库当前没有 `app/src/test`、`app/src/androidTest`，也没有显式测试依赖，因此测试章节主要是为后续维护保留。
- 当前 Gradle 配置仅构建 `armeabi-v7a`。
- `python` flavor 的 `minSdkVersion` 高于普通版本。
- `pyramid/build.gradle` 中硬编码了一个 Windows Python 路径；如果 Python 版本在其他机器上构建失败，优先检查这里。

## 关键运行链路

### 启动链路
应用启动入口在：
- `app/src/main/java/com/github/tvbox/osc/base/App.java`

启动时主要初始化：
- Hawk
- OkGo / 网络辅助能力
- EPG 工具
- 本地控制服务上下文
- Room 数据访问
- 播放器辅助能力
- QuickJS

首页入口在：
- `app/src/main/java/com/github/tvbox/osc/ui/activity/HomeActivity.java`

首页启动后会：
1. 启动本地服务
2. 加载配置
3. 按需加载 spider jar
4. 再驱动首页分类和数据展示

### 配置与 source 编排
配置中心在：
- `app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java`

它负责：
- 拉取和解析配置 JSON
- 维护 source、parse、live、wallpaper、spider 等运行状态
- 处理缓存文件
- 管理 jar/js/python loader
- 处理部分 proxy / 本地代理相关调用

主要视图编排在：
- `app/src/main/java/com/github/tvbox/osc/viewmodel/SourceViewModel.java`

它统一封装多种 source 类型的访问差异。当前代码里重点类型包括：
- `0`：XML API source
- `1`：JSON API source
- `3`：JAR spider source
- `4`：remote source API

很多“列表没问题但详情有问题”“搜索能出结果但播放异常”这类问题，根源不在页面，而在 source 类型差异、配置内容或 loader 状态。

## 动态扩展架构

项目的动态 crawler/plugin 能力主要在：
- `app/src/main/java/com/github/catvod/crawler/JarLoader.java`
- `app/src/main/java/com/github/catvod/crawler/JsLoader.java`
- `app/src/python/java/com/undcover/freedom/pyramid/PythonLoader.java`

### JarLoader
负责：
- 下载和缓存外部 jar
- 创建 `DexClassLoader`
- 调用 `com.github.catvod.spider.Init.init(Context)`
- 动态实例化 spider
- 管理 proxy 方法缓存

### JsLoader
负责：
- 为 JS spider 提供基于 jar 的 JS API 支持
- 配合 QuickJS 做动态能力扩展

### PythonLoader
负责：
- 启动 Python 运行环境
- 按配置实例化 PythonSpider
- 通过本地 proxy 端口与应用其他能力协同

结论：
- jar / js / python 三条链路不是互相独立的“可选附件”，而是整个扩展能力的核心。
- 修改缓存策略、初始化流程、proxy 行为时，要同时考虑首页、搜索、详情、播放是否会被连带影响。

## UI 结构

项目主界面基本采用：
- Activity + Fragment + ViewModel

主要页面包括：
- `HomeActivity`：首页与站点/分类入口
- `GridFragment`：列表、目录、筛选、分页
- `DetailActivity`：详情、线路、选集、收藏
- `SearchActivity` / `FastSearchActivity`：搜索与聚合搜索
- `PlayActivity`：点播播放
- `LivePlayActivity`：直播播放
- `SettingActivity` / `ModelSettingFragment`：设置入口与设置项

这个项目是 TV-first 设计，很多页面天然依赖：
- D-pad 焦点移动
- TvRecyclerView
- 遥控器键盘事件

因此做交互修改时，不要默认用手机触屏逻辑去理解页面行为。

## 播放架构

播放器相关能力分布在：
- `:player` 模块
- `app/src/main/java/com/github/tvbox/osc/player/`
- `app/src/main/java/com/github/tvbox/osc/player/controller/`

点播主入口：
- `app/src/main/java/com/github/tvbox/osc/ui/activity/PlayActivity.java`

直播主入口：
- `app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java`

播放器包装类：
- `app/src/main/java/com/github/tvbox/osc/player/MyVideoView.java`

此外，字幕解析和渲染不是简单依赖外部库，而是在应用内实现了一整套能力，代码位于：
- `app/src/main/java/com/github/tvbox/osc/subtitle/`

## 本地 HTTP 服务与远控

本地服务相关代码在：
- `app/src/main/java/com/github/tvbox/osc/server/ControlManager.java`
- `app/src/main/java/com/github/tvbox/osc/server/RemoteServer.java`

服务基于 NanoHTTPD，承担的职责包括：
- `/proxy`
- `/push/...`
- `/dns-query`
- `/upload`
- 静态页面与脚本资源
- 某些文件读写与局域网控制能力

因此：
- 推送问题、局域网页面异常、部分播放代理问题，往往都需要同时看 `RemoteServer`、`ControlManager`、`ApiConfig`、播放器启动链路。

## 持久化与缓存

项目当前的持久化分两层：

### 1. Hawk
主要存：
- 用户设置
- 当前接口地址
- DNS 选项
- 播放器配置
- 首页/直播相关偏好

关键配置项集中在：
- `app/src/main/java/com/github/tvbox/osc/util/HawkConfig.java`

### 2. Room
主要存：
- 历史、收藏、缓存类结构化数据

主要代码在：
- `app/src/main/java/com/github/tvbox/osc/data/`
- `app/src/main/java/com/github/tvbox/osc/cache/`
- `app/src/main/java/com/github/tvbox/osc/data/AppDataManager.java`

另外，配置 URL、远程配置文件、spider jar 等也会落在应用私有目录缓存中。

排查配置问题时，不要只看当前内存状态；很多问题来自：
- Hawk 中的旧值
- 私有目录中的旧配置缓存
- jar 缓存未刷新

## 代码维护注意点

- 这是 TV-first 项目，焦点移动、遥控器行为、列表选中态都很重要。
- 本地 server、播放链路、proxy、动态 spider 高度耦合，改一处很可能影响另一处。
- `python` flavor 的构建依赖额外环境，不能把它当成和 normal flavor 完全等价的构建路径。
- README 中的配置 schema 不是随便写的示例，而是与 `ApiConfig` 解析逻辑有实际对应关系。
