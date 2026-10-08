# 药物服用与预测 App —— 实现方案

## Context（背景与目标）

用户要从零开发一款「简约、色调清新」的安卓 App，用于药物服用管理、服药打卡与用药时长预测。目录 `a:\workflow\Medicine` 当前为空、无 git 仓库，属于全新项目。

已确认的关键决策（用户答复）：

1. 技术栈：**Kotlin + Jetpack Compose（Material 3）**
2. 药物预测逻辑：**两者结合**（可服天数 + 最少持续数量阈值，任一触发即提醒）
3. 导航结构：**底部 4 个 Tab + 从「打卡」页右滑露出「设置」负一屏**

本机环境：Java 21、Android SDK（platform `android-36.1`、build-tools `36.1.0`）、模拟器 `Medium_Phone_API_36`、adb 可用；Gradle **8.14.3** 已缓存于 `~/.gradle/wrapper/dists`，可直接复用避免下载。

## 技术栈与版本矩阵

| 组件                          | 版本            | 说明                                          |
| --------------------------- | ------------- | ------------------------------------------- |
| Gradle                      | 8.14.3        | 本机已缓存                                       |
| AGP                         | 8.9.1         | 支持 compileSdk 36，JDK 17+（本机 21 兼容）          |
| Kotlin                      | 2.1.20        | 含 `org.jetbrains.kotlin.plugin.compose` 同版本 |
| KSP                         | 2.1.20-1.0.32 | Room 注解处理                                   |
| Compose BOM                 | 2025.06.01    | Material3 / Foundation / UI                 |
| Room                        | 2.7.1         | 本地数据库                                       |
| DataStore                   | 1.1.3         | 偏好设置（提醒、提前预警天数等）                            |
| lifecycle-viewmodel-compose | 2.9.0         | ViewModel                                   |
| activity-compose            | 1.10.1        | 组件 + Photo Picker                           |

* `compileSdk` / `targetSdk` = 36，`minSdk` = 26（Android 8.0，支持通知渠道与前台服务）。

* 包名：`com.baiyue.medicine`。

* 若构建时某依赖版本解析失败（如 KSP/BOM 微版本），在执行阶段即时 WebSearch 校正并重跑构建。

## 项目结构（单模块 `app`）

```
Medicine/
├── settings.gradle.kts
├── build.gradle.kts                # 根
├── gradle.properties
├── gradle/libs.versions.toml       # 版本目录
├── gradle/wrapper/gradle-wrapper.properties   # 指向 8.14.3
├── gradlew / gradlew.bat / gradle-wrapper.jar # 用本机 gradle 生成
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml     # 仅声明必要权限
        ├── res/values/...          # 字符串/主题/图标
        └── java/com/baiyue/medicine/
            ├── MedicineApp.kt      # Application + 通知渠道
            ├── MainActivity.kt     # 负一屏 Pager + 底部导航
            ├── data/               # entity / dao / db / repository
            ├── model/              # 领域模型与枚举
            ├── prediction/         # 预测计算（纯函数，可单测）
            ├── reminder/           # AlarmManager / 前台服务 / 通知
            └── ui/
                ├── theme/          # 清新配色 Color.kt / Theme.kt
                ├── checkin/        # 打卡
                ├── calendar/       # 日历
                ├── manage/         # 药品管理
                ├── card/           # 小卡片
                └── settings/       # 设置（负一屏，含声明）
```

## 数据模型（Room）

`Medication`（药品）

* `id` 主键；`name` 名称；`abbreviation` 缩写（名称超阈值时强制要求，否则可空）

* `imageUri` 自定义图片（Photo Picker URI）

* `usage` 使用方式（枚举默认值 + `customUsage` 自定义文案）

* `doseType` 服用类型枚举：`MORNING_NOON_NIGHT`（早中晚，各带时间区间）/ `EVERY_HOURS`（每 N 小时）/ `CUSTOM`（自定义次数）/ 由 `isOccasional` 区分偶尔服用

* `doseTimes` 具体时间点（JSON/字符串列表）；`intervalHours`（每几小时）

* `timesPerDay` 每日次数；`isOccasional` 是否偶尔服用

* `quantity` 当前数量；`minKeepQuantity` 最少持续数量阈值

* `warningDays` 提前预警天数；`enabled` 启用开关

`DoseRecord`（打卡记录）

* `id`、`medicationId`、`date`(yyyy-MM-dd)、`time`、`category`(NORMAL/OCCASIONAL)、`taken`、`note`、`createdAt`（用于日历着色与详情）

## 功能设计（逐条对应需求）

### 1. 药物预测（两者结合）

* 每日用量 `dailyDose`：早中晚=启用的时段数；每 N 小时=`24/intervalHours`；自定义=`timesPerDay`；偶尔服用不计入每日预测。

* 结果：`daysRemaining = quantity / dailyDose`；`runOutDate = 今天 + daysRemaining`。

* 触发「就医提醒」条件（任一满足）：`daysRemaining <= warningDays` **或** `quantity <= minKeepQuantity`。

* 日历「红色就医时间段」= `[runOutDate - warningDays, runOutDate]` 之间（叠加各类药品）。

* 纯函数集中在 `prediction/Prediction.kt`，用 JUnit 单测覆盖。

### 2. 药物管理

* 列表 + 新增/编辑/删除；表单包含：名称（超长度强制缩写）、图片（系统 Photo Picker，无需权限）、使用方式（默认 + 自定义）、剂量类型（早中晚时间区间 / 每几小时 / 自定义 / 偶尔服用）、数量、最少持续数量、提前预警天数。

### 3. 日历与打卡

* 日历：自绘月历网格，按日期着色——绿色=当日有吃药记录，红色=落在就医时间段，橙色=当日服用了偶尔用药；点击日期弹出当日记录详情。

* 打卡页：显示「本次该吃的药」列表（名称过长显示缩写，点击展开全称）；**大按钮**→记录本次服用并生成随机颜色小卡片（正反馈）；**小按钮**（偶尔服用）→弹出偶尔药品列表（编号 1/2/3 选择）→ 再点大按钮记录。

### 4. 提醒与隐私

* 通知渠道：`吃药提醒`、`就医提醒`（可自定义声音/震动）。

* `AlarmManager.setExactAndAllowWhileIdle` 排程吃药/就医提醒，系统广播后重排。

* **强保活**：前台服务（常驻低优先级通知）+ 引导关闭电池优化（`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`）。

* **最小权限**：仅 `POST_NOTIFICATIONS`（运行时）；图片用 Photo Picker 免权限；不申请定位等无关权限。

### 5. 药物服用小卡片

* 汇总卡片：当日/今日已吃药品 + 各服用次数、今日应服总数 vs 已打卡数，作为轻量概览页展示。

### 6. 声明

* 在「设置→关于/声明」页展示：「本作品为开源项目仅供学习使用，作者为白玥，邮箱为 <A2870315058@outlook.com>」，邮箱做 `mailto:` 链接；同时在 `MainActivity.kt` 顶部注释声明版权。不额外创建 README/LICENSE（如需可后续补充）。

## 导航结构（负一屏）

`MainActivity` 用 `HorizontalPager`：

* 下标 `0` = **设置（负一屏）**；下标 `1..4` = 打卡 / 日历 / 药品管理 / 小卡片。

* 底部导航 4 项映射到 `1..4`；默认停在 `1`（打卡）；在打卡页向右滑动即滑出下标 0 的设置负一屏。

## 主题（简约·清新）

`ui/theme/Color.kt` 采用 Material 3 浅色系：主色薄荷绿 `#4CAF50`、辅色青绿 `#26A69A`、背景 `#F4FAF7`、卡片白 `#FFFFFF`；警示红 `#E57373`（就医）、偶尔服用橙 `#FFB74D`、打卡绿 `#81C784`。圆角卡片 + 留白，整体简约。

## 实施步骤

1. 脚手架：`settings.gradle.kts`、根 `build.gradle.kts`、`gradle.properties`、`libs.versions.toml`、`gradle-wrapper.properties`；用本机 Gradle 8.14.3 执行 `gradle wrapper` 生成 `gradlew`/`gradle-wrapper.jar`。
2. `app/build.gradle.kts`、`AndroidManifest.xml`、`res`（应用名、主题、启动图标）、`MedicineApp`、`theme/*`。
3. 数据层：`entity`/`dao`/`MedicineDatabase`/`repository` + `model/` 枚举。
4. 预测层：`prediction/Prediction.kt` + 单元测试。
5. 提醒层：`reminder/`（通知渠道、AlarmManager 排程、前台服务、AwarenessHelper）。
6. UI：`MainActivity` 负一屏 Pager + 底部导航；随后 `checkin`、`calendar`、`manage`、`card`、`settings` 各页与 ViewModel 交互。
7. 联调与构建。

## 验证方式

* 构建：`gradlew.bat assembleDebug`（复用本机 Gradle 缓存与网络下载依赖，产出 `app-debug.apk`）。

* 逻辑单测：`gradlew.bat testDebugUnitTest`（重点覆盖预测 `daysRemaining`/触发条件）。

* 运行冒烟：`adb install` 到 `Medium_Phone_API_36` 模拟器，`adb shell am start` 启动；`adb shell screencap` 截图核对各页 UI 与日历着色、负一屏滑动。

