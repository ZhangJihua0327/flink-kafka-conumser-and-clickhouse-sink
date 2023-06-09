package org.group32;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.group32.pojo.*;
import org.group32.utils.ClickhouseSinkFunction;

import java.util.*;

public class Main {
    static Map<String, Class<? extends POJO>> pojoMap = new HashMap<String, Class<? extends POJO>>();

    static {
        pojoMap.put("contract", DmVTrContractMx.class);
        pojoMap.put("djk", DmVTrDjkMx.class);
        pojoMap.put("dsf", DmVTrDsfMx.class);
        pojoMap.put("duebill", DmVTrDuebillMx.class);
        pojoMap.put("etc", DmVTrEtcMx.class);
        pojoMap.put("grwy", DmVTrGrwyMx.class);
        pojoMap.put("gzdf", DmVTrGzdfMx.class);
        pojoMap.put("huanb", DmVTrHuanbMx.class);
        pojoMap.put("huanx", DmVTrHuanxMx.class);
        pojoMap.put("sa", DmVTrSaMx.class);
        pojoMap.put("sbyb", DmVTrSbybMx.class);
        pojoMap.put("sdrq", DmVTrSdrqMx.class);
        pojoMap.put("sjyh", DmVTrSjyhMx.class);
        pojoMap.put("shop", VTrShopMx.class);
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5000);
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "03290941");
        kafkaProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        String topic = "test";

        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<String>(topic, new SimpleStringSchema(),
                kafkaProps);

        DataStreamSource<String> source = env.addSource(consumer);
        SingleOutputStreamOperator<POJO> dataSource = createFlinkOperator(source);
        ClickhouseSinkFunction sink = new ClickhouseSinkFunction();
        dataSource.addSink(sink);
        env.execute("Flink Streaming Java API Skeleton");
    }

    public static SingleOutputStreamOperator<POJO> createFlinkOperator(DataStream<String> stream) {
        MapFunction<String, POJO> mpFunc = s -> {
            HashMap<String, JSONObject> json = JSON.parseObject(s, HashMap.class);
            Class<? extends POJO> clazz = pojoMap.get(json.get("eventType"));
            POJO pojo = JSON.parseObject(json.get("eventBody").toString(), clazz);
            System.out.println(pojo.toString());
            return pojo;
        };
        return stream.map(mpFunc);
    }
}