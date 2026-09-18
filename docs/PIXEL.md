# Pixel 原生实时通知适配

## 2026-09-11：通知图标与联系人头像

设备：Pixel 11 Pro XL，Android 17 / API 37，固件 `CD1A.260905.001.B1`。

### 原因与改动

Pixel 的原生实时通知弹窗将发布应用的启动图标放在左侧，将 `largeIcon` 放在右侧。
设置来源 `smallIcon` 可以改变状态栏胶囊，却不一定改变弹窗左侧的发布应用图标。
本次真实 Telegram 消息在修复前就已带有 117×117 的发送者头像，展开后显示在右侧；左侧的流体胶囊 logo 不是头像字段被覆盖的结果。

从该设备的 `SystemUIGoogle.apk` 检查
`NotificationIconStyleProviderImpl.shouldShowAppIcon()`，确认通知 extras 中的
`android.app.preferSmallIcon=true` 会选择小图标而不是发布应用的启动图标。
`NotificationDeviceProfile` 集中识别设备：厂商或品牌为 OPPO 时保留原有展示；
厂商为 Google 且型号属于 Pixel 系列时启用 Pixel 展示；其他设备使用默认展示。
`NotificationFactory` 仅在 Pixel 配置下添加此提示，同时覆盖锁屏隐藏版本。
继续保留原生模板、来源小图标、联系人大图、操作按钮以及实时通知提升请求。

这是当前 Pixel SystemUI 的实现提示，不是本项目 compileSdk 36 的公开 SDK 合约。
不识别它的系统可以忽略该字段；系统升级后应复查弹窗，不能据此承诺所有 Android 设备的表现。

同时统一头像选择顺序：最新消息发送者头像优先，通知大图兜底。
如果最新消息没有发送者头像，不再倒序借用历史消息中其他人的头像。
来源通知没有提供头像时，不能从聊天应用界面自动取得头像。

### 验证

- Release 构建、JVM 单元测试、Android lint 通过。
- Pixel 真机 `PixelNotificationTest` 三项测试通过：头像优先级、缺失头像时不串用历史发送者、图标提示与隐私版本及提升资格兼容。
- 修正版已通过相同签名覆盖安装，保留用户设置与数据。
- 微信与验证码的完整视觉回归需要相应来源通知；不以 Telegram 测试替代。

公开的实时通知样式约束见 [Android Live Updates 文档](https://developer.android.com/develop/ui/views/notifications/live-update)。
不要为改变图标直接替换为不符合提升条件的自定义通知模板。
