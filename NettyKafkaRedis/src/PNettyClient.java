/**
 * Netty客户端类
 * 
 *  @author 朱和章
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class PNettyClient {
	EventLoopGroup eventLoopGroup;// I/O线程组， 用于处理业务逻辑
	String data;// 要发送给服务端的消息
	String ip;// 服务端ip地址
	int port;// 服务端端口号

	public PNettyClient(String ip, int port, String data) {
		this.ip = ip;
		this.port = port;
		this.data = data;
	}

	// 入口方法
	public void Start() {
		eventLoopGroup = new NioEventLoopGroup();

		try {
			Bootstrap bootstrap = new Bootstrap();// 客户端启动引导
			bootstrap.group(eventLoopGroup)// 绑定线程组
					.channel(NioSocketChannel.class)// 指定通道类型
					.option(ChannelOption.TCP_NODELAY, true)// 设置TCP连接的缓冲区
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel socketChannel) throws Exception {
							ChannelPipeline channelpipeline = socketChannel.pipeline();// 获取处理器链
							channelpipeline.addLast(new NettyClientHandler());// 添加新的件处理器
						}
					});
			ChannelFuture channelFuture = bootstrap.connect(ip, port).sync();// 通过bind启动客户端
			channelFuture.channel().closeFuture().sync();// 阻塞在主线程，直到发送信息结束
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			eventLoopGroup.shutdownGracefully();// 关闭I/O线程组
		}
	}

	// 内部事件处理类
	class NettyClientHandler extends ChannelInboundHandlerAdapter {
		// 启动就发送数据到服务端
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ctx.writeAndFlush(Unpooled.wrappedBuffer(data.getBytes()));
		}
	}
}