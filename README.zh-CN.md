# Regent-DREX

[English](README.md) | **中文**

> QQ 群管指令系统 — 基于 DREX 指令路由架构，与墨鸦 Strata 同源。

## 简介

Regent-DREX 是一个运行在 QFun 环境下的群管理工具，采用注册表 + 参数解析器架构。
所有指令为 `/` 开头的英文格式，支持 `@用户` 和直接 UIN 两种指定方式，
每条操作反馈附带蓝色可点击艾特。

## 快速开始

### 安装

1. 将 CI 集成包解压至 QFun 插件目录
2. 在目标群发送 `/on` 启用群管
3. 开始使用指令

### 基础配置

```
/on                  # 启用本群群管
/on                  # 重复启用 → 提示“无需重复开启”
/off                 # 禁用本群群管
/admin add @someone  # 添加代管
```

## 指令

```
/mute @someone <时间> <理由(可选)>
/mute all
/mute list

/unmute @someone
/unmute all

/kick @someone <理由(可选)>

/ban @someone <理由(可选)>
/ban list

/admin add @someone
/admin rm @someone
/admin list
/admin clear

/alliance add
/alliance rm

/fban @someone <理由(可选)>
/unfban @someone <原因(可选)>

/toggle <功能名> on|off
/set <key> <value>
/status
/help
```

## 参数

| 类型 | 格式 | 示例 |
|------|------|------|
| 时间 | `30s` `15m` `2h` `1d` | `/mute @张三 30m` |
| 用户 | `@某人` 或 `UIN` | `/kick @张三` 或 `/kick 123456` |
| 理由 | 任意文本，放在最后 | `/kick @张三 广告` |

## 功能开关

| 功能名 | 说明 | 示例 |
|--------|------|------|
| `muteonat` | 艾特机器人即禁言 | `/toggle muteonat on` |
| `autoban` | 退群自动拉黑 | `/toggle autoban on` |
| `selftitle` | 自助头衔 | `/toggle selftitle off` |
| `unmutedelegate` | 代管被禁言自动解禁 | `/toggle unmutedelegate on` |

## 配置项

| key | 值类型 | 说明 | 示例 |
|-----|--------|------|------|
| `mutetime` | 秒数 | 艾特禁言默认时长 | `/set mutetime 86400` |

## 权限模型

| 角色 | 可执行指令 |
|------|-----------|
| 宿主 | 全部指令（`/on` `/off` `/admin` `/alliance` 仅宿主） |
| 代管 | `/mute` `/unmute` `/kick` `/ban` `/fban` `/unfban` `/toggle` `/set` `/status` |
| 成员 | `/help` |

### 保护规则

- 宿主不可被任何管理指令操作
- 代管一旦添加即进入受保护池，**任何人**（包括宿主）都不能通过 `/mute` `/kick` `/ban` 操作代管
- 代管不能操作其他代管
- 无权限用户发指令 → 直接忽略，不发任何消息
- 未知指令 → 直接忽略，不发任何消息
- 重复 `/on` `/off` → 提示已开启/已关闭

## 反馈格式

```
禁言成功! 用户: 张三(123456) 时长: 30m 理由: 刷屏 执行人:[atUin=789012]
```

`[atuin=xxx]` 在 QQ 中显示为蓝色可点击艾特。

## 事件监听

| 事件 | 行为 |
|------|------|
| 用户入群 | 检查退群黑名单和联盟封禁，命中则自动踢黑 |
| 用户退群 | 退群拉黑开启时，自动加入黑名单 |
| 用户被禁言 | 自动解禁代管开启时，检测代管被禁言则自动解禁 |

## 存储结构

```
config/
├── enabled_sessions.txt    # 已启用群管会话列表
├── delegates/list.txt      # 代管列表
├── banlist/{群号}.txt       # 退群黑名单
├── group_config/{群号}.json # 群设置
├── alliance.txt            # 联盟群列表
├── fban_list.txt           # 联盟封禁列表
└── global_config.json      # 全局配置
```

## 作者

YiJieqwq 基于 MIT 协议开源
