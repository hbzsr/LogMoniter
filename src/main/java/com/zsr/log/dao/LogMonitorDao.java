package com.zsr.log.dao;

import com.zsr.log.domain.App;
import com.zsr.log.domain.Record;
import com.zsr.log.domain.Rule;
import com.zsr.log.domain.User;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class LogMonitorDao {
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        LogMonitorDao dao = new LogMonitorDao();
        System.out.println(dao.getUserList());
        System.out.println(dao.getAppList());
        System.out.println(dao.getRuleList());
    }

    public LogMonitorDao() {
        jdbcTemplate = new JdbcTemplate(DataSourceUtil.getDataSource());
    }

    /**
     * 查询用户（负责人）
     */
    public List<User> getUserList() {
        String sql = "select id, name, mobile, email, isValid  from log_monitor_user where isValid = 1";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }

    /**
     * 查询规则信息
     */
    public List<Rule> getRuleList() {
        String sql = "select id, name, keyword, isValid, appId from log_monitor_rule where isValid = 1";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Rule.class));
    }
    /**
     * 分组查询规则信息
     * zhege buhui
     * appId分组
     */

    /**
     * 查询应用程序的信息
     */
    public List<App> getAppList() {
        String sql = "select id, name, isOnline, typeId, userId from log_monitor_app where isOnline = 1";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(App.class));
    }
    /**
     * 更新触发规则的信息
     */
    public void saveRecord(Record record){
        String sql = "insert into log_monitor_rule_record " +
                "(appId, ruleId, isEmail, isPhone, isClose, noticeInfo, updateDate) " +
                "values(?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, record.getAppId(), record.getRuleId(),
                record.getIsEmail(), record.getIsPhone(), 0, record.getLine(), new Date());

    }
}
