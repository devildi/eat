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
*   **添加交互**:
    *   FAB (+) 点击 -> 弹窗输 URL -> Loading -> 编辑页（标题和正文分别放在两个textinput中） -> 查重 (按标题) -> 保存（写入本地）.

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
    *   **Bottom Area (Transcript)**: 白色背景, 可滚动列表.
*   **交互动画**:
    *   **旋转**: 播放时 3s/圈.
    *   **打碟**: 拖拽旋转, 1圈 = 60秒.
*   **校准逻辑 (Calibration)**:
    *   **场景**: RSS 音频流经常带有片头广告，导致音频与 TED 官网爬取的字幕时间轴不匹配（字幕往往也是从正片开始）。
    *   **操作**: 用户听到正片第一句台词开始时，点击导航栏右上角的 "校准" 按钮。
    *   **算法**:
        1.  获取当前播放器时间 `currentPos`.
        2.  获取字幕第一句的原始开始时间 `firstLineTime` (通常为 0 或极小值).
        3.  计算偏移量 `offset = currentPos - firstLineTime`.
        4.  将该 `offset` 加到**所有字幕行**的时间戳上.
        5.  更新 UI，使字幕与音频同步。

### 3.3 标签页 3: 相机 (Camera)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: "吃了什么"
    *   **Trailing Item**: 无.
*   **布局参数 (Layout Spec)**:
    *   **Viewfinder (非全屏)**:
        *   **AspectRatio**: `3:4` (Width = ScreenWidth, Height = Width * 4/3).
        *   **Position**: Top aligned (under NavBar).
        *   **Background**: Black.
    *   **Bottom Control Area**:
        *   **Height**: ScreenHeight - NavBarHeight - ViewfinderHeight. 白色背景.
        *   **Shutter Button**: Size `80pt`. White ring. Center aligned.
        *   **Toast "正在拍照中"**:
            *   Position: 位于快门按钮上方，白色区域内顶部.
            *   Style: `Capsule`, DarkGray (90% alpha), Text White 14sp, Padding H:16 V:8.
*   **转场动画**:
    *   拍照后，**4:3 的预览画面** 缩小 (Scale Down) 并移动，最终变成屏幕中心的一个 **圆形图标 (Diameter 80pt)**.
    *   同时显示左侧 "Main Meal" 和右侧 "Snack" 区域 (Fade In).

### 3.4 标签页 4: 我的 (Profile)
*   **导航栏 (Navigation Bar)**:
    *   **Title**: 动态日期 "yyyy-MM-dd" (点击可弹出 DatePicker).
    *   **Trailing Item 1**: 图标 `heart.fill` (血压图表入口).
    *   **Trailing Item 2**: 图标 `chart.xyaxis.line` (体重图表入口).
*   **布局参数 (Layout Spec)**:
    *   **Header FAB**: Bottom Right. Size `56pt`. RoundedRect `cornerRadius: 16`. Color Black. Shadow 6.
    *   **Timeline (Canvas)**:
        *   **Hour Marks**: Circle Radius `8pt`. Text Offset `+50pt`. Line Width `4pt`.
        *   **Event Items**:
            *   **Content Box**: Light Blue (`#E0F7FA`). Corner `8pt`.
            *   **Images**: `30pt` Circle. Overlap `15pt`.
            *   **Text**: "正餐 HH:mm".

### 3.5 子页面: 数据图表 (Charts)
*   **通用布局 (Canvas Layout Spec)**:
    *   **Margins**: Left `80pt`, Top `40pt`, Right `40pt`, Bottom `60pt`.
    *   **Axis Lines**: Stroke Width `2pt`, Color Black.
*   **体重页 (Weight)**:
    *   **Y-Axis (Date)**: Top to Bottom.
    *   **X-Axis (Value)**: Range `140` to `150` (Jin).
*   **血压页 (BP)**:
    *   **Y-Axis (Date)**: Top to Bottom.
    *   **X-Axis (Value)**: Range `70` to `150`.
    *   **Lines**: Two paths (High/Low).

## 4. iOS 技术映射 (Tech Mapping)
| Android Component | iOS Target | 说明 |
| :--- | :--- | :--- |
| `TopAppBar` | **NavigationStack + .navigationTitle** | 导航栏标准实现 |
| `Jsoup` | **SwiftSoup** | HTML 解析 |
| `Retrofit` | **URLSession** | 网络请求 |
| `Room` | **SwiftData** | 数据库 |
| `Coil` | **Kingfisher** | 图片加载 |
| `CameraX` | **AVFoundation** | 需自定义 PreviewLayer |

## 5. 存储模型 (Schema)
*   `Article`: `id`, `title`, `content`, `url`, `timestamp`
*   `Event`: `id`, `type`, `timestamp`, `imagePath`
*   `HealthData`: `id`, `type`, `value1`, `value2`, `timestamp`
