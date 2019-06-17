package com.zsr.log;

import com.zsr.log.bolt.FilterBolt;
import com.zsr.log.bolt.SaveRecordBolt;
import com.zsr.log.bolt.SendEmailBolt;
import org.apache.commons.lang.ArrayUtils;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.KafkaSpout;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.topology.TopologyBuilder;

/**
 * 从kafka消费数据，获取到日志数据进行处理，包括异常数据向指定负责人发送邮件，存储信息等操作。
 */
public class LogMonitorTopology {
    public static void main(String[] args) throws InvalidTopologyException, AuthorizationException, AlreadyAliveException {
        TopologyBuilder builder = new TopologyBuilder();
        //kafka的topic
        String topic = "logmonitor";
        ZkHosts hosts = new ZkHosts("bdnode1:2181,bdnode2:2181,bdnode3:2181");
        //将spout作为kafka的消费者，有现成api不用自己再自定义spout类
        SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, "/storm", "logmonitor");
        KafkaSpout spout = new KafkaSpout(spoutConfig);
        builder.setSpout("dataSpout", spout, 1);
        //  bolt1：校验数据的bolt --> FilterBolt、
        builder.setBolt("filter", new FilterBolt(), 2).shuffleGrouping("dataSpout");
        //bolt2: 将触发的信息发给指定的负责人 SendEmailBolt
        builder.setBolt("sendEmail", new SendEmailBolt(), 1).shuffleGrouping("filter");
        //bolt3：将触发规则的消息保存到数据库 SaveRecordBolt
        builder.setBolt("saveRecord", new SaveRecordBolt(), 1).shuffleGrouping("sendEmail");

        Config conf = new Config();
//        conf.setDebug(true);
        //配置是在cluster或者local模式下运行
        if(ArrayUtils.isEmpty(args)){//本地
            new LocalCluster().submitTopology("logMonitor",conf,builder.createTopology());
        }else{
            String topologyName = args[1];
            int num = Integer.parseInt(args[0]);
            conf.setNumWorkers(num);
            StormSubmitter.submitTopology(topologyName,conf,builder.createTopology());
        }

    }
}
