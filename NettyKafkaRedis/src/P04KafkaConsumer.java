/**
 * 从Kafka队列中读取信息并将该信息返回
 * 
 * @author 朱和章
 */

import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class P04KafkaConsumer {
	private String data;// 从kafka队列获得的数据

	// 入口方法
	public String Start(String topic) {
		Properties props = new Properties();// 新建一个配置文件对象
		props.put("bootstrap.servers", "localhost:9092");// 用于建立初始连接到kafka集群的"主机/端口对"配置列表（必须）
		props.put("group.id", "my_group");// 消费者组id（必须）
		props.put("auto.offset.reset", "earliest");// 自动将偏移量重置为最早的偏移量（可选）
		props.put("enable.auto.commit", "true");// 消费者的偏移量将在后台定期提交（可选）
		props.put("auto.commit.interval.ms", "1000");// 如果将enable.auto.commit设置为true，则消费者偏移量自动提交给Kafka的频率（以毫秒为单位）（可选）
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");// 关键字的序列化类（必须）
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");// 值的序列化类（必须）

		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);// 新建一个以上述定义的配置的消费者
		consumer.subscribe(Collections.singletonList(topic));// 订阅消息主题url
		ConsumerRecords<String, String> records = consumer.poll(100);
		consumer.close();
		for (ConsumerRecord<String, String> record : records) {
			data = record.value();
		}
		return data;
	}
}