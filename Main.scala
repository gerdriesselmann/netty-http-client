package net.gerdriesselmann.httpclient

import java.net.URI

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer, SimpleChannelInboundHandler}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.CharsetUtil

import scala.collection.JavaConverters._

object Main {

	private class HttpClientHandler extends SimpleChannelInboundHandler[HttpObject] {
		override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
			msg match {
				case response: HttpResponse =>
					System.out.println("STATUS: " + response.status)
					System.out.println("VERSION: " + response.protocolVersion)
					System.out.println()

					val headers = response.headers
					if (!headers.isEmpty) {
						for (
							name <- headers.names.asScala;
							value <- headers.getAll(name).asScala
						) yield System.out.println(s"HEADER: $name=$value")
					} else {
						System.out.println("No HTTP Headers received")
					}
					System.out.println()

					if (HttpUtil.isTransferEncodingChunked(response)) {
						System.out.println("CHUNKED CONTENT {")
					} else {
						System.out.println("CONTENT {")
					}

				case _ =>
			}

			msg match {
				case content: HttpContent =>
					System.out.print(content.content.toString(CharsetUtil.UTF_8))

					if (content.isInstanceOf[LastHttpContent]) {
						System.out.println("} END OF CONTENT")
						ctx.close()
					}

				case _ =>
			}
		}

		override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
			cause match {
				case e: Exception =>
					System.err.println(s"Caught Exception: ${e.getMessage}")

			}
			cause.printStackTrace()
			ctx.close
		}
	}

	class HttpClientInitializer(val sslCtx: Option[SslContext]) extends ChannelInitializer[SocketChannel] {
		override def initChannel(ch: SocketChannel): Unit = {
			val p = ch.pipeline
			// Enable HTTPS if necessary.
			sslCtx.foreach(ctx => p.addLast(ctx.newHandler(ch.alloc)))
			p.addLast(new HttpClientCodec)
			p.addLast(new HttpContentDecompressor)
			p.addLast(new HttpClientHandler)
		}
	}

	def main(args: Array[String]): Unit = {
		if (args.isEmpty) {
			System.err.println("Pass URL to download, please")
			System.exit(2)
		}
		val url = args.head

		val uri = new URI(url)
		val scheme = Option(uri.getScheme).getOrElse("http")
		val is_http = "http".equalsIgnoreCase(scheme)
		val is_https = "https".equalsIgnoreCase(scheme)

		if (!is_http && !is_https) {
			System.err.println("Only HTTP(S) is supported.")
			System.exit(2)
		}

		val host = uri.getHost
		var port = uri.getPort
		if (port == -1) {
			if (is_http) {
				port = 80
			} else if (is_https) {
				port = 443
			}
		}

		// Configure SSL context if necessary.
		val is_ssl = "https".equalsIgnoreCase(scheme)
		val ssl_ctx = Option.when(is_ssl)(SslContextBuilder.forClient.trustManager(InsecureTrustManagerFactory.INSTANCE).build)

		// Configure the client.
		var ret_code = 0
		val group = new NioEventLoopGroup
		try {
			val b = new Bootstrap
			b.group(group).channel(classOf[NioSocketChannel]).handler(new HttpClientInitializer(ssl_ctx))
			// Make the connection attempt.
			val ch = b.connect(host, port).sync.channel
			// Prepare the HTTP request.
			val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath, Unpooled.EMPTY_BUFFER)
			request.headers.set(HttpHeaderNames.HOST, host)
			request.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
			request.headers.set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
			// Send the HTTP request.
			ch.writeAndFlush(request)
			// Wait for the server to close the connection.
			ch.closeFuture.sync
		} catch {
			case e: Exception =>
				System.err.println(e.getMessage)
				ret_code =1
		} finally {
			// Shut down executor threads to exit.
			group.shutdownGracefully()
		}
		System.exit(ret_code)
	}
}