package API.stuff

import java.util.Date

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class LoggableFuture[A](val f: Future[A]) {
  def thenLog(s: String): Future[A] ={
    f onSuccess {case _ => println(s"[${new Date()}][Success] $s")}
    f onFailure {case e => println(s"[${new Date()}][Failure] $s\n${e.getMessage}")}
    f
  }
}

object LoggableFuture {
  implicit def i1[A](f: Future[A]): LoggableFuture[A] = new LoggableFuture(f)
  implicit def i2[A](lf: LoggableFuture[A]): Future[A] = lf.f
}