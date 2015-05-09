package sntlm

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress
import akka.io.{ IO, Tcp }
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/* ---------------------------------------------------------------------------------------------------------- */

object Server {
  def props() = Props(classOf[Server])
}
class Server extends Actor {
  import Tcp._
  import context.system
  
  val logger = LoggerFactory.getLogger("Server-localhost:3128")
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 3128))

  def receive = {
    case b @ Bound(localAddress) =>
 
    case CommandFailed(_: Bind) => context stop self
 
    case c @ Connected(remote, local) =>
      val handler = context.actorOf(ProxyHandler.props(remote,local))
      val connection = sender()
      connection ! Register(handler)
  }
}

/* ---------------------------------------------------------------------------------------------------------- */

object EchoHandler {
  def props(remote:InetSocketAddress, local:InetSocketAddress) = {
    Props(classOf[EchoHandler],remote,local)
  }
}
class EchoHandler(remote:InetSocketAddress, local:InetSocketAddress) extends Actor {
  import Tcp._
  val logger = LoggerFactory.getLogger("EchoHandler")
  def receive = {
    case Received(data) =>
      logger.debug(s"Received DATA from ${remote.getHostString}:${remote.getPort}")
      sender() ! Write(data)
    case PeerClosed     =>
      logger.info(s"Closed ${remote.getHostString}")
      context stop self
  }
}

/* ---------------------------------------------------------------------------------------------------------- */

object ProxyHandler {
  def props(remote:InetSocketAddress, local:InetSocketAddress) = {
    Props(classOf[ProxyHandler],remote,local)
  }
}
class ProxyHandler(remote:InetSocketAddress, local:InetSocketAddress) extends Actor {
  import Tcp._
  val logger = LoggerFactory.getLogger("ProxyHandler")
  
  def fakeResponse()={
    """HTTP/1.1 200 OK
      |Content-Type: text/html; charset=utf-8
      |Date: Sat, 09 May 2015 21:04:19 GMT
      |Via: 1.0 localhost.localdomain:3128 (squid/2.7.STABLE9)
      |Content-Length: 44
      |Connection: keep-alive
      |
      |<html><body><h1>It works!</h1></body></html>""".stripMargin
  }
  
  def receive = {
    case Received(data) =>
      logger.debug(s"Received DATA from ${remote.getHostString}:${remote.getPort}")
      println(data.decodeString("US-ASCII"))
      sender() ! Write(ByteString(fakeResponse(), "US-ASCII"))
      //sender() ! Close
    case PeerClosed => context stop self
  }
  
  
}


/* ---------------------------------------------------------------------------------------------------------- */

object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}
 
class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor {
 
  import Tcp._
  import context.system
 
  IO(Tcp) ! Connect(remote)
 
  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "connect failed"
      context stop self
 
    case c @ Connected(remote, local) =>
      listener ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(data) =>
          listener ! data
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
}

/* ---------------------------------------------------------------------------------------------------------- */

object Main {
  def main(args:Array[String]):Unit = {
    implicit val system = ActorSystem("sntlm")
    val server = system.actorOf(Server.props)
  }
}
