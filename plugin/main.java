// ==================== 简洁群管 — DREX 指令路由版 ====================
// 功能: /kick /ban /mute /unmute /admin /alliance /fban /unfban /toggle /set /status /help
// 存储: 文件系统（config/ 目录）
// 鉴权: 主人(owner) + 代管(delegate)

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.*;
import org.json.*;

// ==================== 工具方法 ====================
public synchronized void appendLine(File f, String line) {
    try {
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        PrintWriter pw = new PrintWriter(new FileWriter(f, true));
        pw.println(line);
        pw.close();
    } catch (Exception e) { log("appendError: " + e); }
}

public synchronized ArrayList readLines(File f) {
    ArrayList list = new ArrayList();
    if (!f.exists()) return list;
    try {
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) list.add(line);
        }
        br.close();
    } catch (Exception e) { log("readError: " + e); }
    return list;
}

public synchronized void removeLine(File f, String target) {
    if (!f.exists()) return;
    try {
        ArrayList lines = readLines(f);
        lines.remove(target);
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        for (Object l : lines) pw.println(l.toString());
        pw.close();
    } catch (Exception e) { log("removeError: " + e); }
}

public synchronized void overwriteLines(File f, ArrayList lines) {
    try {
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        for (Object l : lines) {
            if (l != null) pw.println(l.toString());
        }
        pw.close();
    } catch (Exception e) { log("overwriteError: " + e); }
}

public boolean containsLine(File f, String target) {
    ArrayList lines = readLines(f);
    return lines.contains(target);
}

// ==================== 文件路径 ====================
String getConfigDir() { return pluginPath + "/config"; }

File getDelegateFile() {
    File dir = new File(getConfigDir() + "/delegates");
    if (!dir.exists()) dir.mkdirs();
    return new File(dir, "list.txt");
}

File getBanFile(String groupId) {
    File dir = new File(getConfigDir() + "/banlist");
    if (!dir.exists()) dir.mkdirs();
    return new File(dir, groupId + ".txt");
}

File getAllianceFile() {
    return new File(getConfigDir() + "/alliance.txt");
}

File getFbanFile() {
    return new File(getConfigDir() + "/fban_list.txt");
}
    
File getEnabledFile() {
    return new File(getConfigDir() + "/enabled_sessions.txt");
}

public boolean isSessionEnabled(String groupId) {
    return containsLine(getEnabledFile(), groupId);
}

// ==================== 权限 ====================
public boolean isOwner(String uin) {
    return uin != null && uin.equals(myUin);
}

public boolean isDelegate(String groupId, String uin) {
    if (uin == null) return false;
    if (isOwner(uin)) return true;
    return containsLine(getDelegateFile(), uin);
}

public boolean isOwnerOrDelegate(String groupId, String uin) {
    return isOwner(uin) || isDelegate(groupId, uin);
}

public boolean isProtected(String groupId, String target, String operator) {
    if (target == null) return false;
    if (isOwner(target)) return true;                     // 宿主不可操作
    if (isDelegate(groupId, target)) return true;         // 代管不可操作（任何人都不行）
    return false;
}

public boolean canOperate(String groupId, String operator, String target) {
    if (operator == null || target == null) return false;
    if (isOwner(operator)) return true;
    if (isDelegate(groupId, operator) && !isDelegate(groupId, target) && !isOwner(target)) return true;
    return false;
}

// ==================== 群组设置 (key-value) ====================
File getGroupConfigFile(String groupId) {
    File dir = new File(getConfigDir() + "/group_config");
    if (!dir.exists()) dir.mkdirs();
    return new File(dir, groupId + ".json");
}

public String getGroupConfig(String groupId, String key, String def) {
    File f = getGroupConfigFile(groupId);
    if (!f.exists()) return def;
    try {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        JSONObject json = new JSONObject(sb.toString());
        return json.optString(key, def);
    } catch (Exception e) { return def; }
}

public void setGroupConfig(String groupId, String key, String value) {
    File f = getGroupConfigFile(groupId);
    JSONObject json = new JSONObject();
    if (f.exists()) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            json = new JSONObject(sb.toString());
        } catch (Exception e) { }
    }
    try {
        if (value == null) json.remove(key);
        else json.put(key, value);
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        pw.print(json.toString(2));
        pw.close();
    } catch (Exception e) { log("setGroupConfigError: " + e); }
}

public boolean isEnabled(String groupId, String feature) {
    return "on".equals(getGroupConfig(groupId, feature, "off"));
}

// ==================== 全局设置 ====================
File getGlobalConfigFile() {
    return new File(getConfigDir() + "/global_config.json");
}

public String getGlobalConfig(String key, String def) {
    File f = getGlobalConfigFile();
    if (!f.exists()) return def;
    try {
        BufferedReader br = new BufferedReader(new FileReader(f));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        JSONObject json = new JSONObject(sb.toString());
        return json.optString(key, def);
    } catch (Exception e) { return def; }
}

public void setGlobalConfig(String key, String value) {
    File f = getGlobalConfigFile();
    JSONObject json = new JSONObject();
    if (f.exists()) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            json = new JSONObject(sb.toString());
        } catch (Exception e) { }
    }
    try {
        if (value == null) json.remove(key);
        else json.put(key, value);
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        pw.print(json.toString(2));
        pw.close();
    } catch (Exception e) { log("setGlobalConfigError: " + e); }
}

// ==================== 联盟管理 ====================
public void addAllianceGroup(String groupId) {
    File f = getAllianceFile();
    if (!containsLine(f, groupId)) appendLine(f, groupId);
}

public void removeAllianceGroup(String groupId) {
    removeLine(getAllianceFile(), groupId);
}

public boolean isAllianceGroup(String groupId) {
    return containsLine(getAllianceFile(), groupId);
}

public ArrayList getAllianceGroups() {
    return readLines(getAllianceFile());
}

// ==================== 封禁用户管理 ====================
public void addFbanUser(String userUin, String reason) {
    File f = getFbanFile();
    String record = userUin + (reason != null && !reason.isEmpty() ? "|" + reason : "");
    // 判断是否已存在，存在则更新
    ArrayList list = readLines(f);
    boolean found = false;
    for (int i = 0; i < list.size(); i++) {
        String line = (String) list.get(i);
        if (line != null && line.startsWith(userUin + "|")) {
            list.set(i, record);
            found = true;
            break;
        } else if (line != null && line.equals(userUin)) {
            list.set(i, record);
            found = true;
            break;
        }
    }
    if (!found) list.add(record);
    overwriteLines(f, list);
}

public void removeFbanUser(String userUin) {
    File f = getFbanFile();
    ArrayList list = readLines(f);
    ArrayList newList = new ArrayList();
    for (Object o : list) {
        String line = (String) o;
        if (line != null && !line.startsWith(userUin + "|") && !line.equals(userUin)) {
            newList.add(line);
        }
    }
    overwriteLines(f, newList);
}

public boolean isFbanUser(String userUin) {
    File f = getFbanFile();
    ArrayList list = readLines(f);
    for (Object o : list) {
        String line = (String) o;
        if (line != null && (line.startsWith(userUin + "|") || line.equals(userUin))) return true;
    }
    return false;
}

public String getFbanReason(String userUin) {
    File f = getFbanFile();
    ArrayList list = readLines(f);
    for (Object o : list) {
        String line = (String) o;
        if (line != null && line.startsWith(userUin + "|")) {
            String[] parts = line.split("\\|", 2);
            if (parts.length > 1) return parts[1];
        }
    }
    return null;
}

// ==================== 成员信息 ====================
public String getMemberDisplayName(String groupId, String uin) {
    if (uin == null) return "未知";
    try {
        if (groupId != null && !groupId.isEmpty()) {
            Object mem = getMemberInfo(groupId, uin);
            if (mem != null && mem.uinName != null) return mem.uinName;
        }
        // 从好友列表查找
        ArrayList friends = getAllFriend();
        if (friends != null) {
            for (Object f : friends) {
                try {
                    String fuin = String.valueOf(f.getClass().getField("uin").get(f));
                    if (fuin.equals(uin)) {
                        String remark = (String) f.getClass().getField("remark").get(f);
                        if (remark != null && !remark.isEmpty()) return remark;
                        return (String) f.getClass().getField("name").get(f);
                    }
                } catch (Exception e2) { }
            }
        }
    } catch (Exception e) { }
    return uin;
}

// ==================== 反馈发送 ====================
public String getCurrentTime() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
}

public void sendFeedback(String groupId, String content) {
    try {
        sendMsg(groupId, content, 2);
    } catch (Exception e) { log("sendFeedbackError: " + e); }
}

public void sendReplyFeedback(String groupId, long replyMsgId, String content) {
    try {
        if (replyMsgId != 0) {
            sendReplyMsg(groupId, replyMsgId, content, 2);
        } else {
            sendMsg(groupId, content, 2);
        }
    } catch (Exception e) {
        sendMsg(groupId, content, 2);
    }
}

// ==================== 时间解析 ====================
public int parseTimeToSeconds(String timeStr) {
    if (timeStr == null || timeStr.isEmpty()) return 0;
    timeStr = timeStr.toLowerCase();
    try {
        if (timeStr.matches("\\d+[smhd]")) {
            char unit = timeStr.charAt(timeStr.length() - 1);
            int num = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
            if (unit == 's') return num;
            if (unit == 'm') return num * 60;
            if (unit == 'h') return num * 3600;
            if (unit == 'd') return num * 86400;
        } else if (timeStr.matches("\\d+")) {
            return Integer.parseInt(timeStr);
        }
    } catch (Exception e) { }
    return 0;
}

public String formatTime(int seconds) {
    if (seconds < 60) return seconds + "秒";
    if (seconds < 3600) return (seconds / 60) + "分";
    if (seconds < 86400) return (seconds / 3600) + "小时";
    return (seconds / 86400) + "天";
}

// ==================== 指令参数解析 ====================
class CommandArgs {
    String command;
    String subCommand;       // list / all / add / rm / clear / on / off
    ArrayList targets;        // UIN 列表
    String timeStr;           // 原始时间字符串 如 1h
    int timeSeconds;          // 解析后的秒数
    String reason;            // 理由
    boolean hasError;
    String errorMsg;
}

public CommandArgs parseCommand(String text, ArrayList atList) {
    CommandArgs args = new CommandArgs();
    args.targets = new ArrayList();
    
    if (text == null || !text.startsWith("/")) {
        args.hasError = true;
        args.errorMsg = "not_a_command";
        return args;
    }
    
    String[] parts = text.split("\\s+");
    if (parts.length == 0) {
        args.hasError = true;
        args.errorMsg = "empty_command";
        return args;
    }
    
    args.command = parts[0].toLowerCase();
    
    // 子命令集合
    Set subCommands = new HashSet();
    subCommands.add("list");
    subCommands.add("all");
    subCommands.add("add");
    subCommands.add("rm");
    subCommands.add("clear");
    subCommands.add("on");
    subCommands.add("off");
    subCommands.add("kick");
    subCommands.add("ban");
    subCommands.add("fban");
    subCommands.add("mute");
    subCommands.add("cancel");
    subCommands.add("status");
    int idx = 1;
    
    // 解析子命令
    if (parts.length > idx && subCommands.contains(parts[idx].toLowerCase())) {
        args.subCommand = parts[idx].toLowerCase();
        idx++;
    }
    
    // 解析 @ 用户（从 atList，与墨鸦一致）
    if (atList != null) {
        for (Object uin : atList) {
            if (uin != null) args.targets.add(uin.toString());
        }
    }
    
    // 跳过 @开头的文本标记（UIN已从atList获取）
    while (parts.length > idx && parts[idx].startsWith("@")) {
        idx++;
    }
    
    // 解析直接 UIN (连续的数字)
    while (parts.length > idx && parts[idx].matches("\\d{4,12}")) {
        if (!args.targets.contains(parts[idx])) args.targets.add(parts[idx]);
        idx++;
    }
    
    // 跳过 @开头的文本标记 以及 @显示名的剩余部分（可能含空格）
    while (parts.length > idx && (parts[idx].startsWith("@") || 
           (!parts[idx].matches("\\d{4,12}") && !parts[idx].matches("\\d+[smMhHdD]")))) {
        idx++;
    }
    
    // 解析时间 (30s / 30m / 2h / 1d)
    if (parts.length > idx && parts[idx].matches("\\d+[smMhHdD]")) {
        args.timeStr = parts[idx].toLowerCase();
        args.timeSeconds = parseTimeToSeconds(args.timeStr);
        idx++;
    }
    
    // 剩余部分为理由
    if (parts.length > idx) {
        StringBuilder reason = new StringBuilder();
        for (int i = idx; i < parts.length; i++) {
            if (reason.length() > 0) reason.append(" ");
            reason.append(parts[i]);
        }
        args.reason = reason.toString();
    }
    
    return args;
}

// ==================== 指令处理器 ====================
interface CommandHandler {
    void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId);
}

Map commandHandlers = new HashMap();

{
    // ========== /mute ==========
    commandHandlers.put("/mute", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "禁言失败: 权限不足"); return;
            }
            
            // /mute all
            if ("all".equals(args.subCommand)) {
                try {
                    shutUp(groupId, "", 1);
                    sendReplyFeedback(groupId, replyMsgId, "已开启全体禁言 执行人: [atUin="+ operatorUin + "]");
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "全体禁言失败: " + e.getMessage());
                }
                return;
            }
            
            // /mute list
            if ("list".equals(args.subCommand)) {
                showMuteList(groupId, replyMsgId);
                return;
            }
            
            // /mute @user <time> [reason]
            if (args.targets.isEmpty() || args.timeSeconds <= 0) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /mute @用户 <时间> [理由]\n时间格式: 15s(秒) / 30m(分) / 2h(时) / 1d(天)");
                return;
            }
            
            if (args.timeSeconds > 2592000) {
                sendReplyFeedback(groupId, replyMsgId, "禁言失败: 时间不能超过30天"); return;
            }
            
            for (Object targetObj : args.targets) {
                String target = (String) targetObj;
                
                if (isProtected(groupId, target, operatorUin)) {
                    if (isOwner(target)) sendReplyFeedback(groupId, replyMsgId, "禁言失败: 不能禁言宿主");
                    else sendReplyFeedback(groupId, replyMsgId, "禁言失败: " + getMemberDisplayName(groupId, target) + " 是代管，受保护");
                    continue;
                }
                if (!canOperate(groupId, operatorUin, target)) {
                    sendReplyFeedback(groupId, replyMsgId, "禁言失败: 权限不足"); continue;
                }
                
                try {
                    shutUp(groupId, target, args.timeSeconds);
                    String fb = "禁言成功! 用户: " + getMemberDisplayName(groupId, target) + "(" + target + ") 时长: " + args.timeStr;
                    if (args.reason != null && !args.reason.isEmpty()) fb += " 理由: " + args.reason;
                    fb += " 执行人: [atUin="+ operatorUin + "]";
                    sendReplyFeedback(groupId, replyMsgId, fb);
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "禁言失败: " + e.getMessage());
                }
            }
        }
    });
    
    // ========== /unmute ==========
    commandHandlers.put("/unmute", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "解禁失败: 权限不足"); return;
            }
            
            // /unmute all
            if ("all".equals(args.subCommand)) {
                try {
                    shutUp(groupId, "", 0);
                    sendReplyFeedback(groupId, replyMsgId, "已关闭全体禁言 执行人: [atUin="+ operatorUin + "]");
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "全体解禁失败: " + e.getMessage());
                }
                return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /unmute @用户 或 /unmute all");
                return;
            }
            
            for (Object targetObj : args.targets) {
                String target = (String) targetObj;
                try {
                    shutUp(groupId, target, 0);
                    sendReplyFeedback(groupId, replyMsgId, "已解禁 " + getMemberDisplayName(groupId, target) + "(" + target + ") 执行人: [atUin="+ operatorUin + "]");
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "解禁失败: " + e.getMessage());
                }
            }
        }
    });
    
    // ========== /kick ==========
    commandHandlers.put("/kick", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "踢出失败: 权限不足"); return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /kick @用户"); return;
            }
            
            for (Object targetObj : args.targets) {
                String target = (String) targetObj;
                
                if (isProtected(groupId, target, operatorUin)) {
                    if (isOwner(target)) sendReplyFeedback(groupId, replyMsgId, "踢出失败: 不能踢出宿主");
                    else sendReplyFeedback(groupId, replyMsgId, "踢出失败: " + getMemberDisplayName(groupId, target) + " 是代管，受保护");
                    continue;
                }
                if (!canOperate(groupId, operatorUin, target)) {
                    sendReplyFeedback(groupId, replyMsgId, "踢出失败: 权限不足"); continue;
                }
                
                try {
                    kickGroup(groupId, target, false);
                    String fb = "已踢出 " + getMemberDisplayName(groupId, target) + "(" + target + ")";
                    if (args.reason != null && !args.reason.isEmpty()) fb += " 理由: " + args.reason;
                    fb += " 执行人:[atUin=" + operatorUin + "]";
                    sendReplyFeedback(groupId, replyMsgId, fb);
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "踢出失败: " + e.getMessage());
                }
            }
        }
    });
    
    // ========== /ban (踢黑) ==========
    commandHandlers.put("/ban", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "踢黑失败: 权限不足"); return;
            }
            
            // /ban list
            if ("list".equals(args.subCommand)) {
                File f = getBanFile(groupId);
                ArrayList list = readLines(f);
                if (list.isEmpty()) {
                    sendReplyFeedback(groupId, replyMsgId, "本群黑名单为空"); return;
                }
                StringBuilder sb = new StringBuilder("本群黑名单(" + list.size() + "人):\n");
                for (int i = 0; i < list.size(); i++) {
                    String u = (String) list.get(i);
                    if (u == null) continue;
                    sb.append((i + 1) + ". " + getMemberDisplayName(groupId, u) + "(" + u + ")\n");
                }
                sendReplyFeedback(groupId, replyMsgId, sb.toString().trim());
                return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /ban @用户 或 /ban list"); return;
            }
            
            for (Object targetObj : args.targets) {
                String target = (String) targetObj;
                
                if (isProtected(groupId, target, operatorUin)) {
                    if (isOwner(target)) sendReplyFeedback(groupId, replyMsgId, "踢黑失败: 不能拉黑宿主");
                    else sendReplyFeedback(groupId, replyMsgId, "踢黑失败: " + getMemberDisplayName(groupId, target) + " 是代管，受保护");
                    continue;
                }
                if (!canOperate(groupId, operatorUin, target)) {
                    sendReplyFeedback(groupId, replyMsgId, "踢黑失败: 权限不足"); continue;
                }
                
                try {
                    kickGroup(groupId, target, true);
                    appendLine(getBanFile(groupId), target);
                    String fb = "踢黑成功! 用户: " + getMemberDisplayName(groupId, target) + "(" + target + ")";
                    if (args.reason != null && !args.reason.isEmpty()) fb += " 理由: " + args.reason;
                    fb += " 执行人:[atUin=" + operatorUin + "]";
                    sendReplyFeedback(groupId, replyMsgId, fb);
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "踢黑失败: " + e.getMessage());
                }
            }
        }
    });
    
    // ========== /admin ==========
    commandHandlers.put("/admin", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwner(operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "权限不足: 仅主人可管理代管"); return;
            }
            
            // /admin list
            if ("list".equals(args.subCommand)) {
                File f = getDelegateFile();
                ArrayList list = readLines(f);
                if (list.isEmpty()) {
                    sendReplyFeedback(groupId, replyMsgId, "当前没有代管"); return;
                }
                StringBuilder sb = new StringBuilder("代管列表(" + list.size() + "人):\n");
                for (int i = 0; i < list.size(); i++) {
                    String u = (String) list.get(i);
                    if (u == null) continue;
                    sb.append((i + 1) + ". " + getMemberDisplayName(groupId, u) + "(" + u + ")\n");
                }
                sendReplyFeedback(groupId, replyMsgId, sb.toString().trim());
                return;
            }
            
            // /admin clear
            if ("clear".equals(args.subCommand)) {
                overwriteLines(getDelegateFile(), new ArrayList());
                sendReplyFeedback(groupId, replyMsgId, "代管已全部清空 执行人: [atUin="+ operatorUin + "]");
                return;
            }
            
            // /admin add 或 /admin rm
            if (args.subCommand == null || (!"add".equals(args.subCommand) && !"rm".equals(args.subCommand))) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /admin add|rm|list|clear @用户");
                return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /admin " + args.subCommand + " @用户");
                return;
            }
            
            File f = getDelegateFile();
            for (Object targetObj : args.targets) {
                String target = (String) targetObj;
                if (isOwner(target)) {
                    sendReplyFeedback(groupId, replyMsgId, "不能操作宿主"); continue;
                }
                
                if ("add".equals(args.subCommand)) {
                    if (containsLine(f, target)) {
                        sendReplyFeedback(groupId, replyMsgId, getMemberDisplayName(groupId, target) + " 已经是代管了");
                    } else {
                        appendLine(f, target);
                        sendReplyFeedback(groupId, replyMsgId, "已添加代管: " + getMemberDisplayName(groupId, target) + "(" + target + ") 执行人: [atUin="+ operatorUin + "]");
                    }
                } else if ("rm".equals(args.subCommand)) {
                    if (containsLine(f, target)) {
                        removeLine(f, target);
                        sendReplyFeedback(groupId, replyMsgId, "已删除代管: " + getMemberDisplayName(groupId, target) + "(" + target + ") 执行人: [atUin="+ operatorUin + "]");
                    } else {
                        sendReplyFeedback(groupId, replyMsgId, getMemberDisplayName(groupId, target) + " 不是代管");
                    }
                }
            }
        }
    });
    
    // ========== /alliance ==========
    commandHandlers.put("/alliance", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwner(operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "权限不足: 仅主人可管理联盟"); return;
            }
            
            if ("add".equals(args.subCommand)) {
                addAllianceGroup(groupId);
                sendReplyFeedback(groupId, replyMsgId, "已添加联盟群: " + groupId + " 执行人: [atUin="+ operatorUin + "]");
            } else if ("rm".equals(args.subCommand)) {
                removeAllianceGroup(groupId);
                sendReplyFeedback(groupId, replyMsgId, "已取消联盟群: " + groupId + " 执行人: [atUin="+ operatorUin + "]");
            } else {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /alliance add|rm");
            }
        }
    });
    
    // ========== /fban ==========
    commandHandlers.put("/fban", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "联盟封禁失败: 权限不足"); return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /fban @用户 [理由]"); return;
            }
            
            String target = (String) args.targets.get(0);
            
            if (isProtected(groupId, target, operatorUin)) {
                if (isOwner(target)) sendReplyFeedback(groupId, replyMsgId, "联盟封禁失败: 不能封禁宿主");
                else sendReplyFeedback(groupId, replyMsgId, "联盟封禁失败: " + getMemberDisplayName(groupId, target) + " 是代管，受保护");
                return;
            }
            if (!canOperate(groupId, operatorUin, target)) {
                sendReplyFeedback(groupId, replyMsgId, "联盟封禁失败: 权限不足"); return;
            }
            if (isFbanUser(target)) {
                sendReplyFeedback(groupId, replyMsgId, "该用户已被封禁，请勿再次封禁"); return;
            }
            
            // 踢出当前群
            try {
                kickGroup(groupId, target, true);
            } catch (Exception e) {
                sendReplyFeedback(groupId, replyMsgId, "联盟封禁失败: " + e.getMessage());
                return;
            }
            
            // 记录封禁
            addFbanUser(target, args.reason);
            
            // 异步遍历所有联盟群踢出
            final String ftarget = target;
            new Thread(new Runnable() {
                public void run() {
                    ArrayList groups = getAllianceGroups();
                    for (Object g : groups) {
                        String gid = (String) g;
                        if (gid != null && !gid.equals(groupId)) {
                            try {
                                kickGroup(gid, ftarget, true);
                                Thread.sleep(200);
                            } catch (Exception e) { }
                        }
                    }
                }
            }).start();
            
            ArrayList allGroups = getAllianceGroups();
            String fb = "联盟封禁 用户: " + getMemberDisplayName(groupId, target) + "(" + target + ")";
            if (args.reason != null && !args.reason.isEmpty()) fb += " 理由: " + args.reason;
            fb += " 已通知 " + allGroups.size() + " 个联盟群 执行人: [atUin="+ operatorUin + "]";
            sendReplyFeedback(groupId, replyMsgId, fb);
        }
    });
    
    // ========== /unfban ==========
    commandHandlers.put("/unfban", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "解除封禁失败: 权限不足"); return;
            }
            
            if (args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /unfban @用户 [原因]"); return;
            }
            
            String target = (String) args.targets.get(0);
            
            if (!isFbanUser(target)) {
                sendReplyFeedback(groupId, replyMsgId, "该用户未被联盟封禁"); return;
            }
            if (!canOperate(groupId, operatorUin, target)) {
                sendReplyFeedback(groupId, replyMsgId, "解除封禁失败: 权限不足"); return;
            }
            
            removeFbanUser(target);
            String fb = "解除联盟封禁 用户: " + getMemberDisplayName(groupId, target) + "(" + target + ")";
            if (args.reason != null && !args.reason.isEmpty()) fb += " 原因: " + args.reason;
            fb += " 执行人: [atUin="+ operatorUin + "]";
            sendReplyFeedback(groupId, replyMsgId, fb);
        }
    });
    
    // ========== /set ==========
    commandHandlers.put("/set", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "权限不足"); return;
            }
            
            if (args.subCommand == null || args.targets.isEmpty()) {
                sendReplyFeedback(groupId, replyMsgId, "格式错误: /set <key> <value>\n支持的 key: mutetime(秒)");
                return;
            }
            
            String key = args.subCommand;
            String value = (String) args.targets.get(0);
            
            if ("mutetime".equals(key)) {
                try {
                    int seconds = Integer.parseInt(value);
                    if (seconds <= 0 || seconds > 2592000) {
                        sendReplyFeedback(groupId, replyMsgId, "时间需在 1~2592000 秒之间"); return;
                    }
                    setGlobalConfig("default_mute_time", String.valueOf(seconds));
                    sendReplyFeedback(groupId, replyMsgId, "默认禁言时间已设为 " + formatTime(seconds) + " 执行人: [atUin="+ operatorUin + "]");
                } catch (Exception e) {
                    sendReplyFeedback(groupId, replyMsgId, "格式错误: /set mutetime <秒数>");
                }
            } else {
                sendReplyFeedback(groupId, replyMsgId, "未知配置: " + key);
            }
        }
    });
    
    // ========== /status ==========
    commandHandlers.put("/status", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "权限不足"); return;
            }
            
            String muteOnAt = isEnabled(groupId, "muteonat") ? "开" : "关";
            String autoBan = isEnabled(groupId, "autoban") ? "开" : "关";
            String selfTitle = isEnabled(groupId, "selftitle") ? "开" : "关";
            String unmuteDel = isEnabled(groupId, "unmutedelegate") ? "开" : "关";
            String defaultMuteTime = getGlobalConfig("default_mute_time", "2592000");
            
            String fb = "群状态:\n" +
                "艾特禁言: " + muteOnAt + " (默认" + formatTime(Integer.parseInt(defaultMuteTime)) + ")\n" +
                "退群拉黑: " + autoBan + "\n" +
                "自助头衔: " + selfTitle + "\n" +
                "自动解禁代管: " + unmuteDel + "\n" +
                "联盟群: " + (isAllianceGroup(groupId) ? "是" : "否");
            sendReplyFeedback(groupId, replyMsgId, fb);
        }
    });
    
    // ========== /on ==========
    commandHandlers.put("/on", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwner(operatorUin)) return;
            if (isSessionEnabled(groupId)) {
                sendReplyFeedback(groupId, replyMsgId, "群管已启用，无需重复开启");
                return;
            }
            appendLine(getEnabledFile(), groupId);
            sendReplyFeedback(groupId, replyMsgId, "群管已启用 执行人:[atUin=" + operatorUin + "]");
        }
    });

    // ========== /off ==========
    commandHandlers.put("/off", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            if (!isOwner(operatorUin)) return;
            if (!isSessionEnabled(groupId)) {
                sendReplyFeedback(groupId, replyMsgId, "群管未启用，无需重复禁用");
                return;
            }
            removeLine(getEnabledFile(), groupId);
            sendReplyFeedback(groupId, replyMsgId, "群管已禁用 执行人:[atUin=" + operatorUin + "]");
        }
    });
    // ========== /vote ==========
commandHandlers.put("/vote", new CommandHandler() {
    public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
        // /vote cancel
        if ("cancel".equals(args.reason) || (args.subCommand != null && "cancel".equals(args.subCommand))) {
            VoteSession v = (VoteSession) activeVotes.get(groupId);
            if (v == null || !v.active) {
                sendReplyFeedback(groupId, replyMsgId, "当前没有进行中的投票"); return;
            }
            if (!operatorUin.equals(v.initiatorUin) && !isOwnerOrDelegate(groupId, operatorUin)) {
                sendReplyFeedback(groupId, replyMsgId, "仅发起人或管理员可取消投票"); return;
            }
            v.active = false;
            activeVotes.remove(groupId);
            sendReplyFeedback(groupId, replyMsgId, "投票已取消 执行人:[atUin=" + operatorUin + "]");
            return;
        }
        
        // /vote status
        if ("status".equals(args.reason) || (args.subCommand != null && "status".equals(args.subCommand))) {
            VoteSession v = (VoteSession) activeVotes.get(groupId);
            if (v == null || !v.active) {
                sendReplyFeedback(groupId, replyMsgId, "当前没有进行中的投票"); return;
            }
            String typeZH = "踢出";
            if ("ban".equals(v.type)) typeZH = "踢黑";
            else if ("fban".equals(v.type)) typeZH = "联盟封禁";
            else if ("mute".equals(v.type)) typeZH = "禁言";
            
            int required = getVoteRequired();
            int current = v.yesVoters.size();
            long elapsed = (System.currentTimeMillis() - v.startTime) / 1000;
            long remaining = getVoteTimeout() - elapsed;
            if (remaining < 0) remaining = 0;
            
            String fb = "[投票] " + typeZH + "投票\n" +
                "发起人: [atUin=" + v.initiatorUin + "] (" + v.initiatorName + ")\n" +
                "目标: " + v.targetName + "(" + v.targetUin + ")\n" +
                "进度: [" + current + "/" + required + "] " + formatYesVoters(v, groupId) + "\n" +
                "剩余: " + remaining + "秒";
            sendReplyFeedback(groupId, replyMsgId, fb);
            return;
        }
        
        // 发起投票: /vote <type> @someone [time] [reason]
        if (args.targets.isEmpty()) {
            sendReplyFeedback(groupId, replyMsgId, "格式错误: /vote kick|ban|fban @用户 [理由]\n/vote mute @用户 <时间> [理由]");
            return;
        }
        
        // 投票类型从 args.subCommand 或 args.reason 的第一个词获取
        String voteType = null;
        if (args.subCommand != null && (args.subCommand.equals("kick") || args.subCommand.equals("ban") || 
            args.subCommand.equals("fban") || args.subCommand.equals("mute"))) {
            voteType = args.subCommand;
        }
        // 如果子命令不是投票类型，检查 reason 第一个词
        if (voteType == null && args.reason != null) {
            String firstWord = args.reason.split("\\s+")[0].toLowerCase();
            if (firstWord.equals("kick") || firstWord.equals("ban") || 
                firstWord.equals("fban") || firstWord.equals("mute")) {
                voteType = firstWord;
                // 从 reason 中去掉类型词，但如果是 mute 还要去掉时间
            }
        }
        
        if (voteType == null) {
            sendReplyFeedback(groupId, replyMsgId, "未知投票类型\n用法: /vote kick|ban|fban @用户 [理由]\n/vote mute @用户 <时间> [理由]");
            return;
        }
        
        // 同群只能有一个活跃投票
        VoteSession oldVote = (VoteSession) activeVotes.get(groupId);
        if (oldVote != null && oldVote.active) {
            oldVote.active = false;
        }
        
        String target = (String) args.targets.get(0);
        
        // 受保护检查
        if (isProtected(groupId, target, operatorUin)) {
            if (isOwner(target)) sendReplyFeedback(groupId, replyMsgId, "投票失败: 不能对宿主发起投票");
            else sendReplyFeedback(groupId, replyMsgId, "投票失败: " + getMemberDisplayName(groupId, target) + " 是代管，受保护");
            return;
        }
        
        // mute 必须有时间
        if ("mute".equals(voteType) && args.timeSeconds <= 0) {
            sendReplyFeedback(groupId, replyMsgId, "格式错误: /vote mute @用户 <时间>\n时间格式: 15s(秒) / 30m(分) / 2h(时) / 1d(天)");
            return;
        }
        if ("mute".equals(voteType) && args.timeSeconds > 2592000) {
            sendReplyFeedback(groupId, replyMsgId, "投票失败: 禁言时间不能超过30天"); return;
        }
        
        // 创建投票
        VoteSession v = new VoteSession();
        v.groupId = groupId;
        v.type = voteType;
        v.targetUin = target;
        v.targetName = getMemberDisplayName(groupId, target);
        v.timeStr = args.timeStr;
        v.timeSeconds = args.timeSeconds;
        v.initiatorUin = operatorUin;
        v.initiatorName = getMemberDisplayName(groupId, operatorUin);
        v.startTime = System.currentTimeMillis();
        v.yesVoters.add(operatorUin);  // 发起人自动算一票
        
        // reason: 从 args.reason 中去掉类型词和时间词
        if (args.reason != null) {
            String r = args.reason;
            // 去掉开头的类型词
            if (r.toLowerCase().startsWith(voteType)) r = r.substring(voteType.length()).trim();
            // mute 时去掉时间词
            if ("mute".equals(voteType) && args.timeStr != null && !args.timeStr.isEmpty()) {
                if (r.startsWith(args.timeStr)) r = r.substring(args.timeStr.length()).trim();
            }
            if (!r.isEmpty()) v.reason = r;
        }
        
        activeVotes.put(groupId, v);
        
        String typeZH = "踢出";
        if ("ban".equals(v.type)) typeZH = "踢黑";
        else if ("fban".equals(v.type)) typeZH = "联盟封禁";
        else if ("mute".equals(v.type)) typeZH = "禁言";
        
        int required = getVoteRequired();
        int timeout = getVoteTimeout();
        
        String fb = "[投票] [atUin=" + operatorUin + "] 发起" + typeZH + "投票\n" +
            "目标: " + v.targetName + "(" + v.targetUin + ")";
        if ("mute".equals(v.type)) fb += "  时长: " + v.timeStr;
        if (v.reason != null && !v.reason.isEmpty()) fb += "\n理由: " + v.reason;
        fb += "\n回复 /yes 投票 (需" + required + "票, " + timeout + "秒)\n" +
            "进度: [1/" + required + "] [atUin=" + operatorUin + "]";
        sendReplyFeedback(groupId, replyMsgId, fb);
        
        // 超时线程
        final String fGroupId = groupId;
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(timeout * 1000); } catch (Exception e) { }
                VoteSession v = (VoteSession) activeVotes.get(fGroupId);
                if (v != null && v.active) {
                    v.active = false;
                    activeVotes.remove(fGroupId);
                    int current = v.yesVoters.size();
                    sendFeedback(fGroupId, "❌ 投票未通过 [" + current + "/" + required + ", 已超时]\n" +
                        "目标: " + v.targetName + "(" + v.targetUin + ")");
                }
            }
        }).start();
    }
});
    
    // ========== /help ==========
    commandHandlers.put("/help", new CommandHandler() {
        public void handle(String groupId, String operatorUin, CommandArgs args, long replyMsgId) {
            boolean isOwnerOrDel = isOwnerOrDelegate(groupId, operatorUin);
            StringBuilder sb = new StringBuilder();
            sb.append("群管指令:\n");
            sb.append("/mute @用户 <时间> [理由] - 禁言\n");
            sb.append("/mute all - 全体禁言\n");
            sb.append("/mute list - 查看禁言列表\n");
            sb.append("/unmute @用户 - 解禁\n");
            sb.append("/unmute all - 全体解禁\n");
            sb.append("/kick @用户 - 踢出\n");
            sb.append("/ban @用户 - 踢黑\n");
            sb.append("/ban list - 查看黑名单\n");
            sb.append("/vote kick|ban|fban @用户 [理由] - 发起投票\n");
            sb.append("/vote mute @用户 <时间> [理由] - 发起禁言投票\n");
            sb.append("/vote cancel - 取消投票\n");
            sb.append("/vote status - 查看投票\n");
            sb.append("/vote set count|timeout <值> - 投票设置\n");
            sb.append("/yes - 投同意票\n");
            if (isOwnerOrDel) {
                sb.append("/admin add|rm @用户 - 管理代管\n");
                sb.append("/admin list - 查看代管\n");
                sb.append("/alliance add|rm - 管理联盟群\n");
                sb.append("/fban @用户 [理由] - 联盟封禁\n");
                sb.append("/unfban @用户 [原因] - 解除联盟封禁\n");
                sb.append("/toggle muteonat|autoban|selftitle|unmutedelegate on|off\n");
                sb.append("/set mutetime <秒> - 默认禁言时间\n");
                sb.append("/status - 查看状态\n");
            }
            sb.append("/on - 启用本群群管\n");
            sb.append("/off - 禁用本群群管\n");
            sb.append("/help - 显示此帮助\n\n");
            sb.append("时间格式: 15s(秒) / 30m(分) / 2h(时) / 1d(天)\n");
            sb.append("支持 @用户 或直接输入 UIN");
            sendReplyFeedback(groupId, replyMsgId, sb.toString());
        }
    });
}

// ==================== 投票系统 ====================
class VoteSession {
    String groupId;
    String type;           // kick / ban / fban / mute
    String targetUin;
    String targetName;
    String reason;
    String timeStr;        // only for mute
    int timeSeconds;       // only for mute
    String initiatorUin;
    String initiatorName;
    Set yesVoters = new HashSet();
    long startTime;
    boolean active = true;
}

Map activeVotes = new HashMap();  // groupId -> VoteSession

public int getVoteRequired() {
    try { return Integer.parseInt(getGlobalConfig("vote_count", "3")); } catch (Exception e) { return 3; }
}

public int getVoteTimeout() {
    try { return Integer.parseInt(getGlobalConfig("vote_timeout", "60")); } catch (Exception e) { return 60; }
}

public String formatYesVoters(VoteSession v, String groupId) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (Object uin : v.yesVoters) {
        if (i > 0) sb.append(" ");
        sb.append("[atUin=" + uin + "]");
        i++;
    }
    return sb.toString();
}

public void handleVoteYes(String groupId, String senderUin) {
    VoteSession v = (VoteSession) activeVotes.get(groupId);
    if (v == null || !v.active) return;
    
    // 不能投给自己
    if (senderUin.equals(v.targetUin)) return;
    
    // 去重
    if (v.yesVoters.contains(senderUin)) return;
    
    v.yesVoters.add(senderUin);
    int required = getVoteRequired();
    int current = v.yesVoters.size();
    
    if (current >= required) {
        // 投票通过，执行操作
        v.active = false;
        activeVotes.remove(groupId);
        executeVoteAction(v, groupId);
    } else {
        // 更新进度
        String voters = formatYesVoters(v, groupId);
        sendFeedback(groupId, "[投票] 进度: [" + current + "/" + required + "] " + voters);
    }
}

public void executeVoteAction(VoteSession v, String groupId) {
    String fb = "";
    try {
        if ("mute".equals(v.type)) {
            shutUp(groupId, v.targetUin, v.timeSeconds);
            fb = "禁言成功! 用户: " + v.targetName + "(" + v.targetUin + ") 时长: " + v.timeStr;
            if (v.reason != null && !v.reason.isEmpty()) fb += " 理由: " + v.reason;
        } else if ("kick".equals(v.type)) {
            kickGroup(groupId, v.targetUin, false);
            fb = "已踢出 " + v.targetName + "(" + v.targetUin + ")";
            if (v.reason != null && !v.reason.isEmpty()) fb += " 理由: " + v.reason;
        } else if ("ban".equals(v.type)) {
            kickGroup(groupId, v.targetUin, true);
            appendLine(getBanFile(groupId), v.targetUin);
            fb = "踢黑成功! 用户: " + v.targetName + "(" + v.targetUin + ")";
            if (v.reason != null && !v.reason.isEmpty()) fb += " 理由: " + v.reason;
        } else if ("fban".equals(v.type)) {
            kickGroup(groupId, v.targetUin, true);
            addFbanUser(v.targetUin, v.reason);
            
            final String ftarget = v.targetUin;
            new Thread(new Runnable() {
                public void run() {
                    ArrayList groups = getAllianceGroups();
                    for (Object g : groups) {
                        String gid = (String) g;
                        if (gid != null && !gid.equals(groupId)) {
                            try { kickGroup(gid, ftarget, true); Thread.sleep(200); } catch (Exception e) { }
                        }
                    }
                }
            }).start();
            
            ArrayList allGroups = getAllianceGroups();
            fb = "联盟封禁 用户: " + v.targetName + "(" + v.targetUin + ")";
            if (v.reason != null && !v.reason.isEmpty()) fb += " 理由: " + v.reason;
            fb += " 已通知 " + allGroups.size() + " 个联盟群";
        }
        fb += "\n投票结果: " + v.yesVoters.size() + "/" + getVoteRequired() + " 通过";
        fb += "\n投票人: " + formatYesVoters(v, groupId);
        sendFeedback(groupId, "✅ 投票通过\n" + fb);
    } catch (Exception e) {
        sendFeedback(groupId, "投票执行失败: " + e.getMessage());
    }
}

// ==================== 显示禁言列表 ====================
public void showMuteList(String groupId, long replyMsgId) {
    try {
        ArrayList list = getProhibitList(groupId);
        if (list == null || list.isEmpty()) {
            sendReplyFeedback(groupId, replyMsgId, "当前没有人被禁言"); return;
        }
        
        StringBuilder sb = new StringBuilder("禁言列表(" + list.size() + "人):\n");
        ArrayList safeList = new ArrayList(list);
        for (int i = 0; i < safeList.size(); i++) {
            Object item = safeList.get(i);
            try {
                String uin = ((ForbidInfo) item).user;
                String name = ((ForbidInfo) item).userName;
                if (uin != null) {
                    sb.append((i + 1) + ". " + (name != null ? name : uin) + "(" + uin + ")\n");
                }
            } catch (Exception e) { }
        }
        sb.append("输入序号可快速操作: 解禁/踢/踢黑");
        sendReplyFeedback(groupId, replyMsgId, sb.toString().trim());
    } catch (Exception e) {
        sendReplyFeedback(groupId, replyMsgId, "获取禁言列表失败: " + e.getMessage());
    }
}

// ==================== 监听入群/退群 ====================
public void joinGroup(String groupId, String userUin) {
    try {
        if (userUin == null || userUin.equals(myUin)) return;
        
        // 退群拉黑检测
        if (isEnabled(groupId, "autoban")) {
            if (containsLine(getBanFile(groupId), userUin)) {
                kickGroup(groupId, userUin, true);
            }
        }
        
        // 联盟封禁检测
        if (isAllianceGroup(groupId) && isFbanUser(userUin)) {
            kickGroup(groupId, userUin, true);
        }
    } catch (Exception e) { log("joinGroupError: " + e); }
}

public void quitGroup(String groupId, String userUin) {
    try {
        if (userUin == null || userUin.equals(myUin)) return;
        
        if (isEnabled(groupId, "autoban")) {
            if (!containsLine(getBanFile(groupId), userUin)) {
                appendLine(getBanFile(groupId), userUin);
            }
        }
    } catch (Exception e) { log("quitGroupError: " + e); }
}

// ==================== 禁言事件监听（自动解禁代管） ====================
public void onForbiddenEvent(String groupId, String userUin, String opUin, long time) {
    try {
        if (!isEnabled(groupId, "unmutedelegate")) return;
        if (time <= 0) return;
        if (isDelegate(groupId, userUin)) {
            shutUp(groupId, userUin, 0);
        }
    } catch (Exception e) { log("onForbiddenError: " + e); }
}

// ==================== 主入口 ====================
public void onMsg(Object msg) {
    if (msg == null) return;
    
    String text = msg.msg;
    if (text == null) return;
    
    String senderUin = String.valueOf(msg.userUin);
    
    // 系统消息保护
    if (senderUin == null || senderUin.isEmpty() || senderUin.equals("0") || senderUin.equals("null")) return;
    
    // 只处理群聊
    if (msg.type != 2) return;
    
    String groupId = String.valueOf(msg.peerUin);
    final long replyMsgId = msg.msgId;
    String trimmed = text.trim();
    
    // 未启用的会话，仅 /on 可通行
    if (!isSessionEnabled(groupId) && !trimmed.startsWith("/on")) {
        return;
    }
    
    // ===== 指令路由 =====
    if (trimmed.startsWith("/")) {
        // 特殊处理 /toggle
        if (trimmed.startsWith("/toggle ")) {
            handleToggle(groupId, senderUin, trimmed, replyMsgId);
            return;
        }
        
        // 特殊处理 /vote set
        if (trimmed.startsWith("/vote set ")) {
            handleVoteSet(groupId, senderUin, trimmed, replyMsgId);
            return;
        }
        
        ArrayList atList = (msg.atList != null) ? new ArrayList(msg.atList) : new ArrayList();
        CommandArgs args = parseCommand(trimmed, atList);
        
        if (args.hasError) return;
        
        // /yes 投票
        if (trimmed.equalsIgnoreCase("/yes") && activeVotes.containsKey(groupId)) {
            handleVoteYes(groupId, senderUin);
            return;
       }
        
        CommandHandler handler = (CommandHandler) commandHandlers.get(args.command);
        // 未知指令 → 直接忽略
        if (handler == null) return;
        
        handler.handle(groupId, senderUin, args, replyMsgId);
        return;
    }
    
    // ===== 非指令 — 快速操作 =====
    // 艾特禁言
    if (isEnabled(groupId, "muteonat") && atMe(msg) && isOwnerOrDelegate(groupId, senderUin)) {
        String defaultTime = getGlobalConfig("default_mute_time", "2592000");
        try {
            shutUp(groupId, senderUin, Integer.parseInt(defaultTime));
        } catch (Exception e) { }
        return;
    }
    
    // 我要头衔
    if (trimmed.startsWith("我要头衔") && isEnabled(groupId, "selftitle")) {
        try {
            String title = trimmed.substring(4).trim();
            if (!title.isEmpty()) {
                setGroupMemberTitle(groupId, senderUin, title);
            }
        } catch (Exception e) { }
        return;
    }
}

public boolean atMe(Object msg) {
    if (msg == null || msg.atList == null || msg.atList.size() == 0) return false;
    for (Object uin : new ArrayList(msg.atList)) {
        if (uin != null && uin.toString().equals(myUin)) return true;
    }
    return false;
}

public void handleVoteSet(String groupId, String operatorUin, String text, long replyMsgId) {
    if (!isOwnerOrDelegate(groupId, operatorUin)) {
        sendReplyFeedback(groupId, replyMsgId, "权限不足"); return;
    }
    
    String[] parts = text.split("\\s+");
    if (parts.length < 4) {
        sendReplyFeedback(groupId, replyMsgId, "格式错误: /vote set count <数量> 或 /vote set timeout <秒>");
        return;
    }
    
    String key = parts[2].toLowerCase();
    String value = parts[3];
    
    if ("count".equals(key)) {
        try {
            int count = Integer.parseInt(value);
            if (count < 2 || count > 50) { sendReplyFeedback(groupId, replyMsgId, "票数需在 2~50 之间"); return; }
            setGlobalConfig("vote_count", String.valueOf(count));
            sendReplyFeedback(groupId, replyMsgId, "投票所需票数已设为 " + count + " 执行人:[atUin=" + operatorUin + "]");
        } catch (Exception e) {
            sendReplyFeedback(groupId, replyMsgId, "格式错误: /vote set count <数字>");
        }
    } else if ("timeout".equals(key)) {
        try {
            int timeout = Integer.parseInt(value);
            if (timeout < 10 || timeout > 3600) { sendReplyFeedback(groupId, replyMsgId, "超时需在 10~3600 秒之间"); return; }
            setGlobalConfig("vote_timeout", String.valueOf(timeout));
            sendReplyFeedback(groupId, replyMsgId, "投票超时已设为 " + timeout + "秒 执行人:[atUin=" + operatorUin + "]");
        } catch (Exception e) {
            sendReplyFeedback(groupId, replyMsgId, "格式错误: /vote set timeout <秒数>");
        }
    } else {
        sendReplyFeedback(groupId, replyMsgId, "未知配置: " + key + "\n可用: count timeout");
    }
}

// ==================== /toggle 特殊处理（因为 atList 可能吃掉 feature 名） ====================
public void handleToggle(String groupId, String operatorUin, String text, long replyMsgId) {
    if (!isOwnerOrDelegate(groupId, operatorUin)) {
        sendReplyFeedback(groupId, replyMsgId, "权限不足"); return;
    }
    
    String[] parts = text.split("\\s+");
    if (parts.length < 3) {
        sendReplyFeedback(groupId, replyMsgId, "格式错误: /toggle <功能名> on|off\n功能: muteonat autoban selftitle unmutedelegate");
        return;
    }
    
    String feature = parts[1].toLowerCase();
    String state = parts[2].toLowerCase();
    
    if (!"on".equals(state) && !"off".equals(state)) {
        sendReplyFeedback(groupId, replyMsgId, "格式错误: /toggle " + feature + " on|off");
        return;
    }
    
    Set validFeatures = new HashSet();
    validFeatures.add("muteonat");
    validFeatures.add("autoban");
    validFeatures.add("selftitle");
    validFeatures.add("unmutedelegate");
    
    if (!validFeatures.contains(feature)) {
        sendReplyFeedback(groupId, replyMsgId, "未知功能: " + feature + "\n可用: muteonat autoban selftitle unmutedelegate");
        return;
    }
    
    String value = "on".equals(state) ? "on" : null;
    String nameZh = "";
    
    if ("muteonat".equals(feature)) { nameZh = "艾特禁言"; setGroupConfig(groupId, "muteonat", value); }
    else if ("autoban".equals(feature)) { nameZh = "退群拉黑"; setGroupConfig(groupId, "autoban", value); }
    else if ("selftitle".equals(feature)) { nameZh = "自助头衔"; setGroupConfig(groupId, "selftitle", value); }
    else if ("unmutedelegate".equals(feature)) { nameZh = "自动解禁代管"; setGroupConfig(groupId, "unmutedelegate", value); }
    
    sendReplyFeedback(groupId, replyMsgId, nameZh + "已" + ("on".equals(state) ? "开启" : "关闭") + " 执行人: [atUin="+ operatorUin + "]");
}

// ==================== 生命周期 ====================
public void onDestroy() {
    // 清理资源（无共享连接需要清理）
}

/*
*  MIT License
*
*  Regent-DREX v1.2
*
*  Copyright (c) 2026 YiJieqwq异界
*
*  Permission is hereby granted, free of charge, to any person obtaining a copy
*  of this software and associated documentation files (the "Software"), to deal
*  in the Software without restriction, including without limitation the rights
*  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*  copies of the Software, and to permit persons to whom the Software is
*  furnished to do so, subject to the following conditions:

*  The above copyright notice and this permission notice shall be included in all
*  copies or substantial portions of the Software.

*  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
*  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
*  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
*  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
*  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
*  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
