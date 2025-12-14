# BiliTV 直播播放与弹幕 API 分析文档

## 概述

本文档详细分析 BiliTV 应用中点击直播间卡片进入直播播放的逻辑和获取弹幕的实现方式，包括相关的 API 接口调用和数据结构。

## 1. 直播播放流程

### 1.1 流程概述

1. 用户在直播间列表页面点击直播间卡片
2. 调用 `LiveRoomListViewModel.enterLiveRoom()` 方法
3. 获取直播间播放信息（包括流地址）
4. 设置 `fullScreenLivePlayInfo` 状态变量
5. 触发页面切换，进入全屏播放模式
6. 启动 `VideoPlayerScreen` 组件播放直播流
7. 初始化弹幕管理器并连接弹幕服务器

### 1.2 进入直播间的代码逻辑

```kotlin
// LiveRoomListScreen.kt
fun enterLiveRoom(roomId: Int, title: String, uname: String, onEnterLiveRoom: (LivePlayInfo, String) -> Unit) {
    viewModelScope.launch {
        val livePlayInfo = LiveStreamUrlFetcher.fetchLivePlayInfo(roomId, title, uname)
        if (livePlayInfo != null) {
            shouldRestoreFocusToGrid = true
            onEnterLiveRoom(livePlayInfo, title)
        }
    }
}
```

### 1.3 页面切换逻辑

```kotlin
// MainScreen.kt
onEnterLiveRoom = { livePlayInfo, title ->
    isFullScreenPlayer = true
    fullScreenLivePlayInfo = livePlayInfo
    fullScreenVideoTitle = title
}
```

## 2. 直播流获取 API

### 2.1 API 接口

**接口地址**: `https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo`

**请求方式**: GET

**请求参数**:
```
room_id: 直播间 ID
qn: 10000 (画质，10000 表示最高画质)
platform: web
protocol: 0,1 (协议，0:HTTP-FLV, 1:HLS)
format: 0,1,2 (格式，0:flv, 1:ts, 2:fmp4)
codec: 0,1 (编码，0:avc, 1:hevc)
dolby: 5
panorama: 1
```

**请求头**:
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Referer: https://live.bilibili.com
Cookie: [登录用户的 Cookie]
```

### 2.2 响应数据结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "room_id": 123456,
    "live_status": 1,
    "playurl_info": {
      "playurl": {
        "stream": [
          {
            "protocol_name": "http_stream",
            "format": [
              {
                "format_name": "fmp4",
                "codec": [
                  {
                    "codec_name": "avc",
                    "current_qn": 10000,
                    "base_url": "基础URL",
                    "url_info": [
                      {
                        "host": "主机地址",
                        "extra": "额外参数"
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      }
    }
  }
}
```

### 2.3 数据模型说明

#### LiveStreamResponse
- `code`: 响应状态码，0 表示成功
- `message`: 响应消息
- `data`: 直播流数据

#### LiveStreamData
- `room_id`: 直播间 ID
- `live_status`: 直播状态，1 表示正在直播
- `playurl_info`: 播放 URL 信息

#### LivePlayUrlInfo
- `playurl`: 播放 URL 数据

#### LivePlayUrl
- `stream`: 流信息列表

#### LiveStream
- `protocol_name`: 协议名称（如 http_stream、http_hls）
- `format`: 格式列表

#### LiveFormat
- `format_name`: 格式名称（如 flv、ts、fmp4）
- `codec`: 编码列表

#### LiveCodec
- `codec_name`: 编码名称（如 avc、hevc）
- `current_qn`: 当前画质
- `base_url`: 基础 URL
- `url_info`: URL 信息列表

#### LiveUrlInfo
- `host`: 主机地址
- `extra`: 额外参数

### 2.4 直播流 URL 构建逻辑

1. 优先选择 HTTP-FLV 协议，其次是 HLS
2. 优先选择 fmp4 格式，其次是 ts，最后是 flv
3. 选择第一个可用的编码
4. 组合 URL：`host + base_url + extra`

## 3. 弹幕系统

### 3.1 弹幕系统架构

弹幕系统由以下组件构成：
- `DanmakuLiveManager`: 直播弹幕管理器
- `DanmakuLiveRepository`: 弹幕信息获取仓库
- `LiveDanmakuWebSocketClient`: WebSocket 客户端
- `DanmakuManager`: 通用弹幕管理器

### 3.2 弹幕连接流程

1. 获取弹幕服务器信息（包括 token 和服务器列表）
2. 建立 WebSocket 连接
3. 发送认证信息
4. 启动心跳机制
5. 接收并解析弹幕消息
6. 将弹幕添加到显示界面

### 3.3 弹幕信息获取 API

#### 3.3.1 API 接口

**接口地址**: `https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo`

**请求方式**: GET

**请求参数**:
```
id: 直播间 ID
type: 0 (类型，0 表示直播)
wts: 时间戳
w_rid: WBI 签名
```

**请求头**:
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36
```

#### 3.3.2 WBI 签名获取

在请求弹幕信息前，需要先获取 WBI 签名所需的密钥：

**接口地址**: `https://api.bilibili.com/x/web-interface/nav`

**请求方式**: GET

从响应中提取 `wbi_img.img_url` 和 `wbi_img.sub_url`，获取最后的文件名部分作为 `imgKey` 和 `subKey`。

#### 3.3.3 响应数据结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "group": "live",
    "business_id": 0,
    "refresh_row_factor": 1.0,
    "refresh_rate": 100,
    "max_delay": 5000,
    "token": "认证token",
    "host_list": [
      {
        "host": "broadcastlv.chat.bilibili.com",
        "port": 2243,
        "wss_port": 2245,
        "ws_port": 2244
      }
    ]
  }
}
```

#### 3.3.4 数据模型说明

#### LiveDanmuInfoResponse
- `code`: 响应状态码，0 表示成功
- `message`: 响应消息
- `data`: 弹幕信息数据

#### LiveDanmuInfoData
- `group`: 组名
- `business_id`: 业务 ID
- `refresh_row_factor`: 刷新行因子
- `refresh_rate`: 刷新率
- `max_delay`: 最大延迟
- `token`: 认证令牌
- `host_list`: 服务器列表

#### LiveHostItem
- `host`: 主机地址
- `port`: 端口
- `wss_port`: WebSocket Secure 端口
- `ws_port`: WebSocket 端口

### 3.4 WebSocket 连接

#### 3.4.1 连接建立

```kotlin
// 使用 wss_port 建立 WebSocket 连接
val url = "wss://${hostItem.host}:${hostItem.wss_port}/sub"
val request = Request.Builder().url(url).build()
webSocket = client.newWebSocket(request, webSocketListener)
```

#### 3.4.2 认证消息

连接建立后，需要发送认证消息：

```json
{
  "uid": 0,
  "roomid": 直播间ID,
  "protover": 3,
  "platform": "web",
  "type": 2,
  "key": "token"
}
```

认证消息使用二进制格式发送，包含：
- 总长度（4 字节）
- 头部长度（2 字节）
- 协议版本（2 字节）
- 操作码（4 字节，7 表示认证）
- 序列号（4 字节）
- 消息体（JSON 字符串）

#### 3.4.3 心跳机制

建立连接后，每 30 秒发送一次心跳消息：

- 操作码：2（表示心跳）
- 消息体：空

#### 3.4.4 消息接收与解析

接收到的消息可能是以下几种格式：
- 协议版本 0：纯 JSON
- 协议版本 2：Zlib 压缩
- 协议版本 3：Brotli 压缩

需要根据协议版本进行解压缩，然后解析消息内容。

#### 3.4.5 弹幕消息格式

弹幕消息的操作码为 5，消息体格式如下：

```json
{
  "cmd": "DANMU_MSG",
  "info": [
    [
      弹幕参数数组,
      弹幕文本,
      弹幕时间戳
    ],
    "弹幕文本内容",
    [
      用户信息数组,
      用户ID,
      用户名
    ]
  ]
}
```

从 `info[1]` 获取弹幕文本，从 `info[0][3]` 获取弹幕颜色，从 `info[2][1]` 获取用户名。

### 3.5 弹幕数据模型

```kotlin
data class LiveDanmakuItem(
    val time: Long,      // 弹幕时间戳
    val text: String,    // 弹幕文本
    val color: Int,      // 弹幕颜色
    val userName: String // 用户名
)
```

## 4. 播放器集成

### 4.1 ExoPlayer 集成

直播播放使用 ExoPlayer (media3) 作为播放器核心：

```kotlin
// 创建播放器
val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
        // 设置媒体源
        val mediaItem = MediaItem.fromUri(videoPlayInfo.videoUrl)
        setMediaItem(mediaItem)
        prepare()
        playWhenReady = true
    }
}
```

### 4.2 弹幕显示集成

在 `VideoPlayerScreen` 中集成弹幕显示：

```kotlin
// 初始化弹幕管理器
val danmakuManager = remember { DanmakuManager(context) }
val danmakuLiveManager = remember { DanmakuLiveManager(danmakuManager) }

// 启动弹幕
danmakuLiveManager.start(videoPlayInfo.cid)

// 停止弹幕
danmakuLiveManager.stop()
```

## 5. 实现细节

### 5.1 状态管理

- 使用 `StateFlow` 管理播放状态
- 保存和恢复播放位置
- 处理播放器生命周期

### 5.2 错误处理

- 网络连接失败处理
- WebSocket 连接断开重连机制
- 弹幕解析错误处理

### 5.3 性能优化

- 使用协程处理异步操作
- 弹幕消息使用 Channel 缓冲
- 播放器资源及时释放

## 6. 注意事项

1. 直播流 URL 有时效性，需要实时获取
2. WebSocket 连接可能不稳定，需要实现重连机制
3. 弹幕消息频率可能很高，需要控制显示频率
4. WBI 签名密钥会定期更换，需要动态获取
5. 直播状态检查：只有 `live_status` 为 1 时才能获取到直播流
6. 认证 token 也有时效性，连接断开重连时需要重新获取