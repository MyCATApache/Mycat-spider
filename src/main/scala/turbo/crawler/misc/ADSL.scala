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
import java.util.ArrayList
import java.util.Collections

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod

import turbo.crawler.Logable
import turbo.crawler.StringAdapter

/**
 * ADSL路由器拨号 <tt>当出现目标IDC对爬虫进行屏蔽时,可以通过本工具类实现ADSL网络重新拨号更改IP以突破屏蔽限制</tt>
 *
 * <b>本拨号工具针对TPLINK品牌</b>
 * @author mclaren
 *
 */
object ADSL extends Logable with StringAdapter {
  private var locking = false

  private var timestamp = 0L

  private val queue = Collections.synchronizedList(new ArrayList[AnyRef]())

  val lock = new Object

  var flag = false
  var thread = new Thread(new Runnable() {
    override def run = {
      while (true) {
        if (flag) _reconnect
        for (i <- 0 to queue.size - 1) {
          var x = queue.get(i)
          x.synchronized(x.notify)
        }
        queue.clear
        flag = false

        Thread.sleep(100)
      }
    }
  })

  thread.setDaemon(true)
  thread.start()

  /**
   * 防止出现雪崩式拨号
   */
  def reconnect(caller: AnyRef) = {
    this.synchronized { if (!flag) flag = queue.isEmpty }
    queue.add(caller)
    caller.synchronized(caller.wait)
  }

  /**
   * 执行重新拨号连接ADSL网络
   */
  private def _reconnect = {
    if (!locking) {
      locking = true
      var client = new HttpClient
      var username = prop("router.username")
      var password = prop("router.password")
      var url = prop("router.url")

      var u = new URL(url)
      var credentials = new UsernamePasswordCredentials(username, password)
      client.getState.setCredentials(AuthScope.ANY, credentials)
      client.getHostConfiguration.setHost(u.getHost, 80)
      var get = new GetMethod(url)
      var status = client.executeMethod(get)

      if (status != 200) {
        logger.error("Reconnect router fault,caused by HTTP response code is :" + status)
      } else {
        var authorization = get.getRequestHeader("Authorization").getValue
        var disconnect = new GetMethod(
          "http://" + u.getHost() + "/userRpm/StatusRpm.htm?Disconnect=%B6%CF%20%CF%DF&wan=1");
        disconnect.setRequestHeader("Authorization", authorization);

        status = client.executeMethod(disconnect);

        logger.info("disconnect WAN connection on Router [OK]");

        Thread.sleep(3000)

        var connect = new GetMethod(
          "http://" + u.getHost() + "/userRpm/StatusRpm.htm?Connect=%C1%AC%20%BD%D3&wan=1");
        connect.setRequestHeader("Authorization", authorization);
        status = client.executeMethod(connect);
        logger.info("connect WAN connection on Router [OK]");
        locking = false;
      }
    }
  }

  private def prop(name: String) = {
    var value = System.getProperty(name)
    if (isEmpty(value)) throw new IllegalArgumentException("required parameter:[" + name + "] must not be null")
    value
  }
}