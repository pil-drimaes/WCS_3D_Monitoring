package com.example.WCS_DataStream.etl.service;

import com.example.WCS_DataStream.etl.model.vendor.ant.AntPodInfoRecord;
import com.example.WCS_DataStream.etl.model.vendor.ant.AntRobotInfoRecord;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyAgvInfoRecord;
import com.example.WCS_DataStream.etl.model.vendor.mushiny.MushinyPodInfoRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.antRobotInfo:ant_robot_info_events}")
    private String antRobotTopic;

    @Value("${kafka.topic.antPodInfo:ant_pod_info_events}")
    private String antPodTopic;

    @Value("${kafka.topic.mushinyAgv:mushiny_agv_events}")
    private String mushinyAgvTopic;

    @Value("${kafka.topic.mushinyPod:mushiny_pod_events}")
    private String mushinyPodTopic;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAntRobot(AntRobotInfoRecord record) {
        if (record == null) return;
        kafkaTemplate.send(antRobotTopic, record.getUuid(), record);
    }

    public void publishAntPod(AntPodInfoRecord record) {
        if (record == null) return;
        kafkaTemplate.send(antPodTopic, record.getUuid(), record);
    }

    public void publishMushinyAgv(MushinyAgvInfoRecord record) {
        if (record == null) return;
        kafkaTemplate.send(mushinyAgvTopic, record.getUuid(), record);
    }

    public void publishMushinyPod(MushinyPodInfoRecord record) {
        if (record == null) return;
        kafkaTemplate.send(mushinyPodTopic, record.getUuid(), record);
    }
}


