/**
 * @(#) SampleCrawlerOfAutoHomePower.scala 2015年3月7日
 * TURBO CRAWLER高性能网络爬虫
 */
package turbo.crawler.sample

import java.util.Date
import org.w3c.dom.Document
import turbo.crawler.Logable
import turbo.crawler.io.LocalIO
import turbo.crawler.power.EventDrivenFetcher
import turbo.crawler.power.pages
import turbo.crawler.JQSupport
import turbo.crawler.StringAdapter
import turbo.crawler.power.EventManager.shutdown
import turbo.crawler.power.EventManager.attachEvent
import turbo.crawler.power.eventId
import turbo.crawler.db.db
import turbo.crawler.power.MessageDriven
import turbo.crawler.power.Evt

/**
 * 事件机制爬虫DEMO (抓汽车之家）
 * @author mclaren
 *
 */
object SampleCrawlerOfAutoHomePower extends Logable with LocalIO with JQSupport with StringAdapter with MessageDriven {
  var fetcher = new EventDrivenFetcher[AutoBrand]("1")

  var ID_OF_BRAND = eventId(1)

  var FETCH_QQ = eventId(3)

  /**
   * 在运行代码之前，必须在classpath内加入crawler-db.xml文件来描述数据库连接
   */
  var rdbms = db("default-ds")

  attachEvent(ID_OF_BRAND.mkComplted, e => {
    val d = e.source.asInstanceOf[AutoBrand]
    logger.info("complete:" + d.getName + "  " + d.getAutos)
    rdbms.save(e.source)
  })

  var now = new Date

  def main(args: Array[String]): Unit = {
    fromFile("sample/seeds").foreach(x => {
      pages(x._2, x => x, d => 1, (x, y) => x)(d => false)((x, y) => ()).foreach(p => fetcher.fetch(p, x => x, parse _)(x => false)((x, y) => ())())
    })

    shutdown
  }

  def parse(document: Document): List[AutoBrand] = {
    var brands = List[AutoBrand]()
    $(document).filter("dl").foreach(dl => {
      var brand = new AutoBrand
      brand.setFetchedAt(now)
      dl.filter("dt").foreach(dt => {
        dt.filter("a").filter(link => isNotEmpty(link.text())).foreach(link => {
          brand.setName(link.text)
          brand.setUrl(link.attr("href"))
        })
      })

      /* 分析品牌下的车型 */
      dl.filter("li").filter(li => isNotEmpty(li.attr("id"))).foreach(li => {
        var autocar = new AutoCar
        li.filter("h4").foreach(h4 => h4.filter("a").filter(link => isNotEmpty(link.text)).foreach(link => {
          autocar.setName(link.text)
          autocar.setUrl(link.attr("href"))
        }))

        /* 分析价格 */
        li.filter(".red -> a").foreach(link => autocar.setGovPrice(formatPrice(link.text())))

        autocar.setFetchedAt(now)
        brand.getAutos.add(autocar)
      })
      brands = brands.+:(brand)
    })

    brands.filter(brand => isNotEmpty(brand.getName))
  }

  private def formatPrice(s: String): Double = {
    var rt = 0d
    "\\d+\\.\\d+".r.findAllMatchIn(s).foreach { x => rt = x.toString.toDouble }
    rt
  }
}