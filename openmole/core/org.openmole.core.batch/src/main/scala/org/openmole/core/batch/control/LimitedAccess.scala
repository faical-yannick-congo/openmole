/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.control

import java.util.concurrent.locks.ReentrantLock

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.stm._

class LimitedAccess(val nbTokens: Int, val maxByPeriod: Int) extends UsageControl { la ⇒

  case class LimitedAccessToken(number: Int) extends AccessToken {
    val lock = new ReentrantLock {
      def thread = Option(super.getOwner)
    }

    override def access[T](op: ⇒ T): T = {
      lock.lock()
      try {
        la.accessed()
        op
      }
      finally lock.unlock()
    }
  }

  def period = 1 minute

  private lazy val tokens: TSet[LimitedAccessToken] =
    TSet((0 until nbTokens).map { i ⇒ new LimitedAccessToken(i) }: _*)

  private lazy val taken: TMap[LimitedAccessToken, Int] = TMap()
  private lazy val accesses: Ref[List[Long]] = Ref(List())

  private def add(token: LimitedAccessToken) = atomic { implicit txn ⇒
    val n = taken(token)
    if (n < 1) {
      taken -= token
      tokens += token
    }
    else taken(token) = n - 1
  }

  def releaseToken(token: AccessToken) = {
    add(token.asInstanceOf[LimitedAccessToken])
  }

  private def checkAccessRate() = atomic { implicit txn ⇒
    clearOldAccesses()
    if (accesses().size < maxByPeriod) true
    else false
  }

  private def accessed() = atomic { implicit txn ⇒
    clearOldAccesses()
    accesses() = System.currentTimeMillis() :: accesses()
  }

  private def clearOldAccesses() = atomic { implicit txn ⇒
    val now = System.currentTimeMillis()
    accesses() = accesses().takeWhile(_ > now - period.toMillis)
  }

  def tryGetToken: Option[AccessToken] = tryGetToken(Thread.currentThread())

  def tryGetToken(thread: Thread): Option[AccessToken] = atomic { implicit txn ⇒
    val allReadyHasAToken = taken.find { case (k, v) ⇒ k.lock.thread.map(_ == thread).getOrElse(false) }

    def tryGet = (checkAccessRate(), tokens.headOption) match {
      case (true, Some(head)) ⇒ Some(head)
      case _                  ⇒ None
    }

    allReadyHasAToken match {
      case Some((t, n)) ⇒
        taken += (t -> (n + 1))
        Some(t)
      case None ⇒
        val token = tryGet
        token.foreach {
          t ⇒
            tokens -= t
            taken += (t -> 0)
        }
        token
    }
  }

  def waitAToken: AccessToken = {
    val thread = Thread.currentThread()
    atomic { implicit txn ⇒
      @tailrec def getToken: AccessToken =
        tryGetToken(thread) match {
          case None ⇒
            retryFor(1000)
            getToken
          case Some(token) ⇒ token
        }
      getToken
    }
  }

  def available = tokens.single.size
}
