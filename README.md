一.项目需求：
1、软件运行在android 智能电视上
2、家长可以设定每次可以观看的时长，当快到设定的时长，提前2 分钟提示“你已使用\*\* 分钟，该休息一下了。”（此处的提前多少分钟也要可以由用户设置，默认是2 分钟）。时间一到则将屏幕锁定；
3、屏幕锁定后如需解锁，需要输入事先设定的密码；
4、在不输入密码的情况下，如需再次观看，需要等待20分钟，屏幕自动解锁

二.项目目录：
Healthy_Way_Using_TV/
├── build.gradle.kts # 根构建配置
├── settings.gradle.kts # 项目设置
├── gradle.properties # Gradle属性
├── gradle/libs.versions.toml # 版本目录
├── app/
│ ├── build.gradle.kts # 应用构建（Compose + Material3）
│ ├── proguard-rules.pro
│ └── src/main/
│ ├── AndroidManifest.xml # TV应用声明
│ ├── res/ # 资源文件
│ └── java/com/healthyway/tv/
│ ├── data/
│ │ ├── TimerConfig.kt # 配置数据类
│ │ ├── TimerState.kt # 状态枚举
│ │ └── PreferencesManager.kt # 首选项管理（单例）
│ ├── service/
│ │ ├── TimerService.kt # 前台计时服务（核心）
│ │ └── TimerNotificationHelper.kt # 通知管理
│ ├── ui/
│ │ ├── theme/Theme.kt # TV深色主题
│ │ ├── main/ # 主界面 + ViewModel
│ │ ├── settings/ # 设置界面 + ViewModel
│ │ └── lock/ # 锁定界面 + Activity + VM
│ ├── receiver/
│ │ └── BootReceiver.kt # 开机恢复
│ ├── MainActivity.kt
│ └── HealthyWayApp.kt
└── docs/
└── 开发文档.md # 完整开发文档

---

三.核心功能实现
需求 实现
设定观看时长 SettingsScreen 1~480分钟
提前提醒（可配置） TimerService 在剩余 warningMinutes 时发通知
到时锁定屏幕 TimerService → LockScreenActivity 全屏启动
密码解锁 LockViewModel SHA-256 哈希校验
20分钟自动解锁 LockViewModel 倒计时自动触发
开机恢复 BootReceiver → TimerService 自动恢复
架构特点
MVVM 模式，ViewModel 通过 StateFlow 驱动 UI
高内聚低耦合：Data/Service/UI 三层分离，LocalBroadcastManager 通信
核心方法均使用中文备注
使用 AndroidStudio 打开项目后，连接电视 ADB 即可 Run
