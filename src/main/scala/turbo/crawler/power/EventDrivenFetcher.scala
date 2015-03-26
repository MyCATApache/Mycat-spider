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

import java.io.IOException
import java.io.StringReader
import java.net.BindException
import java.net.SocketException
import java.net.SocketTimeoutException

import org.apache.commons.httpclient.ConnectTimeoutException
import org.apache.commons.httpclient.Header
import org.cyberneko.html.parsers.DOMParser
import org.w3c.dom.Document
import org.xml.sax.InputSource

import turbo.crawler.FetchRejectedException
import turbo.crawler.Fetchable
import turbo.crawler.IO
import turbo.crawler.Logable
import turbo.crawler.ResourceHasAlreadyBeenFetchedException
import turbo.crawler.StringAdapter
import turbo.crawler.io.HttpReturns

/**
 * Event driven fetcher
 * @author mclaren
 *
 */
class EventDrivenFetcher[T <: Fetchable](eventId: String) extends Logable with MessageDriven with IO with StringAdapter {
  def fetch(fetchUrl: String /* URL */ ,
    contentFilter: String => String /* content filter */ ,
    parseDocument: Document => List[T])(hasRejected: Document => Boolean) /* test if it has been rejected */ (howToContinue: (String, turbo.crawler.io.Proxy) => Unit)(referer: String => String = x => x): Unit /* How to continue */ = {
    val _retry = (msg: String) => {
      logger.info("Retry " + msg)
      Thread.sleep(3000)
      this.fetch(fetchUrl, contentFilter, parseDocument)(hasRejected)(howToContinue)(referer)
    }
    var httpReturns: HttpReturns = null
    try {
      val dom = new DOMParser
      httpReturns = this.fromUrl(fetchUrl, Array[Header](new Header("Referer", referer(fetchUrl))))
      dom.parse(new InputSource(new StringReader(contentFilter(httpReturns.body))))
      var document = dom.getDocument

      //检查是否被屏蔽
      if (hasRejected(document)) throw new FetchRejectedException(fetchUrl)

      parseDocument(document).foreach(x => fireEvent(new Evt(eventId + "_COMPLETION", x)))

    } catch {
      case e: SocketTimeoutException => _retry(e.getMessage)
      case e: SocketException => _retry(e.getMessage)
      case e: ConnectTimeoutException => _retry(e.getMessage)
      case e: IOException => {
        logger.info("Oh网络错误with代理：" + httpReturns.proxy.ip + ":" + httpReturns.proxy.port)
        howToContinue(fetchUrl, httpReturns.proxy)
        //10秒之内只允许出现一次重拨
        _retry(e.getMessage)
      }
      case e: BindException => _retry(e.getMessage)
      case e: FetchRejectedException => {
        logger.info("Oh 惨遭屏蔽~")
        howToContinue(e.getFetchUrl, httpReturns.proxy)
        //10秒之内只允许出现一次重拨
        _retry(e.getMessage)
      }
      case e: ResourceHasAlreadyBeenFetchedException =>
      case e: Exception => {
        logger.error("Unknown exception has been occurred", e)
      }
    }
  }
}
