# iOS 移植功能与布局规格说明书 (Spec) v5.0

## 1. 概述 (Overview)
本项目旨在将现有的 Android 应用 "Eat" (日常记录与学习助手) 移植到 iOS 平台。本文档包含详细的业务逻辑、数据源和交互动画说明。

## 2. 导航结构 (Navigation)
采用 `TabView` 结构，包含 4 个 Tab：
1.  **Reading (阅读)** - 图标: `house.fill`
2.  **Listening (听力)** - 图标: `headphones`
3.  **Camera (相机)** - 图标: `camera.fill`
4.  **Profile (我的)** - 图标: `person.fill`

---

## 3. 功能模块详解 (Detailed Features & Layout)

### 3.1 标签页 1: 阅读 (Reading)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: "每日阅读"
    *   **Trailing Item**: `Text` "今日已读 X 篇" (Color: Black, Font: Body).
*   **列表布局 (Layout Spec)**:
    *   **Container**: `LazyVStack` (List), `contentPadding = 16pt`, `spacing = 8pt`.
    *   **Card Item**:
        *   **Style**: 圆角矩形卡片 (Radius 12), 阴影 (Shadow Radius 2, Y 1).
        *   **Content Padding**: `16pt`.
        *   **Title**: Font `Headline` (Bold), MaxLines 1, Truncated Tail. Weight 1.
        *   **Date**: Font `Footnote` (Gray), Format "yy/MM/dd", Alignment Right.
        *   **Interaction**:
            *   **Tap**: 进入文章详情页.
            *   **Long Press**: 弹出删除确认框.
*   **文章详情页 (Detail Spec)**:
    *   **NavBar**: Title "文章详情", Back Button.
    *   **Content**: Scrollable `VStack`. Title (Headline, Bold) + Spacer (8pt) + Body Text.
*   **添加交互**:
    *   FAB (+) 点击 -> 弹窗输 URL (Parsing) -> 编辑页（标题/正文 TextInput） -> 保存.

### 3.2 标签页 2: 听力 (Listening)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: "每日听力"
    *   **Trailing Item**: `Button` "校准/已校准" (Text Black). 点击触发校准逻辑.
*   **数据源**: RSS `http://feeds.feedburner.com/tedtalks_audio`.
*   **布局参数 (Layout Spec)**:
    *   **Top Area (Vinyl)**: 灰色背景 (`Color(0xFFF5F5F5)`).
        *   **Vinyl Size**: `240pt x 240pt`.
        *   **Cover Size**: `130pt x 130pt` (Circle), Position Center.
        *   **Play Button**: Overlay `64pt`.
        *   **Interaction**: 支持 "打碟" 手势 (Drag) 进行 Seek (1圈 = 60秒), 且播放时自动旋转.
    *   **Bottom Area (Transcript)**: 白色背景, `List` 列表.
        *   **Highlight**: 当前播放行高亮为红色 (Color.Red).
        *   **Seek**: 点击任意行，音频跳转至该行 StartTime.
*   **校准逻辑 (Calibration)**:
    *   点击校准按钮，将当前播放进度与第一句台词时间对齐，修正 RSS 音频片头偏差.

### 3.3 标签页 3: 相机 (Camera)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: "吃了什么"
*   **布局参数 (Layout Spec)**:
    *   **Viewfinder**: 宽高比 `3:4`. 拍照中显示实时预览.
    *   **Bottom Control**:
        *   **Shutter Button**: Size `80pt`, White ring.
        *   **Toast**: 拍照时显示 "正在拍照中" Pill.
*   **交互动画 (Interaction)**:
    *   **Capture**: 拍照后，预览缩小为 Diameter `80pt` 的圆球，位于屏幕中心.
    *   **Categorization (Drag)**:
        *   显示左侧文本 "正餐"、右侧文本 "零食".
        *   **Drag Left (< -200pt)**: 归类为 "Main Meal".
        *   **Drag Right (> 200pt)**: 归类为 "Snack".
        *   **Drag Vertical**:若是大幅度垂直拖动，则视为取消/重新拍摄 (Reset).

### 3.4 标签页 4: 我的 (Profile)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: 动态日期 "yyyy-MM-dd" (点击可弹出 DatePicker).
    *   **Actions**: 血压图表入口, 体重图表入口, 设置入口.
*   **整体交互**:
    *   **Horizontal Swipe**: 左右滑动屏幕可切换日期 (上一天/下一天).
*   **布局参数 (Layout Spec)**:
    *   **Header FAB**: Bottom Right, "Add Health Data".
    *   **Timeline (Custom Canvas)**:
        *   **Hour Marks**: Circle Radius `8pt`. Text Offset `+50pt`.
        *   **Lines**: 实线 (Active 期间)，虚线 (Inactive 期间).
        *   **Item Placing**: 根据 `minute` 计算 Y 轴偏移. 重叠项目自动计算横向 Gap.
    *   **Event Items**:
        *   **Style**: Light Blue Box (`#E0F7FA`), Corner `8pt`.
        *   **Content**: Time Text + Image Circles (Stack).
        *   **Tap**: 弹出详情对话框 (Event Detail Dialog).
*   **Event Detail Dialog**:
    *   **Features**:
        *   **Category Switch**: 按钮 "改为正餐/零食".
        *   **Image Pager**: 左右滑动查看多图.
        *   **Delete**: 删除当前事件或特定图片.

### 3.5 子页面: 数据图表 (Charts)
*   **通用交互**:
    *   **Vertical Swipe**: 上下滑动切换月份.
    *   **Tap Point**: 点击数据点，显示具体数值 (Red Text).
*   **体重页 (Weight)**:
    *   **X-Axis (Weight)**: Range `140` - `150` (Jin).
    *   **Y-Axis (Date)**: Top (Day 1) to Bottom (Max Day).
*   **血压页 (BP)**:
    *   **X-Axis (Value)**: Range `70` - `150`.
    *   **Series**: 绘制两条折线 (High / Low).

### 3.6 局域网同步 (LAN Sync)
*   **入口**: 首页 -> 配置对话框 -> "局域网同步" 选项 -> 确认.
*   **功能目标**: 在局域网内无需外网，从“旧手机”向“新手机”传输所有数据（数据库+图片）。
*   **UI 布局**:
    *   **Navbar**: 标题 "局域网同步".
    *   **Mode Selection**: 两个大按钮 "我是发送方", "我是接收方".
    *   **Sender View**: 
        *   **Backup Details**: 顶部显示动态饼状图 (Pie Chart)，展示 Articles, Images, HealthData, Events 的数量分布与百分比图例.
        *   **Server Status**: 居中显示文本 "数据已打包并准备就绪" (中文).
        *   **Address**: 显示本机 IP 端口 (e.g., `192.168.x.x:8081`). 提示 "请在接收方输入此 IP".
    *   **Receiver View**: 输入框 (默认为空) + "连接并同步" 按钮 + 状态日志文本区域 (中文提示).
*   **传输协议 (Tech Spec)**:
    *   **Format**: `backup.zip`. 内含 `data.json` (所有表数据) 和 `images/` (所有图片文件).
    *   **Sender (Server)**:
        *   启动本地 HTTP Server.
        *   **Dynamic Port**: 尝试绑定端口 8080. 若被占用，自动重试 8081-8090，直到成功.
        *   **Stats Calculation**: 再启动服务前，先计算各类数据的数量 (Summary).
        *   端点 `/backup.zip`: 实时打包数据库内容与图片文件夹为 Zip 流并返回.
    *   **Receiver (Client)**:
        *   HTTP GET `http://<IP>:8080/backup.zip`.
        *   下载 -> 解压 -> 解析 JSON -> 批量插入/覆盖本地数据库 -> 复制图片到沙盒.

## 4. iOS 技术映射 (Tech Mapping)
| Android Component | iOS Target | 说明 |
| :--- | :--- | :--- |
| `TopAppBar` | **NavigationStack + .navigationTitle** | 导航栏标准实现 |
| `Jsoup` | **SwiftSoup** | HTML 解析 |
| `Retrofit` | **URLSession** | 网络请求 |
| `Room` | **SwiftData** | 数据库 |
| `Coil` | **Kingfisher** | 图片加载 |
| `CameraX` | **AVFoundation** | 需自定义 PreviewLayer |
| `NanoHTTPD` | **GCDWebServer** / **Telegraph** | 本地 HTTP 服务器 |
| `java.util.zip` | **ZIPFoundation** | Zip 压缩与解压 |

## 5. 存储模型 (Schema)
*   `Article`: `id`, `title`, `content`, `url`, `timestamp`
*   `Event`: `id`, `type`, `timestamp`, `imagePath`
*   `HealthData`: `id`, `type`, `value1`, `value2`, `timestamp`
