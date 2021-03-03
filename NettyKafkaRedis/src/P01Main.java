/**
 * Netty库的使用: 从不同的网站进行爬虫（可以是预先设定的也可以是动态爬虫）; 简单起见可以只爬取英文网站; 将html文件信息存储至Apache
 * Kafka队列，同时保留html的url信息; 从Apache Kafka队列中读取文本信息以及url信息;
 * 将读取到的信息再保存至Redis（内存数据库，注意字段划分）; 该文件为程序入口。
 * 
 * @author 朱和章
 */

public class P01Main implements Runnable {
	final static String URL = "https://docs.oracle.com/javase/7/docs/api/overview-summary.html";// 爬取的网站网址（Java第7版API文档）
	final static String IP = "127.0.0.1";// ip地址
	final static int PORT = 8080;// 端口号
	final static String KAFKA_TOPIC = "my_topic";// 存放数据的kafka主题名
	final static String REDIS_KEY = "my_key";// 存放数据的redis键值

	@Override
	public void run() {
		try {
			Thread.sleep(3000);
			new P02Crawler(IP, PORT, URL).Start();// 爬取网站信息并通过Netty传到P03KafkaProducer
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// 需要提前通过终端输入以下命令打开Zookeeper、Kafka、Redis服务端
		// /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/bin/zookeeper-server-start.sh /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/config/zookeeper.properties
		// /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/bin/kafka-server-start.sh /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/config/server.properties
		// /home/mpimpi/计算机网络管理/redis-6.0.9/src/redis-server /home/mpimpi/计算机网络管理/redis-6.0.9/redis.conf

		// 删除、读取kafka中名为my_topic的主题内容
		// /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/bin/kafka-topics.sh --delete --zookeeper localhost:2181 --topic my_topic
		// /home/mpimpi/计算机网络管理/kafka_2.13-2.6.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my_topic --from-beginning
		
		// 启动redis-cli、获取/删除键值为my_key的内容
		// /home/mpimpi/计算机网络管理/redis-6.0.9/src/redis-cli
		// get my_key
		// del my_key
		
		P01Main p01Main = new P01Main();
		new Thread(p01Main).start();// 该线程休眠3s保证接收信息的服务端已启动并已做好接收来自客户端的信息的准备
		new P03KafkaProducer(KAFKA_TOPIC, PORT).Start();// 通过Netty获取Crawler发送过来的信息并将其保存到Kafka
		new P05SaveToRedis().Start(REDIS_KEY, new P04KafkaConsumer().Start(KAFKA_TOPIC));// 从Kafka队列中读取信息并将该信息返回，同时将返回的信息传到Redis
	}
}