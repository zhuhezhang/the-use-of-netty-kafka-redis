/**
 * Netty服务端类
 * 
 *  @author zhz
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.nio.charset.Charset;

public class PNettyServer {
	ServerBootstrap serverBootstrap;// 服务端启动引导
	int port;// 端口号
	String data = "";// 存放接收自客户端的数据
	EventLoopGroup bossGroup;// accept线程组，用来接受连接
	EventLoopGroup workerGroup;// I/O线程组， 用于处理业务逻辑

	public PNettyServer(int port) {
		this.port = port;
	}

	// 入口方法
	public void Start() {
		bossGroup = new NioEventLoopGroup(1);// accept线程组，用来接受连接
		workerGroup = new NioEventLoopGroup(1);// I/O线程组， 用于处理业务逻辑

		try {
			serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup)// 绑定两个线程组
					.channel(NioServerSocketChannel.class)// 指定通道类型
					.option(ChannelOption.SO_BACKLOG, 100)// 设置TCP连接的缓冲区
					.handler(new LoggingHandler(LogLevel.INFO))// 设置日志级别
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel socketChannel) throws Exception {
							ChannelPipeline channelpipeline = socketChannel.pipeline();// 获取处理器链
							channelpipeline.addLast(new NettyServerHandler());// 添加新的件处理器
						}
					});
			ChannelFuture channelFuture = serverBootstrap.bind(port).sync();// 通过bind启动服务
			channelFuture.channel().closeFuture().sync();// 阻塞主线程，直到网络服务被关闭
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();// 关闭I/O线程组
			bossGroup.shutdownGracefully();// 关闭accept线程祖，服务端关闭
		}
	}

	// 获取dataSB
	public String GetData() {
		return data;
	}

	// 内部事件处理类
	class NettyServerHandler extends ChannelInboundHandlerAdapter {
		// 每当从客户端收到新的数据时，这个方法会在收到消息时被调用
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			data = data + ((ByteBuf) msg).toString(Charset.defaultCharset());
		}

		// 数据读取完后被调用
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();// 清理缓冲区
			workerGroup.shutdownGracefully();// 关闭I/O线程组
			bossGroup.shutdownGracefully();// 关闭accept线程祖，服务端关闭
		}
	}
}
