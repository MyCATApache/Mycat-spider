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
package turbo.crawler.io

import scala.io.Source

import turbo.crawler.Logable
import turbo.crawler.StringAdapter

/**
 * 网络代理管理器
 * 
 * @author mclaren
 *
 */
case class Proxy(ip: String, port: Int, var enabled: Boolean = true, var timestamp: Long = System.currentTimeMillis())
class ProxyManager extends Logable with LocalIO with StringAdapter {
  var table = Map[String, Proxy]()

  val in = getClass.getClassLoader.getResourceAsStream("proxy")
  if (in != null) {
    Source.fromInputStream(in).getLines().filter(line => isNotEmpty(line)).foreach(line => {
      var splits = line.split(" ")
      table += (splits(0) -> Proxy(splits(0), splits(1).toInt))
    })
  } else {
    logger.warn("could not find any proxy definations")
  }

  /**
   * 屏蔽IP
   */
  def disable(ip: String) = table.synchronized {
    val proxy = table(ip)
    proxy.enabled = false
    proxy.timestamp = System.currentTimeMillis()
  }

  def isActived = table.synchronized(!table.isEmpty)

  /**
   * 获取当前可用的proxy
   */
  def getAvailableProxy() = table.synchronized {
    var t = table.filter(x => x._2.enabled == true).map(x => x._2).toList.sortWith((x, y) => x.timestamp < y.timestamp)
    if (t.isEmpty) {
      logger.info("所有IP都已经被屏蔽,系统正在重新计算")
      t = table.map(x => {
        x._2.enabled = true
        x._2
      }).toList.sortWith((x, y) => x.timestamp < y.timestamp)
    }
    t(0)
  }
}

object call_proxy {
  val instance = new ProxyManager
  def apply() = instance
}