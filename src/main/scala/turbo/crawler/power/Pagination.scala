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
import java.net.SocketException
import java.net.SocketTimeoutException

import org.apache.commons.httpclient.ConnectTimeoutException
import org.apache.commons.httpclient.Header
import org.cyberneko.html.parsers.DOMParser
import org.w3c.dom.Document
import org.xml.sax.InputSource

import turbo.crawler.FetchRejectedException
import turbo.crawler.Logable
import turbo.crawler.ResourceHasAlreadyBeenFetchedException
import turbo.crawler.io.HttpReturns
import turbo.crawler.io.InternetIO

/**
 * 分页支持
 * @author mclaren
 *
 */
object pages extends Logable with InternetIO {
  def apply(fetchUrl: String, contentFilter: String => String, checkBoundary: Document => Int, urlFactory: (String, Int) => String)(hasRejected: Document => Boolean)(howToContinue: (String, turbo.crawler.io.Proxy) => Unit): List[String] = {
    var value = new ValueRef[Int](0)
    resetBoundary(fetchUrl, value, contentFilter, checkBoundary, urlFactory)(hasRejected)(howToContinue)
    var rts = List[String]()
    value.get
    for (i <- 1 to value.get) {
      rts = rts.+:(urlFactory(fetchUrl, i))
    }
    rts
  }

  private def resetBoundary(fetchUrl: String, lastPage: ValueRef[Int], contentFilter: String => String = x => x, checkBoundary: Document => Int, urlFactory: (String, Int) => String)(hasRejected: Document => Boolean /* 检验是否被目标拒绝 ，如屏蔽*/ )(howToContinue: (String, turbo.crawler.io.Proxy) => Unit /*屏蔽补偿*/ ): Unit = {
    val _retry = (() => {
      Thread.sleep(3000)
      resetBoundary(fetchUrl, lastPage, contentFilter, checkBoundary, urlFactory)(hasRejected)(howToContinue)
    })

    var httpReturns: HttpReturns = null
    try {
      var domp = new DOMParser
      httpReturns = this.fromUrl(fetchUrl, Array[Header]())
      domp.parse(new InputSource(new StringReader(contentFilter(httpReturns.body))))
      var document = domp.getDocument

      if (hasRejected(document)) throw new FetchRejectedException(fetchUrl, httpReturns.proxy)

      lastPage.set(checkBoundary(document))
    } catch {
      case e: SocketTimeoutException => _retry()
      case e: SocketException => _retry()
      case e: ConnectTimeoutException => _retry()
      case e: IOException => _retry()
      case e: FetchRejectedException => {
        logger.info("Oh 惨遭屏蔽~")
        howToContinue(e.getFetchUrl, httpReturns.proxy)
        _retry()
      }
      case e: ResourceHasAlreadyBeenFetchedException =>
      case e: Exception => {
        logger.error("Unknown exception has been occurred", e)
      }
    }
  }
}
class ValueRef[M](v: M) {
  var value = v
  def set(vv: M) = this.value = vv
  def get = value
}