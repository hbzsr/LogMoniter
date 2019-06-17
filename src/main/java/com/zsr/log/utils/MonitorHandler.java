package com.zsr.log.utils;


import com.zsr.log.dao.LogMonitorDao;
import com.zsr.log.domain.*;
import com.zsr.log.mail.MailInfo;
import com.zsr.log.mail.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MonitorHandler {
    // 存储用户信息
    private static List<User> userList;
    // 存储应用程序信息
    private static List<App> appList;

    // 存储规则信息   key=appId   value=Rule
    private static Map<String, List<Rule>> ruleMap;
    // 存储appId对应的所有负责人  key=appId   value=User
    private static Map<String, List<User>> userMap;
    private static boolean reloaded = false;

    static {
        load();
    }

    //从数据库中将数据加载出来
    private static void load() {
        LogMonitorDao dao = new LogMonitorDao();
        userList = dao.getUserList();
        appList = dao.getAppList();
        ruleMap = rule2Map(dao.getRuleList());
        userMap = user2Map();
    }

    /**
     * rules 对应关系是 key：appId ； value：rules；
     * 因为可能是 一个app下有多条对应的规则，需要对之前从数据库中查询出来的结果做分组。
     * 或者直接从数据库中分组查询。《---优化---》
     *
     * @param rules
     * @return
     */
    private static Map<String, List<Rule>> rule2Map(List<Rule> rules) {
        Map<String, List<Rule>> map = null;
        if (!CollectionUtils.isEmpty(rules)) {
            map = new HashMap<>();
            for (Rule rule : rules) {
                // 获取这个规则id对应的规则信息
                List<Rule> ruleListByAppId = map.get(rule.getAppId() + "");
                if (ruleListByAppId == null) {
                    ruleListByAppId = new ArrayList<>();
                }
                ruleListByAppId.add(rule);
                map.put(rule.getAppId() + "", ruleListByAppId);
            }
        }
        return map;
    }

    /**
     * app id和负责人之间关系也是 1对多，也是需要做上面的操作
     * 这个也是优化的地方
     */
    private static Map<String, List<User>> user2Map() {
        Map<String, List<User>> map = new HashMap<>();
        for (App app : appList) {
            String userIds = app.getUserId();
            List<User> userListInApp = map.get(app.getId() + "");
            if (userListInApp == null) {
                userListInApp = new ArrayList<>();
            }
            String[] userIdArr = userIds.split(",");
            for (String userId : userIdArr) {
                userListInApp.add(queryUserByUserId(userId));
            }
            map.put(app.getId() + "", userListInApp);
        }
        return map;
    }

    /**
     * 根据负责人id获取负责人信息
     *
     * @param userId
     * @return
     */
    private static User queryUserByUserId(String userId) {
        for (User user : userList) {
            if (user.getId() == Integer.parseInt(userId))
                return user;
        }

        return null;
    }

    /**
     * 用来校验数据
     * 数据合法返回 message
     * 不合法返回null
     *
     * @param line
     * @return
     */
    public static Message parse(String line) {
        // 切分数据
        String[] messageArr = line.split("\\$\\$\\$\\$\\$");
        //数据不合法的直接返回null
        if (messageArr.length != 2 || StringUtils.isBlank(messageArr[0]) || StringUtils.isBlank(messageArr[1])) {
            return null;
        }
        Message msg = null;
        int appId = Integer.parseInt(messageArr[0].trim());
        if (appIdIsValid(appId)) {
            msg = new Message();
            msg.setAppId(appId);
            msg.setLine(messageArr[1]);
        }
        return msg;
    }

    /**
     * 校验appid是否授权
     * 正常应该是appid实时查询出来的结果，--》》》》优化
     *
     * @return
     */
    private static boolean appIdIsValid(int appId) {
        for (App app : appList) {
            if (app.getId() == appId) {
                return true;
            }
        }
        return false;
    }

    public static boolean trigger(Message msg) {
        if (ruleMap == null) {
            load();
        }
        //根据appid获取到所有的rules
        List<Rule> rules = ruleMap.get(msg.getAppId()+"");
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }
        for (Rule rule : rules) {
            if (msg.getLine().contains(rule.getKeyword())) {
                msg.setRuleId(rule.getId());
                msg.setKeyword(rule.getKeyword());
                return true;
            }
        }
        return false;
    }

    public static void scheduleLoad() {
        String date = DateUtils.getDateTime();

        // 获取当前时间的分钟数  yyyy-MM-dd HH:mm:ss
        int nowMinute = Integer.parseInt(date.split(":")[1]);
        if (nowMinute % 10 == 0) {
            reLoadData();
        } else {
            reloaded = true;
        }
    }

    private static void reLoadData() {
        if (reloaded) {
            load();//这里重新从数据库里获取一遍数据,storm是要7*24小时一直工作，这里使用10分钟更新一次也合乎情理。
            reloaded = false;
        }
    }


    /**
     * 进行邮件通知
     *
     * @param appId
     * @param message
     */
    public static void notify(int appId, Message message) {
        // 根据appId获取负责人信息
        List<User> users = userMap.get(appId + "");
        if (senMail(appId, users, message)) {
            message.setIsEmail(1);
        }
    }

    /**
     * 发送邮件
     */
    private static boolean senMail(int appId, List<User> users, Message message) {
        // 存储负责人邮箱地址
        List<String> receiver = new ArrayList<>();

        for (User user : users) {
            receiver.add(user.getEmail());
        }

        // 找到应用程序名称并封装到Message中
        for (App app : appList) {
            if (app.getId() == appId) {
                message.setAppName(app.getName());
            }
        }

        if (receiver.size() >= 1) {
            String date = DateUtils.getDateTime();

            // 拼接发送邮件内容
            String content = "应用程序[" + message.getAppName() + "]在" + date +
                    "触发了规则，触发信息为：[" + message.getLine() + "]";

            MailInfo mailInfo = new MailInfo("logmonitor", content, receiver, null);

            return MessageSender.sendMail(mailInfo);
        }

        return false;
    }

    /**
     * 将触发规则的记录保存到数据库 log_monitor_rule_record
     *
     * @param record
     */
    public static void save(Record record) {
        new LogMonitorDao().saveRecord(record);
    }
}
