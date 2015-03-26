package turbo.crawler.sample

import scala.io.Source
import turbo.crawler.io.InternetIO
import turbo.crawler.io.LocalIO
import turbo.crawler.StringAdapter
import turbo.crawler.FetchController
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.SimpleHttpConnectionManager
import java.nio.charset.Charset
import org.apache.commons.httpclient.DefaultMethodRetryHandler
import turbo.crawler.io.HttpReturns
import org.apache.commons.httpclient.params.HttpClientParams
import java.io.ByteArrayOutputStream
import org.apache.commons.httpclient.Header
import org.apache.commons.httpclient.HttpClient

case class DOMAIN(host: String, port: Int, var ping: Long) {
  override def toString = "ping " + host + ":" + port + " " + ping + "ms"
}
object Test extends App with InternetIO with LocalIO with StringAdapter {
  //  val url = "http://www.ebay.com/sch/Test-Auctions-/14112/i.html/i.html?_ipg=200&rt_nc"
  //  var t = fromUrl(url, Array[Header](new Header("Referer", "http://www.tmall.com")))
  //  println(t)
  val url = "http://s.taobao.com/search?q=%E5%BE%B7%E5%9B%BD+%E8%B4%BA%E6%9C%AC%E6%B8%85+%E5%B0%8F%E7%94%98%E8%8F%8A+%E6%8A%A4%E6%89%8B%E9%9C%9C+75&js=1&stats_click=search_radio_all%253A1&initiative_id=staobaoz_20150320&sort=sale-desc"

  val headers = Array[Header]()
  var rets = List[DOMAIN]()
  Source.fromFile("E://dbconfig/proxy").getLines().filter(line => isNotEmpty(line)).map(line => {
    var splits = line.split(" ")
    DOMAIN(splits(0), splits(1).toInt, 0)
  }).foreach(domain => {
    try {
      val time = System.currentTimeMillis()
      val txt = fromHttpClient(url, domain, headers)
      if (txt != null) {
        val ping = System.currentTimeMillis() - time
        domain.ping = ping
        rets = rets.+:(domain)
        println("CHECKING :" + domain)
      }
    } catch {
      case e: Exception =>
    }
  })

  println("====================================")
  rets.sortWith((x, y) => x.ping < y.ping).foreach(println)
  def fromHttpClient(fetchUrl: String, proxy: DOMAIN, headers: Array[Header]): HttpReturns = {
    var get: GetMethod = null
    try {
      //      logger.info("Fetch url {}", url)
      val client = new HttpClient(new HttpClientParams, new SimpleHttpConnectionManager(true))
      //设置代理
      val returns = new HttpReturns(null, null)
      //如果系统激活了代理配置则自动设置
      //      println("proxy: " + proxy)
      client.getHostConfiguration.setProxy(proxy.host, proxy.port)

      client.getHttpConnectionManager.getParams.setConnectionTimeout(10000)
      client.getHttpConnectionManager.getParams.setSoTimeout(10000)
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

      //重定向
      if (code == 302) {
        var directUrl = get.getResponseHeader("Location").getValue

        fromHttpClient(directUrl, proxy, get.getResponseHeaders)
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
    } catch {
      case e: Exception => throw e
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


