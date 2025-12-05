# LandPlasmoVoice

一个用于在 Minecraft 服务器上联动 **Lands** 领地插件与 **PlasmoVoice** 语音插件的简单扩展。

通过在 Lands 中注册一个新的角色权限标志 `speak`，本插件可以控制玩家在领地 / 子区域内是否允许使用语音聊天。

## 功能概述

- 在 Lands 中注册自定义角色标志 `speak`（类别：ACTION）。
- 在玩家通过 PlasmoVoice 开始说话时，检查其当前所在位置是否允许说话。
- 若所在位置禁用 `speak`：
  - 自动取消该次语音发送；
  - 每 3 秒最多提示一次：`你在当前领地的子区域中已禁止发言`。
- 支持通过权限跳过检查。

## 依赖

- **Minecraft 服务端**：Paper / Spigot 等 Bukkit 系列服务端。
- **Lands** 插件：用于领地与角色权限系统。
- **PlasmoVoice** 插件：用于语音聊天。

这三个插件都需要正确安装并启用，本插件才会生效。

## 权限

- `landspeak.bypass`  
  拥有该权限的玩家不会受到领地内 `speak` 标志的限制，始终可以在 PlasmoVoice 中发言。

## 使用说明

1. 将编译好的 `LandPlasmoVoice` 插件放入服务器的 `plugins` 目录。
2. 确保服务器已安装并启用了：
   - `Lands`
   - `PlasmoVoice`
3. 重启或重载服务器，插件将在 `onLoad` 阶段向 Lands 注册 `speak` 角色标志。
4. 在 Lands 的角色权限配置中，为不同角色配置 `speak` 标志：
   - 允许：该角色在对应领地内可以使用语音；
   - 禁止：该角色在对应领地内无法通过 PlasmoVoice 说话。

## 工作原理简述

- 插件在加载时通过 Lands API 注册一个名为 `speak` 的角色标志，并在界面中显示为“Speak”。
- 当玩家说话（`PlayerSpeakEvent`）时：
  - 获取玩家当前位置对应的 Lands 领地 / 子区域；
  - 调用 Lands API 检查该玩家在当前位置是否拥有 `speak` 标志；
  - 无权限则取消语音事件，并发送中文提示信息；
  - 为减少 API 调用与刷屏，对检查结果做了 3 秒的简单缓存。

本插件不提供额外命令，仅作为 Lands 与 PlasmoVoice 间的权限桥接工具使用。

