/**
 * 爬取网站信息并通过Netty传到P03KafkaProducer
 * 
 * @author zhz
 */

import java.io.*;
import java.net.*;

public class P02Crawler {
	String url;// 爬取的网站网址
	String dataLine;// 爬取的每一行的数据
	StringBuilder dataSB;// 网址+返回的数据（使用字符串生成器，由于需要频繁附加字符串）
	PNettyClient pNettyClient;// 定义Netty客户端对象
	String ip;// ip地址
	int port;// 端口号

	public P02Crawler(String ip, int port, String url) {
		this.ip = ip;
		this.port = port;
		this.url = url;
		dataSB = new StringBuilder(url + '\n');
	}

	// 入口方法
	public void Start() {
		try {
			URL urlObject = new URL(url);// 建立URL对象
			URLConnection conn = urlObject.openConnection();// 通过URL对象打开连接
			InputStream is = conn.getInputStream();// 通过连接取得网页返回的数据
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));// 一般按行读取网页数据，因此用BufferedReader和InputStreamReader把字节流转化为字符流的缓冲流
			while ((dataLine = br.readLine()) != null) {// 按行读取
				dataSB.append(dataLine + '\n');
			}
			pNettyClient = new PNettyClient(ip, port, dataSB.toString());
			pNettyClient.Start();// 通过Netty客户端将数据发送到P03KafkaProducer
			br.close();// 关闭BufferedReader
			is.close();// 关闭InputStream
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
