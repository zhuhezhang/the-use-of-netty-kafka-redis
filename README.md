在Linux环境下，从网站进行爬虫，并通过Netty将爬取的信息发送至另一方；将接收的html文件信息存储至Apache Kafka队列，同时保留html的url信息；再从Apache Kafka队列中读取文本信息以及url信息；将读取到的信息再保存至Redis数据库。
@[TOC](目录)
# 1．环境搭建
## 1.1安装JDK
 - 从官网下载合适的安装包，这里使用的是安装包是jdk-15.0.1_linux-x64_bin.tar.gz
 - 解压tar -zxvf jdk-15.0.1_linux-x64_bin.tar.gz
 - 设置环境变量：打开文件vim /etc/profile并在最前面添加，其中第一个路径为jdk文件解压后的路径
```powershell
export JAVA_HOME=/usr/lib/jvm/jdk 
export JRE_HOME=${JAVA_HOME}/jre
export CLASSPATH=.:${JAVA_HOME}/lib:${JRE_HOME}/lib  
export  PATH=${JAVA_HOME}/bin:$PATH
```
 - 使用source /etc/profile使修改后的文件生效
## 1.2安装eclipse
从官网上下载，解压后直接打开里面的执行文件就可以了，这里使用的安装包是eclipse-java-2020-12-R-linux-gtk-x86_64.tar.gz
## 1.3安装、打开Kafka服务
 - 从官网下载安装包，这里使用的安装包是kafka_2.13-2.6.0.tgz
 - 解压下载的安装包tar -zxf kafka_2.13-2.6.0.tgz
 - 切换到解压后的文件的目录，cd kafka_2.13-2.6.0
 - 启动安装包里面自带的zookeeper（如果没有的话）bin/zookeeper-server-start.sh config/zookeeper.properties
 - 最后再通过命令bin/kafka-server-start.sh config/server.properties启动Kafka服务
## 1.4安装、打开Redis服务
 - 官网下载安装包，这里使用的是redis-6.0.9.tar.gz并解压
 - 切换到解压后的目录
 - make
 - 完成后使用./src/redis-server ./redis.conf启动Redis服务即可开始使用
## 1.5导入jar包
 - 在maven项目的pom.xml文件中添加以下代码导入kafka及其相关jar包
```xml
<dependency>
			<groupId>org.apache.kafka</groupId>
			<artifactId>kafka-clients</artifactId>
			<version>1.0.0</version>
		</dependency>
		<dependency>
			<groupId>org.slf4j</groupId>
			<artifactId>slf4j-jdk14</artifactId>
			<version>1.7.25</version>
		</dependency>
```
- 在项目中新建lib文件，并把jedis-3.3.0.jar和netty-all-4.1.54.Final.jar（官网下载）放入并建立路径来使用redis和netty服务
# 2．程序使用说明
在程序运行之前需要通过命令行启动zookeeper/kafka/redis服务，也就是实验环境搭建部分的服务启动，然后直接在eclipse中运行即可。而爬虫获得的数据在kafka中保存在名为my_topic的主题，数据在redis数据库中保存的键值为my_key。
# 3．总体设计
P01Main类为程序入口，负责程序其它类的调用。由于需要通过netty来将P02Crawler爬虫程序爬取的信息传到P03KafkaProducer，且netty client在前者，后netty server在后者，server需要在client之前开启。所以将P02Crawler爬虫程序放在另一个线程，先休眠3s，等待P03里面的server启动完成。P03接收到爬虫类传过来的信息后传到Kafka队列，然后通过P04KafkaConsumer消费Kafka队列里面的信息并返回作为P05SaveToRedis的方法Start的形参并通过该方法保存到redis中。
# 4．详细设计	
## 4.1 PNettyServer.java
该类为Netty服务端。首先通过构造函数传入目的port端口号，在入口方法Start实例化一个I/O线程组，用于处理业务逻辑；同时还有accepet线程组，用来接受连接。通过服务端启动引导的group方法绑定这两个线程组、channel指定通道类型、option设置TCP连接的缓冲区、handle设置日志级别、childHandle里面的匿名类方法initChannel，通过覆盖此方法获取处理器链、添加新的件处理器。然后利用服务端启动引导bind方法绑定端口并启动服务，返回ChannelFuture对象并阻塞主线程直到服务被关闭，同时还定义一个获取data接收的信息方法，里面直接返回data。
对于添加的服务端内部事件类，继承自ChannelInboundHandlerAdapter类。每当有数据从客户端发送过来时，覆盖的方法channelRead方法会被调用，将接收的信息类型Object转化为ByteBuf类型，再利用toString函数转化为String类型。这里由于接收的数据过长，所以它会自动将数据分为几部分发送，所以这里需要将每次接收的信息连接起来。读完数据后，覆盖的方法channelReadComplete方法会被调用，里面会清除缓冲区。由于这里只需要接收一次信息，所以在接收完成后，关闭I/O线程组、accept线程组，服务结束。
## 4.2 PNettyClient.java
该类为Netty服务端。通过构造函数传入ip, port, data，分别对应服务端IP地址、端口号、以及要发送给服务端的数据。首先还是通过客户端启动引导绑定I/O线程组，用于处理业务逻辑，通过channel方法指定通道类型、option设置TCP连接的缓冲区、handle设置事件处理类，里面的匿名类含有覆盖的方法initChannel用于获取处理器链并添加新的件处理器。然后利用服务端启动引导bind方法绑定端口、IP并启动服务，返回ChannelFuture对象并阻塞主线程直到服务被关闭。
对于添加的客户端内部处理器类，继承自ChannelInboundHandlerAdapter类。里面含有channelActive覆盖的方法，启动就执行。里面先将String类型的data通过getBytes方法转化为字节类型，再利用Unpooled的wrappedBuffer方法打包，最后再利用ChannelHandlerContext的方法writeAndFlush方法将数据写入通道并刷新。

## 4.3 P02Crawler.java
该类爬取指定的网站信息并通过Netty传到P03KfkaProducer类。构造函数传入ip, port, url（爬取网站网址），由于爬取的网站信息读取是逐行读取的，对字符串的连接较多，所以这里使用字符串生成器StringBuilder更为节省时间，然后将爬取的网站网址添加在首行。在入口方法Start处，通过网址建立URL对象，利用其方法openConnection打开连接并返回URLConnection对象，再通过该对象的getInputStream方法连接取得网页返回的数据，这里返回InputStream对象。将该对象转化为BufferedReader对象，并指定UTF-8编码，利用while进行按行读取并利用字符串生成器append方法逐行添加，再利用PnettyClient传送数据后依次关闭BufferedStream, InputStream。以上代码放在try-catch语句块中用于处理异常信息。

## 4.4 P03KafkaProducer.java
该类通过Netty获取P02Crawler发送过来的信息并将其保存到Kafka队列。构造函数形参包括Kafka主题名、端口号，里面通过调用PNettyServer来获取发送过来的数据。在入口方法Start处，新建一个Properties配置文件对象设置bootstrap.servers为localhost:9092用于建立初始连接到kafka集群的"主机/端口对"配置列表（必须）；acks 为all，表示Producer在确认一个请求发送完成之前需要收到的反馈信息的数量，0表示不会收到，认为已经完成发送；retries为0，若设置大于0的值，则客户端会将发送失败的记录重新发送（可选）；batch.size为16384，控制发送数据批次的大小（可选）；linger.ms为1，发送时间间隔，producer会将两个请求发送时间间隔内到达的记录合并到一个单独的批处理请求中（可选）；buffer.memory为33554432，Producer用来缓冲等待被发送到服务器的记录的总字节数（可选）；key.serializer为org.apache.kafka.common.serialization.StringSerializer，关键字的序列化类（必须）；value.serializer为org.apache.kafka.common.serialization.StringSerializer，值的序列化类（必须）。
然后利用以上的配置新建一个Kafka生产者，并利用send方法将数据往Kafka队列指定主题的发送，最后close关闭即可。

 ## 4.5 P04KafkaConsumer.java
该类从Kafka队列中读取信息并将该信息返回。通过入口方法Start传入主题名参数，同样的新建一个配置文件对象，bootstrap.servers字段设置为localhost:9092，用于建立初始连接到kafka集群的"主机/端口对"配置列表（必须）；group.id为my_group，消费者组id（必须）；auto.offset.reset为earliest，自动将偏移量重置为最早的偏移量（可选）；enable.auto.commit为true，消费者的偏移量将在后台定期提交（可选）；auto.commit.interval.ms为1000，如果将enable.auto.commit设置为true，则消费者偏移量自动提交给Kafka的频率（以毫秒为单位）（可选）；key.deserializer为org.apache.kafka.common.serialization.StringDeserializer，关键字的序列化类（必须）；value.deserializer 为org.apache.kafka.common.serialization.StringDese
rializer，值的序列化类（必须）。
	利用上述配置新建一个Kafka消费者，利用方法subscribe订阅指定名称的主题。再利用poll方法获取队列中的数据集，再利用for结构输出即可，同时close消费者并返回获得的数据。

## 4.6 P05SaveToRedis.java
该类将传过来的信息保存到Redis。该类的入口方法包含存放数据key值、要保存的数据。创建Jedis对象，连接本地的Redis服务，利用该对象的set方法存放数据，然后利用close关闭Redis服务即可。

## 4.7 P01Main.java
该方法为该程序的入口，负责各个类方法的调用。程序前面定义一些常量，包括爬取的网址站址、IP地址、端口号、存放数据的Kafka主题名、存放数据的Redis键值。在主方法里面创建该类对象，执行线程start方法，run方法里面休眠3s后再爬虫爬取信息，保证P03KafkaProducer类里面的Netty服务端启动完成，再通过P04KafkaConsumer的Start方法获取发送过来的信息，并将其作为P05SveToRedis类Start方法的参数之一保存到Redis。
# 5．存在问题
每次重新启动各项相关服务后运行的第1、2或者第1次运行都会出现从Kafka队列中读取的数据是空的，之后的运行就完全没问题了。虽然已经知道是消费者的问题，但目前仍然没有找到解决办法。
