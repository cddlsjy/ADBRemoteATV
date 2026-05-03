# 技术设计文档 - IP扫描功能

## 文档信息
- 创建日期: 2026-05-03
- 项目名称: ADBRemoteATV
- 功能名称: IP扫描功能
- 版本: 1.0
- 技术栈: Java, Android SDK, Socket

## 架构概览

本功能在现有ADB遥控器应用基础上，增加局域网IP扫描能力，帮助用户快速发现并连接ADB设备。

### 技术架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     ConnectOperateDialog                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  IP输入框     │  │  扫描按钮     │  │  端口输入框       │  │
│  └──────────────┘  └──────┬───────┘  └──────────────────┘  │
│                            │                                  │
│                            ▼                                  │
│                  ┌─────────────────────┐                     │
│                  │  DeviceScanDialog   │                     │
│                  │  (设备扫描结果弹窗)   │                     │
│                  └──────────┬──────────┘                     │
│                             │                                 │
│                             ▼                                 │
│                  ┌─────────────────────┐                     │
│                  │   IpScanService     │                     │
│                  │   (IP扫描服务)       │                     │
│                  └──────────┬──────────┘                     │
│                             │                                 │
│                             ▼                                 │
│                  ┌─────────────────────┐                     │
│                  │  NetworkScanner     │                     │
│                  │  (网络扫描核心)       │                     │
│                  └──────────┬──────────┘                     │
│                             │                                 │
│                ┌────────────┴────────────┐                   │
│                ▼                         ▼                   │
│      ┌──────────────────┐      ┌──────────────────┐          │
│      │  ThreadPool      │      │  Socket          │          │
│      │  (线程池管理)     │      │  (端口检测)       │          │
│      └──────────────────┘      └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## 组件设计

### 1. IpScanService（新增）
**包路径**: `com.swx.adbremote.service.IpScanService`

**职责**:
- 管理IP扫描任务的启动、停止和取消
- 协调网络扫描和结果收集
- 提供扫描进度和结果回调接口

**核心方法**:
```java
public interface IpScanService {
    void startScan(IpScanCallback callback);
    void stopScan();
    boolean isScanning();
}

public interface IpScanCallback {
    void onScanStart();
    void onDeviceFound(DeviceInfo device);
    void onScanProgress(int current, int total);
    void onScanComplete(List<DeviceInfo> devices);
    void onScanError(String error);
    void onScanCancelled();
}

public class DeviceInfo {
    private String ipAddress;
    private int port;
    private String deviceName;  // 可选，通过adb shell获取
    private long responseTime;  // 响应时间
}
```

### 2. NetworkScanner（新增）
**包路径**: `com.swx.adbremote.utils.NetworkScanner`

**职责**:
- 执行实际的IP扫描逻辑
- 使用线程池并发检测端口
- 获取当前网络信息

**核心方法**:
```java
public class NetworkScanner {
    private static final int DEFAULT_PORT = 5555;
    private static final int THREAD_POOL_SIZE = 15;
    private static final int SCAN_TIMEOUT_MS = 30000;

    public List<DeviceInfo> scanNetwork(Context context) throws Exception;
    private String getLocalSubnet(Context context) throws Exception;
    private boolean isPortOpen(String ip, int port, int timeout);
    private String getDeviceName(String ip, int port);  // 可选实现
}
```

### 3. DeviceScanDialog（新增）
**包路径**: `com.swx.adbremote.components.DeviceScanDialog`

**职责**:
- 显示扫描结果列表
- 提供设备选择功能
- 显示扫描进度

**UI布局**:
```
┌─────────────────────────────────────┐
│  发现设备           [X]               │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │  IP: 192.168.1.100            │  │
│  │  状态: 已连接                 │  │
│  │  [连接]                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  IP: 192.168.1.101            │  │
│  │  状态: 可连接                 │  │
│  │  [连接]                       │  │
│  └───────────────────────────────┘  │
│  ┌───────────────────────────────┐  │
│  │  IP: 192.168.1.102            │  │
│  │  状态: 可连接                 │  │
│  │  [连接]                       │  │
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│  [取消]                            │
└─────────────────────────────────────┘
```

### 4. 修改 ConnectOperateDialog（现有）
**包路径**: `com.swx.adbremote.components.ConnectOperateDialog`

**修改内容**:
- 在IP输入框右侧添加扫描按钮
- 添加扫描按钮点击事件处理
- 集成DeviceScanDialog

**布局修改**:
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal">

    <com.swx.adbremote.components.NewTextInputLayout
        android:id="@+id/input_layout_text_ip"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1">

        <EditText
            android:id="@+id/text_ip"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="@string/text_ip" />
    </com.swx.adbremote.components.NewTextInputLayout>

    <ImageButton
        android:id="@+id/btn_scan_ip"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="center_vertical"
        android:src="@drawable/ic_scan"
        android:contentDescription="@string/scan_ip" />
</LinearLayout>
```

### 5. DeviceScanAdapter（新增）
**包路径**: `com.swx.adbremote.adapter.DeviceScanAdapter`

**职责**:
- 管理扫描结果设备列表的展示
- 处理设备项点击事件

## 数据结构设计

### DeviceInfo
```java
public class DeviceInfo implements Serializable {
    private String ipAddress;      // IP地址
    private int port;             // 端口号
    private String deviceName;    // 设备名称（可选）
    private long responseTime;    // 响应时间（毫秒）
    private String macAddress;    // MAC地址（可选）

    // Getters and Setters
    // equals() and hashCode() 基于IP和Port
}
```

## 核心算法设计

### 1. 获取本地网段
```
算法：获取本地子网

输入: Context context
输出: String subnet (如 "192.168.1")

步骤：
1. 获取WifiManager实例
2. 获取DhcpInfo对象
3. 提取IP地址并转换为整数
4. 提取子网掩码
5. 计算网络地址: IP & Mask
6. 转换为字符串格式返回

异常处理：
- 如果无法获取网络信息，抛出NetworkException
- 如果不是WiFi网络，提示用户连接WiFi
```

### 2. 并发端口扫描
```
算法：并发扫描IP段

输入: String subnet, int port, int threadCount
输出: List<DeviceInfo>

步骤：
1. 创建固定大小的线程池（threadCount个线程）
2. 创建CompletionService用于管理异步任务
3. 提交255个IP扫描任务（xxx.1 - xxx.254）
4. 每个任务：
   a. 尝试连接目标IP的5555端口
   b. 超时时间设置为1000ms
   c. 如果连接成功，创建DeviceInfo对象
   d. 关闭Socket连接
5. 收集所有成功的连接结果
6. 关闭线程池
7. 返回设备列表

优化：
- 使用CountDownLatch等待所有任务完成
- 限制最大并发数为15-20
- 使用AtomicInteger统计扫描进度
```

### 3. 端口连接检测
```
算法：检测端口是否开放

输入: String ip, int port, int timeout
输出: boolean

步骤：
1. 创建Socket对象
2. 尝试连接到目标IP和端口
3. 设置连接超时时间
4. 如果连接成功，立即关闭Socket并返回true
5. 如果连接超时或失败，返回false
6. 确保Socket在finally块中被关闭

异常处理：
- 捕获所有异常并返回false
- 使用try-with-resources确保资源释放
```

## 线程模型设计

### 主线程（UI Thread）
- 处理用户交互
- 更新UI界面
- 显示进度和结果

### 工作线程（Background Thread）
- IP扫描任务
- Socket连接检测
- 网络信息获取

### 线程间通信
```
主线程 ──startScan()──> IpScanService
IpScanService ──创建──> ThreadPool
ThreadPool ──执行──> NetworkScanner
NetworkScanner ──callback──> IpScanService
IpScanService ──通知──> 主线程（通过Handler）
主线程 ──更新UI──> DeviceScanDialog
```

## 权限需求

### AndroidManifest.xml新增权限
```xml
<!-- 已有权限，无需新增 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Android 13+ 需要通知权限（可选，用于扫描完成提示） -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 性能优化策略

### 1. 扫描速度优化
- 使用线程池并发扫描（15-20个线程）
- 设置合理的连接超时时间（1000ms）
- 跳过网络地址和广播地址（.0和.255）

### 2. 内存优化
- 限制扫描结果数量（最多20个）
- 及时释放Socket连接
- 使用弱引用持有扫描结果

### 3. 用户体验优化
- 实时显示扫描进度
- 发现设备时立即显示（不等待全部完成）
- 允许用户取消扫描
- 扫描超时保护（30秒）

## 错误处理设计

### 错误场景和处理

| 错误场景 | 处理方式 | 用户提示 |
|---------|---------|---------|
| 未连接WiFi | 显示提示对话框 | "请先连接WiFi网络" |
| 网络权限被拒绝 | 引导用户开启权限 | "需要网络权限才能扫描" |
| 扫描超时 | 停止扫描并显示结果 | "扫描超时，已发现X台设备" |
| 扫描过程中WiFi断开 | 停止扫描并提示 | "网络连接已断开" |
| 未发现任何设备 | 显示友好提示 | "未发现ADB设备，请确认设备已开启ADB调试" |
| 扫描异常 | 捕获异常并记录日志 | "扫描失败，请稍后重试" |

## 测试策略

### 单元测试
- NetworkScanner的扫描逻辑
- 端口连接检测函数
- IP地址解析函数

### 集成测试
- IpScanService与NetworkScanner的集成
- DeviceScanDialog的UI交互
- 扫描结果回调机制

### 手动测试场景
1. **正常场景**：局域网内有1-3台ADB设备
2. **边界场景**：局域网内有20+台ADB设备
3. **异常场景**：WiFi断开、无设备、超时
4. **性能场景**：扫描254个IP的耗时

## 实施计划

### 阶段1：核心扫描功能（1-2天）
- 创建NetworkScanner类
- 实现IP扫描算法
- 实现端口检测逻辑

### 阶段2：服务层和UI（1天）
- 创建IpScanService
- 创建DeviceScanDialog
- 实现DeviceScanAdapter

### 阶段3：集成到现有界面（0.5天）
- 修改ConnectOperateDialog布局
- 添加扫描按钮
- 集成扫描功能

### 阶段4：测试和优化（0.5天）
- 功能测试
- 性能优化
- Bug修复

### 总计：3-4天

## 风险和缓解措施

### 技术风险
1. **扫描速度慢**
   - 风险：254个IP扫描可能超过30秒
   - 缓解：优化线程池配置，提前返回已发现设备

2. **内存占用高**
   - 风险：大量并发Socket连接可能占用过多内存
   - 缓解：限制并发数，及时释放资源

3. **防火墙阻止**
   - 风险：某些防火墙可能阻止端口扫描
   - 缓解：提供手动输入IP的备选方案

### 用户体验风险
1. **扫描结果不准确**
   - 风险：可能发现非ADB设备
   - 缓解：增加端口验证，确保是ADB服务

2. **网络性能影响**
   - 风险：扫描可能影响局域网性能
   - 缓解：控制扫描频率，限制并发数

## 后续扩展可能

1. **设备名称获取**：通过adb shell getprop获取设备名称
2. **历史记录**：保存扫描到的设备，下次快速连接
3. **自动连接**：发现设备后自动尝试连接
4. **扫描计划**：支持定时扫描或后台扫描
5. **多端口扫描**：支持扫描自定义端口范围

## 附录

### 关键代码示例

#### 1. 端口检测核心代码
```java
private boolean isPortOpen(String ip, int port, int timeout) {
    Socket socket = null;
    try {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), timeout);
        return true;
    } catch (IOException e) {
        return false;
    } finally {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket", e);
            }
        }
    }
}
```

#### 2. 获取本地网段
```java
private String getLocalSubnet(Context context) throws Exception {
    WifiManager wifiManager = (WifiManager) context.getApplicationContext()
        .getSystemService(Context.WIFI_SERVICE);
    if (wifiManager == null) {
        throw new Exception("WiFi not available");
    }

    DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
    int ip = dhcpInfo.ipAddress;
    int mask = dhcpInfo.netmask;

    int networkAddress = ip & mask;
    return String.format("%d.%d.%d",
        (networkAddress >> 24) & 0xFF,
        (networkAddress >> 16) & 0xFF,
        (networkAddress >> 8) & 0xFF);
}
```

#### 3. 扫描按钮点击事件
```java
btnScanIp.setOnClickListener(v -> {
    if (ipScanService == null) {
        ipScanService = new IpScanServiceImpl();
    }

    if (ipScanService.isScanning()) {
        Toast.makeText(getContext(), "正在扫描中...", Toast.LENGTH_SHORT).show();
        return;
    }

    DeviceScanDialog dialog = new DeviceScanDialog(getContext());
    dialog.setOnDeviceSelectedListener(device -> {
        textIp.setText(device.getIpAddress());
        textPort.setText(String.valueOf(device.getPort()));
        if (TextUtils.isEmpty(textAlias.getText())) {
            textAlias.setText(device.getIpAddress());
        }
    });
    dialog.show();
});
```
