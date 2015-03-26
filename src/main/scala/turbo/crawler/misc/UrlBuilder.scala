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
package turbo.crawler.misc

import java.net.URL

/**
 * URL Builder 工具类方便对已有的URL进行编辑
 * @author mclaren
 *
 */
class UrlBuilder(url: String) {
  private var u = new URL(url)
  private var f = u.getFile
  private var m = Map[String, String]()
  private var queries = u.getQuery.split("&").foreach(x => {
    var s = x.split("=")
    if (s.length == 2)
      m = m.+((s(0), s(1)))
  })

  def param(name: String): String = {
    try {
      m.get(name).get
    } catch {
      case e: java.util.NoSuchElementException => null
    }
  }

  def param(name: String, value: Any): UrlBuilder = {
    if (this.m.contains(name)) {
      m = m.updated(name, "" + value)
    } else {
      m = m.+((name, "" + value))
    }
    this
  }

  override def toString: String = {
    var buf = new StringBuffer(url.substring(0, url.indexOf("?"))).append("?")
    m.foreach(x => buf.append(x._1).append("=").append(x._2).append("&"))
    buf.toString
  }
}