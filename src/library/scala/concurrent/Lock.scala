/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala.concurrent

/** This class ...
 *
 *  @author  Martin Odersky
 *  @version 1.0, 10/03/2003
 */
class Lock {
  var available = true

  def acquire() = synchronized {
    while (!available) _root_.java.lang.Object.instancehelper_wait(this)
    available = false
  }

  def release() = synchronized {
    available = true
    _root_.java.lang.Object.instancehelper_notify(this)
  }
}
