package sntlm

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress
import akka.io.{ IO, Tcp }
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object Server {
  def props() = Props(classOf[Server])
}
class Server extends Actor {
  import Tcp._
  import context.system
 
  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8888))

  def receive = {
    case b @ Bound(localAddress) =>
 
    case CommandFailed(_: Bind) => context stop self
 
    case c @ Connected(remote, local) =>
      val handler = context.actorOf(EchoHandler.props(remote,local))
      val connection = sender()
      connection ! Register(handler)
  }
}


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
      logger.info(s"Received data on ${local.getHostString}:${local.getPort} FROM ${remote.getHostString}:${remote.getPort}")
      sender() ! Write(data)
    case PeerClosed     =>
      logger.info(s"Closed ${remote.getHostString}")
      context stop self
  }
}



object Main {
  def main(args:Array[String]):Unit = {
    implicit val system = ActorSystem("sntlm")
    val server = system.actorOf(Server.props)
  }
}
