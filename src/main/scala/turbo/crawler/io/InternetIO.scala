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

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import org.apache.commons.httpclient.DefaultMethodRetryHandler
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.SimpleHttpConnectionManager
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpClientParams
import turbo.crawler.FetchController
import turbo.crawler.FetchController.ensureReady
import turbo.crawler.Logable
import scala.io.Source
import org.apache.commons.httpclient.HttpHost

/**
 * 互联网IO
 * @author mclaren
 *
 */
case class HttpReturns(var body: String, var proxy: Proxy)

trait InternetIO extends Logable {
  var cookie = ""

  val proxy = call_proxy()

  val cfg = System.getProperty("cookie.cfg")
  if (cfg != null)
    Source.fromFile(cfg).getLines().foreach(line => cookie = line)
  /**
   * 根据URL抓取网络内容
   */
  def fromUrl(url: String, headers: Array[Header]) = {
    ensureReady(url)
    fromHttpClient(url, headers)
  }

  private def fromHttpClient(url: String, headers: Array[Header]): HttpReturns =
    fromHttpClient(url, headers.+:(new Header("Cookie", cookie)), x => ())
  /**
   * 通过HTTP client进行抓取
   */
  private def fromHttpClient(url: String, headers: Array[Header], f: Header => Unit): HttpReturns = {
    var get: GetMethod = null
    try {
      logger.info("Fetch url {}", url)
      val client = new HttpClient(new HttpClientParams, new SimpleHttpConnectionManager(true))
      //设置代理
      val returns = new HttpReturns(null, null)
      if (proxy.isActived) {
        //如果系统激活了代理配置则自动设置
        val p = proxy.getAvailableProxy()
        returns.proxy = p
        client.getHostConfiguration.setProxy(p.ip, p.port)
      }

      client.getHttpConnectionManager.getParams.setConnectionTimeout(15000)
      client.getHttpConnectionManager.getParams.setSoTimeout(15000)
      var retryHandler = new DefaultMethodRetryHandler
      retryHandler.setRetryCount(1)
      retryHandler.setRequestSentRetryEnabled(false)

      get = new GetMethod(url)
      get.setMethodRetryHandler(retryHandler)
      get.setRequestHeader("User-Agent", "Mozilla/5.0 (X11 Ubuntu i686; rv:26.0) Gecko/20100101 Firefox/26.0")

      get.setRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

      get.setRequestHeader("Accept-Language", "en-US,en;q=0.5")

      get.setRequestHeader("Connection", "close")
      headers.foreach(get.setRequestHeader _)

      var code = client.executeMethod(get)

      get.getResponseHeaders.foreach(f)

      //重定向
      if (code == 302) {
        var directUrl = get.getResponseHeader("Location").getValue

        fromHttpClient(directUrl, get.getResponseHeaders)
      } else {
        var encoding = getencoding(get.getResponseHeader("Content-Type").getValue, Charset.defaultCharset().name())

        var inputstream = get.getResponseBodyAsStream
        var out = new ByteArrayOutputStream
        var i = 0
        var bytes = new Array[Byte](1024)

        do {
          i = inputstream.read(bytes)
          if (i != -1)
            out.write(bytes, 0, i)
        } while (i != -1)

        var s = new String(out.toByteArray(), encoding)
        FetchController.recordUrl(url)
        returns.body = s
        returns
      }
    } finally {
      if (get != null) get.releaseConnection();
    }
  }

  private def getencoding(contentType: String, default: String): String = {
    try {
      var cs = default
      contentType.split(";").foreach(part => {
        if (part.toUpperCase().trim().startsWith("CHARSET")) cs = part.split("=")(1).trim
      })
      cs
    } catch {
      case e: Exception => default
    }
  }
}