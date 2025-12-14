# BiliTV 直播页面 API 分析文档

## 概述

BiliTV 应用的直播页面主要包含两个部分：直播分区页面（LiveAreaScreen）和直播间列表页面（LiveRoomListScreen）。本文档详细分析这两个页面的 API 接口调用方式和数据结构。

## 1. 直播分区页面 API

### 1.1 API 接口

**接口地址**: `https://api.live.bilibili.com/room/v1/Area/getList`

**请求方式**: GET

**请求头**:
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
```

### 1.2 响应数据结构

```json
{
  "code": 0,
  "msg": "success",
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "直播",
      "list": [
        {
          "id": "1",
          "parent_id": "1",
          "old_area_id": "1",
          "name": "娱乐",
          "pic": "图片URL",
          "parent_name": "直播",
          "area_type": 0
        }
      ]
    }
  ]
}
```

### 1.3 数据模型说明

#### LiveAreaResponse
- `code`: 响应状态码，0 表示成功
- `msg`: 响应消息
- `message`: 响应消息（与 msg 相同）
- `data`: 直播分区组数据列表

#### LiveAreaGroup
- `id`: 分区组 ID（主标签 ID）
- `name`: 分区组名称（主标签名称）
- `list`: 该分组下的子分区列表

#### LiveAreaItem
- `id`: 分区 ID
- `parent_id`: 父分区 ID（所属主标签 ID）
- `old_area_id`: 旧分区 ID
- `name`: 分区名称
- `pic`: 分区图标 URL
- `parent_name`: 父分区名称
- `area_type`: 分区类型

### 1.4 UI 实现说明

直播分区页面使用 `CommonTabRow` 组件展示主标签（分区组），使用网格布局展示子分区。用户选择主标签后，会显示该标签下的子分区列表。

## 2. 直播间列表页面 API

### 2.1 API 接口

**接口地址**: `https://api.live.bilibili.com/room/v1/Area/getRoomList`

**请求方式**: GET

**请求参数**:
```
parent_area_id: 父分区 ID
area_id: 分区 ID
sort_type: online (排序方式，online 表示按在线人数排序)
page: 页码
page_size: 每页数量（默认30）
```

**请求头**:
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Cookie: [登录用户的 Cookie]
```

### 2.2 响应数据结构

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "roomid": 123456,
      "uid": 789012,
      "title": "直播间标题",
      "uname": "主播名称",
      "online": 10000,
      "user_cover": "用户封面 URL",
      "system_cover": "系统封面 URL",
      "cover": "直播间封面 URL",
      "face": "主播头像 URL",
      "parent_id": 1,
      "parent_name": "娱乐",
      "area_id": 1,
      "area_name": "娱乐"
    }
  ]
}
```

### 2.3 数据模型说明

#### LiveRoomListResponse
- `code`: 响应状态码，0 表示成功
- `message`: 响应消息
- `data`: 直播间列表数据

#### LiveRoomItem
- `roomid`: 直播间 ID
- `uid`: 主播 UID
- `title`: 直播间标题
- `uname`: 主播名称
- `online`: 在线人数
- `user_cover`: 用户封面 URL
- `system_cover`: 系统封面 URL
- `cover`: 直播间封面 URL
- `face`: 主播头像 URL
- `parent_id`: 父分区 ID
- `parent_name`: 父分区名称
- `area_id`: 分区 ID
- `area_name`: 分区名称

### 2.4 UI 实现说明

直播间列表页面使用网格布局展示直播间卡片，支持分页加载和刷新功能。每个直播间卡片显示封面、标题、主播名称和在线人数。

## 3. 直播流获取 API

### 3.1 API 接口

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

### 3.2 响应数据结构

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

### 3.3 数据模型说明

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

### 3.4 直播流 URL 构建逻辑

1. 优先选择 HTTP-FLV 协议，其次是 HLS
2. 优先选择 fmp4 格式，其次是 ts，最后是 flv
3. 选择第一个可用的编码
4. 组合 URL：`host + base_url + extra`

## 4. 实现细节

### 4.1 状态管理

- 使用 `StateFlow` 管理数据状态
- 支持分页加载和刷新
- 保存滚动位置和焦点状态，用于页面切换后恢复

### 4.2 错误处理

- 网络请求异常处理
- API 响应错误码检查
- 数据解析异常处理

### 4.3 性能优化

- 使用协程进行异步网络请求
- 图片加载使用 Coil 库
- 网格布局使用 LazyVerticalGrid 实现虚拟化

## 5. 注意事项

1. 直播流获取需要登录用户的 Cookie
2. 直播状态检查：只有 `live_status` 为 1 时才能获取到直播流
3. 直播流 URL 有时效性，需要实时获取
4. 分页加载时注意避免重复请求
5. 焦点管理对 TV 应用非常重要，需要妥善处理焦点切换和恢复