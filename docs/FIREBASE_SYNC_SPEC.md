# Firebase 多端同步功能规格说明书 (Spec)

## 1. 目标 (Goal)
实现应用数据的云端存储与多端实时同步，支持 Android 和未来的 iOS 端数据互通。
核心同步范围：**每日阅读文章**、**饮食记录（含图片）**、**健康数据（体重/血压）**。

## 2. 架构设计 (Architecture)

采用 **Firebase** 作为统一后端服务：
*   **Authentication**: 管理用户身份，确保数据隔离（用户 A 只能看用户 A 的数据）。
*   **Cloud Firestore**: NoSQL 数据库，存储结构化数据（文章、饮食记录文本、健康数据）。
*   **Cloud Storage**: 对象存储，专门存储饮食记录中的**用户上传图片**。

### 2.1 离线优先策略 (Offline First)
*   **读取**: UI 优先监听 Firebase 实时数据流。Firestore SDK 自带本地持久化缓存，无网时直接读取本地缓存。
*   **写入**: 所有写入操作直接调用 Firebase API。SDK 会自动处理离线队列和网络重连后的同步。
*   **本地 Room 数据库**: 
    *   *方案*: 保持 Room 为“单一可信源” (Single Source of Truth) 或双写。建议逐步**让 Firebase 接管数据层**。

## 3. 数据模型设计 (Data Models)

所有数据将存储在 `users/{userId}/` 集合下，确保数据隐私。

### 3.1 饮食记录 (Events)
**Firestore Path**: `users/{userId}/events/{eventId}`

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 文档 ID (UUID) |
| `type` | String | "Main Meal" 或 "Snack" |
| `timestamp` | Number | 记录发生的时间戳 (Long) |
| `image_url` | String | **关键**: 图片在 Firebase Storage 的下载链接 (http...) |
| `local_image_path` | String | (仅本地使用) 原始本地路径，用于上传未完成时的本地显示 |

### 3.2 健康数据 (HealthData)
**Firestore Path**: `users/{userId}/health_data/{dataId}`

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 文档 ID |
| `type` | String | "Weight" 或 "BloodPressure" |
| `value1` | Number | 体重(kg) 或 高压(mmHg) |
| `value2` | Number | 低压(mmHg) (仅血压有，体重为 null) |
| `timestamp` | Number | 记录时间 |

### 3.3 文章 (Articles)
**Firestore Path**: `users/{userId}/articles/{articleId}`

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | String | 文档 ID |
| `title` | String | 标题 |
| `content` | String | 正文内容 |
| `url` | String | 原文链接 |
| `timestamp` | Number | 添加时间 |
| `read_date` | String | (可选) 辅助字段 "YYYY-MM-DD" 方便按日查询 |

## 4. 关键流程 (Key Workflows)

### 4.1 图片上传流程
1.  用户拍照 -> 获得本地文件 `local_file.jpg`。
2.  UI 立即显示本地图片（基于本地路径）。
3.  **后台**:
    *   将 `local_file.jpg` 上传至 Firebase Storage: `images/{userId}/{timestamp}.jpg`。
    *   获取下载 URL: `https://firebasestorage.googleapis.com/...`。
    *   将 URL 写入 Firestore `events` 文档。
4.  **同步端 (iOS/其他 Android)**:
    *   监听 Firestore 更新 -> 收到含 `image_url` 的新文档。
    *   使用图片加载库 (Coil/Glide) 加载该 URL。

### 4.2 历史数据迁移 (Migration)
*   **场景**: 用户已在使用 App，本地 SQLite 里堆积了大量数据。
*   **动作**: 在用户首次登录成功后，触发一次性的 **"Cloud Sync"** 任务。
    *   读取所有 Room 数据。
    *   批量写入 Firestore。
    *   对于包含本地图片的 Event，触发批量上传任务。

### 4.3 认证 (Authentication)
*   首选 **Google Sign-In**：Android 体验最佳，一键登录。
*   备选 **Email/Password**：通用性强。

## 5. UI 变更
*   **"我的"页面**: 增加 "登录/同步" 入口。
*   **设置页**: 增加 "仅 WiFi 上传图片" 开关。

## 6. 风险与规避
*   **图片流量**: 默认开启图片压缩（在上传前将图片压缩至 1080p 或更低），大幅减少流量和存储占用。
*   **权限**: 需要申请 INTERNET 权限（已有）。

## 7. 下一步行动 (Action Items)
1.  创建 Firebase 项目。
2.  集成 SDK。
3.  编写 Repository 和 ViewModel。
