/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package turbo.crawler.power

import java.util.ArrayList
import java.util.Hashtable
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import turbo.crawler.Lifecycle
import turbo.crawler.Logable
import turbo.crawler.StringAdapter
import java.util.Collections

/**
 * Event manager
 * @author mclaren
 *
 */
object EventManager extends Lifecycle with Logable with StringAdapter with MessageDriven {
  /**
   * 线程池
   */
  private val exec = new ScheduledThreadPoolExecutor(sysprop("fetch.threads", "100").toInt)

  /**
   * 事件处理器
   */
  private val handlers = new Hashtable[String, java.util.List[Evt => Unit]]()

  /**
   * 获取JVM配置参数
   */
  private def sysprop(key: String, default: String) = {
    var matched = System.getProperty(key)
    if (isNotEmpty(matched)) matched else default
  }

  /**
   * 卸载系统
   */
  override def shutdown = {
    try {
      while (true) {
        if (exec.getActiveCount == 0) {
          exec.shutdown()
          throw new RuntimeException()
        }
      }
    } catch {
      case e: Exception => logger.info("Fetch completed and shutdown concurrenty fetchers.")
    }
  }

  /**
   * 向系统注册事件监听
   */
  def attachEvent(eventId: String, handler: Evt => Unit): Unit = {
    handlers.synchronized {
      var hds = handlers.get(eventId)
      if (hds == null) hds = new ArrayList[Evt => Unit]()
      hds.add(handler)
      handlers.put(eventId, hds)
    }
  }

  /**
   * 处理事件分发
   */
  override def fireEvent(evt: Evt): Unit = {
    if (handlers.containsKey(evt.eventId)) new WrapList[Evt => Unit](handlers.get(evt.eventId)).foreach(fd => dispatchEventConcurrently(evt, fd)) else logger.error("No handlers for event" + evt)
  }

  /**
   * 并行分发事件
   */
  private def dispatchEventConcurrently(evt: Evt, f: Evt => Unit) = {
    var task = new FutureTask[Unit](new Callable[Unit]() {
      def call: Unit = f(evt)
    })
    this.exec.submit(task)
  }

  /**
   * 包装Java列表为SCALA风格
   */
  private class WrapList[T](list: java.util.List[T]) {
    def foreach(f: T => Unit) = for (i <- 0 to list.size() - 1) f(list.get(i))
  }
}