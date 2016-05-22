package API.stuff

import java.util.Date

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

class FutureImplicits[A](val f: Future[A]) {
  def thenLog(s: String): Future[A] ={
    f onSuccess {case _ => println(s"[${new Date()}][Success] $s")}
    f onFailure {case e => println(s"[${new Date()}][Failure] $s\n${e.getMessage}")}
    f
  }

  def >>[B](body: => Future[B]) = f flatMap (_ => body)
  def >>=[B](body: (A => Future[B])) = f flatMap body
}

object FutureImplicits {
  implicit def i1[A](f: Future[A]): FutureImplicits[A] = new FutureImplicits(f)
  implicit def i2[A](lf: FutureImplicits[A]): Future[A] = lf.f
}