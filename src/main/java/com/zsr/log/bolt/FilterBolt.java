package com.zsr.log.bolt;

import com.zsr.log.domain.Message;
import com.zsr.log.utils.MonitorHandler;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

public class FilterBolt extends BaseBasicBolt {

    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        byte[] value = (byte[]) input.getValue(0);
        String line = new String(value);
        //解析数据，校验数据是否合法
        Message msg = MonitorHandler.parse(line);
        if (msg == null) {
            return;
        }
        //匹配规则判断，如果没有规则匹配，就直接过去
        if (MonitorHandler.trigger(msg)) {
            System.out.println(">>>>>>>>>>");
            collector.emit(new Values(msg.getAppId(), msg));
        }
        //更新匹配规则
        MonitorHandler.scheduleLoad();

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("appId", "msg"));
    }
}
