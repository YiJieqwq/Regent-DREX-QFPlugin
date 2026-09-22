# Regent-DREX

**English** | [中文](README.zh-CN.md)

> A QQ group administration command system — built on the DREX command-routing architecture, a sibling of Corax Strata.

## Introduction

Regent-DREX is a group management tool running in the QFun environment, built on a registry + argument-parser architecture.
Every command is an English `/`-prefixed command and accepts either `@user` or a bare UIN.
Each action reply includes a blue, clickable mention.

## Quick Start

### Installation

1. Extract the CI bundle into the QFun plugin directory
2. Send `/on` in the target group to enable group administration
3. Start using the commands

### Basic Setup

```
/on                  # Enable administration in this group
/on                  # Repeated enable → "already enabled"
/off                 # Disable administration in this group
/admin add @someone  # Add a delegate
```

## Commands

```
/mute @someone <duration> <reason (optional)>
/mute all
/mute list

/unmute @someone
/unmute all

/kick @someone <reason (optional)>

/ban @someone <reason (optional)>
/ban list

/admin add @someone
/admin rm @someone
/admin list
/admin clear

/alliance add
/alliance rm

/fban @someone <reason (optional)>
/unfban @someone <cause (optional)>

/toggle <feature> on|off
/set <key> <value>
/status
/help
```

## Arguments

| Type | Format | Example |
|------|------|------|
| Duration | `30s` `15m` `2h` `1d` | `/mute @Alice 30m` |
| User | `@someone` or a UIN | `/kick @Alice` or `/kick 123456` |
| Reason | Any text, placed last | `/kick @Alice advertising` |

## Feature Toggles

| Feature | Description | Example |
|--------|------|------|
| `muteonat` | Mute someone for mentioning the bot | `/toggle muteonat on` |
| `autoban` | Auto-blacklist users who leave the group | `/toggle autoban on` |
| `selftitle` | Self-service titles | `/toggle selftitle off` |
| `unmutedelegate` | Auto-unmute a muted delegate | `/toggle unmutedelegate on` |

## Configuration Keys

| key | Type | Description | Example |
|-----|--------|------|------|
| `mutetime` | Seconds | Default duration for mention-triggered mutes | `/set mutetime 86400` |

## Permission Model

| Role | Allowed commands |
|------|-----------|
| Host | All commands (`/on` `/off` `/admin` `/alliance` are host-only) |
| Delegate | `/mute` `/unmute` `/kick` `/ban` `/fban` `/unfban` `/toggle` `/set` `/status` |
| Member | `/help` |

### Protection Rules

- The host can never be acted on by any administrative command
- Once added, a delegate enters a protected pool: **nobody** (including the host) can act on a delegate via `/mute` `/kick` `/ban`
- A delegate cannot act on another delegate
- Unauthorized users sending commands → silently ignored, no message sent
- Unknown commands → silently ignored, no message sent
- Repeated `/on` `/off` → a notice that it is already enabled/disabled

## Reply Format

```
Muted! User: Alice(123456) Duration: 30m Reason: spamming By: [atUin=789012]
```

`[atuin=xxx]` renders in QQ as a blue, clickable mention.

## Event Listeners

| Event | Behavior |
|------|------|
| User joins group | Checks the leave-blacklist and alliance ban list; kicks automatically on a hit |
| User leaves group | When leave-blacklisting is on, adds the user to the blacklist |
| User gets muted | When auto-unmute-delegates is on, unmutes a delegate who was muted |

## Storage Layout

```
config/
├── enabled_sessions.txt    # Sessions where administration is enabled
├── delegates/list.txt      # Delegate list
├── banlist/{group}.txt     # Leave-blacklist per group
├── group_config/{group}.json # Per-group settings
├── alliance.txt            # Alliance group list
├── fban_list.txt           # Alliance ban list
└── global_config.json      # Global configuration
```

## Author

YiJieqwq — released under the MIT License.
