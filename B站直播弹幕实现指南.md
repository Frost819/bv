# B站直播弹幕实现指南

## 概述

本文档详细介绍了如何实现B站直播弹幕功能，包括获取弹幕服务器连接信息、建立WebSocket连接、处理认证和心跳、接收和解析弹幕数据，以及最终显示弹幕的完整流程。

## 1. 获取弹幕服务器连接信息

### 1.1 获取WBI密钥

首先需要获取WBI签名所需的密钥：

**接口**：
```
GET https://api.bilibili.com/x/web-interface/nav
```

**响应示例**：
```json
{
  "code": 0,
  "data": {
    "wbi_img": {
      "img_url": "https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png",
      "sub_url": "https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01aa08b70aca2.png"
    }
  }
}
```

从响应中提取imgKey和subKey：
```javascript
const imgKey = img_url.substringAfterLast("/").substringBefore(".");
const subKey = sub_url.substringAfterLast("/").substringBefore(".");
```

### 1.2 WBI签名算法

WBI签名算法实现：

```javascript
// 混淆密钥表
const MIXIN_KEY_ENC_TAB = [
  46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
  33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
  61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
  36, 20, 34, 44, 52
];

// 获取混淆密钥
function getMixinKey(imgKey, subKey) {
  const s = imgKey + subKey;
  let result = '';
  for (let i = 0; i < 32; i++) {
    if (i < MIXIN_KEY_ENC_TAB.length && MIXIN_KEY_ENC_TAB[i] < s.length) {
      result += s[MIXIN_KEY_ENC_TAB[i]];
    }
  }
  return result;
}

// 生成签名
function signParams(params, imgKey, subKey) {
  const mixinKey = getMixinKey(imgKey, subKey);
  const currTime = Math.floor(Date.now() / 1000);
  
  // 添加时间戳
  const newParams = {...params, wts: currTime.toString()};
  
  // 按key排序并构建查询字符串
  const sortedKeys = Object.keys(newParams).sort();
  let queryBuilder = '';
  
  for (const key of sortedKeys) {
    if (queryBuilder.length > 0) {
      queryBuilder += '&';
    }
    queryBuilder += encodeURIComponent(key) + '=' + encodeURIComponent(newParams[key]);
  }
  
  // 计算w_rid
  const strToHash = queryBuilder + mixinKey;
  const wRid = md5(strToHash);
  
  return {...newParams, w_rid: wRid};
}

// MD5哈希函数
function md5(str) {
  // 实现MD5哈希算法
  // 可以使用现有的MD5库
}
```

### 1.3 获取弹幕服务器信息

**接口**：
```
GET https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo
```

**请求头**：
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36
```

**请求参数**：
- `id`: 直播间ID
- `type`: 0（固定值）
- `wts`: 当前时间戳（秒）
- `w_rid`: WBI签名值

**响应示例**：
```json
{
  "code": 0,
  "message": "0",
  "data": {
    "group": "live",
    "business_id": 0,
    "refresh_row_factor": 1.0,
    "refresh_rate": 100,
    "max_delay": 5000,
    "token": "xxxxx",
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

## 2. WebSocket连接和认证

### 2.1 建立连接

使用获取到的host_list中的第一个host建立WebSocket连接：

```javascript
const hostItem = data.host_list[0];
const url = `wss://${hostItem.host}:${hostItem.wss_port}/sub`;
const ws = new WebSocket(url);
```

### 2.2 认证流程

连接成功后，需要发送认证包：

```javascript
function sendAuth(ws, roomId, token) {
  const authParams = {
    uid: 0,
    roomid: roomId,
    protover: 3,
    platform: "web",
    type: 2,
    key: token
  };
  
  const body = JSON.stringify(authParams);
  const bodyBytes = new TextEncoder().encode(body);
  
  // 构建包头
  const header = new ArrayBuffer(16);
  const headerView = new DataView(header);
  
  // 总长度
  headerView.setInt32(0, 16 + bodyBytes.length, false); // 大端序
  // 头部长度
  headerView.setInt16(4, 16, false);
  // 协议版本
  headerView.setInt16(6, 1, false);
  // 操作码：7表示认证
  headerView.setInt32(8, 7, false);
  // 序列号
  headerView.setInt32(12, 1, false);
  
  // 合并包头和包体
  const packet = new Uint8Array(16 + bodyBytes.length);
  packet.set(new Uint8Array(header), 0);
  packet.set(bodyBytes, 16);
  
  ws.send(packet);
}
```

### 2.3 心跳机制

连接建立后，需要定期发送心跳包保持连接：

```javascript
function sendHeartbeat(ws) {
  // 构建心跳包头
  const header = new ArrayBuffer(16);
  const headerView = new DataView(header);
  
  // 总长度
  headerView.setInt32(0, 16, false);
  // 头部长度
  headerView.setInt16(4, 16, false);
  // 协议版本
  headerView.setInt16(6, 1, false);
  // 操作码：2表示心跳
  headerView.setInt32(8, 2, false);
  // 序列号
  headerView.setInt32(12, 1, false);
  
  ws.send(header);
}

// 每30秒发送一次心跳
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    sendHeartbeat(ws);
  }
}, 30000);
```

## 3. 弹幕数据接收和解析

### 3.1 数据包格式

接收到的数据包可能使用不同的压缩方式：
- 协议版本0：明文JSON
- 协议版本2：Zlib压缩
- 协议版本3：Brotli压缩

### 3.2 数据包解析

```javascript
function handleMessage(bytes) {
  const buffer = new DataView(bytes.buffer);
  const totalLen = buffer.getUint32(0, false); // 大端序
  const headerLen = buffer.getUint16(4, false);
  const protoVer = buffer.getUint16(6, false);
  const opcode = buffer.getUint32(8, false);
  const seq = buffer.getUint32(12, false);
  
  if (bytes.length < totalLen) return; // 不完整的数据包
  
  const bodyLen = totalLen - headerLen;
  const body = bytes.slice(headerLen, headerLen + bodyLen);
  
  switch (protoVer) {
    case 0: // 明文JSON
      handlePacketBody(opcode, body);
      break;
    case 2: // Zlib压缩
      decompressZlib(body).then(decompressed => {
        handleStream(decompressed);
      });
      break;
    case 3: // Brotli压缩
      decompressBrotli(body).then(decompressed => {
        handleStream(decompressed);
      });
      break;
  }
}

function handleStream(bytes) {
  let offset = 0;
  while (offset < bytes.length) {
    if (bytes.length - offset < 16) break;
    
    const buffer = new DataView(bytes.buffer, offset, bytes.length - offset);
    const len = buffer.getUint32(0, false);
    const headerLen = buffer.getUint16(4, false);
    const ver = buffer.getUint16(6, false);
    const op = buffer.getUint32(8, false);
    const seq = buffer.getUint32(12, false);
    
    const bodyLen = len - headerLen;
    const body = bytes.slice(offset + headerLen, offset + headerLen + bodyLen);
    
    handlePacketBody(op, body);
    
    offset += len;
  }
}

function handlePacketBody(opcode, body) {
  if (opcode === 5) { // 命令消息
    try {
      const jsonStr = new TextDecoder().decode(body);
      const json = JSON.parse(jsonStr);
      const cmd = json.cmd;
      
      if (cmd.startsWith('DANMU_MSG')) {
        const info = json.info;
        const text = info[1];
        const extra = info[0];
        const color = extra[3];
        const userArr = info[2];
        const userName = userArr[1];
        const timestamp = extra[4];
        
        // 处理弹幕数据
        processDanmaku({
          time: timestamp,
          text: text,
          color: color,
          userName: userName
        });
      }
    } catch (e) {
      console.error('解析弹幕消息失败', e);
    }
  }
}
```

### 3.3 解压缩函数

```javascript
// Zlib解压缩
async function decompressZlib(compressed) {
  // 使用pako等库进行解压缩
  // const pako = require('pako');
  // return pako.inflate(compressed);
}

// Brotli解压缩
async function decompressBrotli(compressed) {
  // 使用brotli-decode等库进行解压缩
  // const brotli = require('brotli-decode');
  // return brotli.decompress(compressed);
}
```

## 4. 弹幕显示

### 4.1 弹幕数据模型

```javascript
class LiveDanmakuItem {
  constructor(time, text, color, userName) {
    this.time = time;
    this.text = text;
    this.color = color;
    this.userName = userName;
  }
}
```

### 4.2 弹幕渲染

弹幕渲染可以使用现成的弹幕库，如Danmaku2.js、DPlayer等，也可以自己实现：

```javascript
function addDanmakuToDisplay(danmakuItem) {
  // 创建弹幕元素
  const danmakuElement = document.createElement('div');
  danmakuElement.className = 'danmaku';
  danmakuElement.textContent = `${danmakuItem.userName}: ${danmakuItem.text}`;
  danmakuElement.style.color = `#${danmakuItem.color.toString(16).padStart(6, '0')}`;
  
  // 设置弹幕位置和动画
  danmakuElement.style.position = 'absolute';
  danmakuElement.style.top = `${Math.random() * 80}%`;
  danmakuElement.style.whiteSpace = 'nowrap';
  danmakuElement.style.animation = 'scroll-left 10s linear';
  
  // 添加到显示区域
  const danmakuContainer = document.getElementById('danmaku-container');
  danmakuContainer.appendChild(danmakuElement);
  
  // 动画结束后移除元素
  danmakuElement.addEventListener('animationend', () => {
    danmakuContainer.removeChild(danmakuElement);
  });
}
```

### 4.3 CSS样式

```css
#danmaku-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}

.danmaku {
  font-size: 25px;
  font-weight: bold;
  text-shadow: 1px 1px 1px #333;
  padding: 2px 5px;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.3);
}

@keyframes scroll-left {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(-100%);
  }
}
```

## 5. 完整实现示例

```javascript
class BilibiliLiveDanmaku {
  constructor(roomId, container) {
    this.roomId = roomId;
    this.container = container;
    this.ws = null;
    this.heartbeatInterval = null;
    this.retryCount = 0;
    this.maxRetryCount = 5;
  }
  
  async start() {
    try {
      // 1. 获取WBI密钥
      const wbiKeys = await this.getWbiKeys();
      
      // 2. 获取弹幕服务器信息
      const danmuInfo = await this.getDanmuInfo(wbiKeys.imgKey, wbiKeys.subKey);
      
      if (!danmuInfo) {
        throw new Error('获取弹幕服务器信息失败');
      }
      
      // 3. 建立WebSocket连接
      await this.connect(danmuInfo);
    } catch (error) {
      console.error('启动弹幕失败', error);
      this.reconnect();
    }
  }
  
  async getWbiKeys() {
    const response = await fetch('https://api.bilibili.com/x/web-interface/nav');
    const data = await response.json();
    
    const wbiImg = data.data.wbi_img;
    const imgKey = wbiImg.img_url.split('/').pop().split('.')[0];
    const subKey = wbiImg.sub_url.split('/').pop().split('.')[0];
    
    return { imgKey, subKey };
  }
  
  async getDanmuInfo(imgKey, subKey) {
    const params = {
      id: this.roomId.toString(),
      type: '0'
    };
    
    const signedParams = this.signParams(params, imgKey, subKey);
    
    const queryString = Object.entries(signedParams)
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
      .join('&');
    
    const url = `https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?${queryString}`;
    
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
      }
    });
    
    const data = await response.json();
    return data.code === 0 ? data.data : null;
  }
  
  signParams(params, imgKey, subKey) {
    const mixinKey = this.getMixinKey(imgKey, subKey);
    const currTime = Math.floor(Date.now() / 1000);
    
    const newParams = {...params, wts: currTime.toString()};
    
    const sortedKeys = Object.keys(newParams).sort();
    let queryBuilder = '';
    
    for (const key of sortedKeys) {
      if (queryBuilder.length > 0) {
        queryBuilder += '&';
      }
      queryBuilder += `${encodeURIComponent(key)}=${encodeURIComponent(newParams[key])}`;
    }
    
    const strToHash = queryBuilder + mixinKey;
    const wRid = this.md5(strToHash);
    
    return {...newParams, w_rid: wRid};
  }
  
  getMixinKey(imgKey, subKey) {
    const MIXIN_KEY_ENC_TAB = [
      46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
      33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
      61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
      36, 20, 34, 44, 52
    ];
    
    const s = imgKey + subKey;
    let result = '';
    for (let i = 0; i < 32; i++) {
      if (i < MIXIN_KEY_ENC_TAB.length && MIXIN_KEY_ENC_TAB[i] < s.length) {
        result += s[MIXIN_KEY_ENC_TAB[i]];
      }
    }
    return result;
  }
  
  md5(str) {
    // 使用MD5库实现
    // 例如：return CryptoJS.MD5(str).toString();
  }
  
  async connect(danmuInfo) {
    const hostItem = danmuInfo.host_list[0];
    const url = `wss://${hostItem.host}:${hostItem.wss_port}/sub`;
    
    this.ws = new WebSocket(url);
    
    this.ws.onopen = () => {
      console.log('WebSocket连接成功');
      this.retryCount = 0;
      this.sendAuth(danmuInfo.token);
      this.startHeartbeat();
    };
    
    this.ws.onmessage = (event) => {
      if (event.data instanceof Blob) {
        event.data.arrayBuffer().then(buffer => {
          this.handleMessage(new Uint8Array(buffer));
        });
      }
    };
    
    this.ws.onclose = () => {
      console.log('WebSocket连接关闭');
      this.stopHeartbeat();
      this.reconnect();
    };
    
    this.ws.onerror = (error) => {
      console.error('WebSocket错误', error);
      this.reconnect();
    };
  }
  
  sendAuth(token) {
    const authParams = {
      uid: 0,
      roomid: this.roomId,
      protover: 3,
      platform: 'web',
      type: 2,
      key: token
    };
    
    const body = JSON.stringify(authParams);
    const bodyBytes = new TextEncoder().encode(body);
    
    const header = new ArrayBuffer(16);
    const headerView = new DataView(header);
    
    headerView.setInt32(0, 16 + bodyBytes.length, false);
    headerView.setInt16(4, 16, false);
    headerView.setInt16(6, 1, false);
    headerView.setInt32(8, 7, false);
    headerView.setInt32(12, 1, false);
    
    const packet = new Uint8Array(16 + bodyBytes.length);
    packet.set(new Uint8Array(header), 0);
    packet.set(bodyBytes, 16);
    
    this.ws.send(packet);
  }
  
  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        const header = new ArrayBuffer(16);
        const headerView = new DataView(header);
        
        headerView.setInt32(0, 16, false);
        headerView.setInt16(4, 16, false);
        headerView.setInt16(6, 1, false);
        headerView.setInt32(8, 2, false);
        headerView.setInt32(12, 1, false);
        
        this.ws.send(header);
      }
    }, 30000);
  }
  
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }
  
  reconnect() {
    if (this.retryCount >= this.maxRetryCount) {
      console.error('重连次数已达上限');
      return;
    }
    
    this.retryCount++;
    console.log(`尝试重连... (${this.retryCount}/${this.maxRetryCount})`);
    
    setTimeout(() => {
      this.start();
    }, 3000);
  }
  
  handleMessage(bytes) {
    const buffer = new DataView(bytes.buffer);
    const totalLen = buffer.getUint32(0, false);
    const headerLen = buffer.getUint16(4, false);
    const protoVer = buffer.getUint16(6, false);
    const opcode = buffer.getUint32(8, false);
    
    if (bytes.length < totalLen) return;
    
    const bodyLen = totalLen - headerLen;
    const body = bytes.slice(headerLen, headerLen + bodyLen);
    
    switch (protoVer) {
      case 0:
        this.handlePacketBody(opcode, body);
        break;
      case 2:
        // Zlib解压缩
        break;
      case 3:
        // Brotli解压缩
        break;
    }
  }
  
  handlePacketBody(opcode, body) {
    if (opcode === 5) {
      try {
        const jsonStr = new TextDecoder().decode(body);
        const json = JSON.parse(jsonStr);
        const cmd = json.cmd;
        
        if (cmd.startsWith('DANMU_MSG')) {
          const info = json.info;
          const text = info[1];
          const extra = info[0];
          const color = extra[3];
          const userArr = info[2];
          const userName = userArr[1];
          const timestamp = extra[4];
          
          this.addDanmaku({
            time: timestamp,
            text: text,
            color: color,
            userName: userName
          });
        }
      } catch (e) {
        console.error('解析弹幕消息失败', e);
      }
    }
  }
  
  addDanmaku(danmakuItem) {
    const danmakuElement = document.createElement('div');
    danmakuElement.className = 'danmaku';
    danmakuElement.textContent = `${danmakuItem.userName}: ${danmakuItem.text}`;
    danmakuElement.style.color = `#${danmakuItem.color.toString(16).padStart(6, '0')}`;
    danmakuElement.style.position = 'absolute';
    danmakuElement.style.top = `${Math.random() * 80}%`;
    danmakuElement.style.whiteSpace = 'nowrap';
    danmakuElement.style.animation = 'scroll-left 10s linear';
    
    this.container.appendChild(danmakuElement);
    
    danmakuElement.addEventListener('animationend', () => {
      this.container.removeChild(danmakuElement);
    });
  }
  
  stop() {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

// 使用示例
// const container = document.getElementById('danmaku-container');
// const danmaku = new BilibiliLiveDanmaku(123456, container);
// danmaku.start();
```

## 6. 注意事项

1. **WBI签名**：WBI签名算法可能会更新，需要关注B站的最新变化
2. **协议版本**：目前主要使用协议版本3（Brotli压缩），但也需要兼容版本0和版本2
3. **错误处理**：网络请求和WebSocket连接可能会失败，需要实现重试机制
4. **性能优化**：大量弹幕可能会影响性能，需要实现弹幕池管理和显示限制
5. **跨域问题**：在浏览器环境中可能需要处理跨域问题

## 7. 依赖库推荐

1. **MD5哈希**：CryptoJS、spark-md5等
2. **解压缩**：pako（Zlib）、brotli-decode（Brotli）等
3. **弹幕渲染**：Danmaku2.js、DPlayer等

通过以上文档，您应该能够在任何项目中实现B站直播弹幕功能。根据您的具体技术栈，可能需要调整部分实现细节。