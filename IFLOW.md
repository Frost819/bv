# BV 项目概述

BV 是一个哔哩哔哩的第三方 Android TV 应用，使用 Jetpack Compose 开发，支持 Android 5.0+ 系统。

## 技术架构

### 开发语言与框架
- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构模式**: MVVM + Repository
- **依赖注入**: Koin
- **网络请求**: Ktor
- **数据库**: Room
- **视频播放**: ExoPlayer (Media3)

### 项目结构
```
C:\Users\59925\Desktop\bv\
├── app/                    # 主应用模块
├── bili-api/              # 哔哩哔哩 API 接口模块
├── bili-api-grpc/         # gRPC API 模块
├── bili-subtitle/         # 字幕处理模块
├── bv-player/             # 视频播放器模块
├── libs/                  # 本地库依赖
│   ├── av1Decoder/        # AV1 解码器
│   ├── ffmpegDecoder/     # FFmpeg 解码器
│   ├── libVLC/            # VLC 播放器
│   └── media3Container/   # Media3 容器
└── buildSrc/              # 构建配置
```

## 构建与运行

### 环境要求
- Android SDK 36
- Java 17
- Kotlin 2.1.21
- Gradle 8.8.0

### 构建命令
```bash
# 构建调试版本
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 构建 Lite 版本
./gradlew assembleLiteDebug

# 运行测试
./gradlew test
```

### 版本管理
- 版本号格式: `主版本.次版本.补丁.热修复.r构建号.git提交哈希`
- 构建号自动从 git 提交历史生成
- 当前版本: 0.3.10

## 主要功能

### 视频播放
- 支持多种视频格式和编码
- 弹幕显示与设置
- 播放速度控制
- 断点续播
- 分P和合集播放

### 用户系统
- 登录/登出
- 用户信息管理
- 多用户切换
- 收藏、历史、稍后再看

### 界面优化
- 左侧导航栏重做
- 列表滚动性能优化
- 视频卡片信息展示
- 设置页面重构

## 开发规范

### 代码风格
- 使用 Kotlin 官方代码风格
- 遵循 Compose 最佳实践
- 使用协程处理异步操作

### 依赖管理
- 使用 Version Catalogs 管理依赖版本
- 主要依赖版本见 `gradle/libs.versions.toml`

### 模块设计
- 按功能模块划分代码
- 使用 Repository 模式管理数据
- 通过 Koin 进行依赖注入

## 特殊配置

### 签名配置
- 调试版本使用 debug keystore
- 发布版本需要配置 `signing.properties`

### 构建变体
- `default`: 完整功能版本
- `lite`: 精简版本(移除 VLC)
- `debug`: 调试版本
- `r8Test`: R8 测试版本
- `alpha`: Alpha 测试版本

### 代理功能
- 支持 HTTP 和 gRPC 代理
- 可在设置中配置代理服务器

## 注意事项

1. 项目不支持在中国大陆地区内使用
2. 需要配置 `google-services.json` 才能使用 Firebase 功能
3. 视频播放功能依赖网络环境，部分地区可能无法正常使用
4. 项目使用了多个本地库，确保这些库文件正确配置

## API 文档

详细的 API 分析文档请参考 `直播页面API分析文档.md`，包含:
- 直播分区页面 API
- 直播间列表页面 API
- 直播流获取 API
- 数据模型说明
- 实现细节