package com.zsr.log.bolt;

import com.zsr.log.domain.Message;
import com.zsr.log.domain.Record;
import com.zsr.log.utils.MonitorHandler;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.lang.reflect.InvocationTargetException;

public class SendEmailBolt extends BaseBasicBolt {
    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        int appId = input.getIntegerByField("appId");
        Message msg = (Message) input.getValueByField("msg");
        // 进行邮件通知
        MonitorHandler.notify(appId, msg);
        Record record = new Record();
        try {
            //将msg数据映射到record
            BeanUtils.copyProperties(record,msg);
            collector.emit(new Values(record));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("record"));
    }
}
