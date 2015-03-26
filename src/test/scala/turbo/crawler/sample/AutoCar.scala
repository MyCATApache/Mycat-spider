/**
 * @(#) AutoCar.scala 2015年3月4日
 * TURBO CRAWLER高性能网络爬虫
 */
package turbo.crawler.sample

import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.GenerationType
import turbo.crawler.Fetchable

/**
 * @author Administrator
 *
 */
@Entity
@Table(name = "AUTO_CAR")
class AutoCar extends Fetchable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private var id = 0
  def setId(id: Int) = this.id = id
  def getId = id

  @Column(name = "NAME", nullable = false)
  private var name = ""
  def setName(name: String) = this.name = name
  def getName = name

  @Column(name = "FETCHED_AT", nullable = false)
  private var fetchedAt: Date = null
  def setFetchedAt(date: Date) = this.fetchedAt = date
  def getFetchedAt = fetchedAt

  @Column(name = "GOV_PRICE")
  private var govPrice: Double = 0
  def setGovPrice(price: Double) = this.govPrice = price
  def getGovPrice = this.govPrice

  @Column(name = "URL")
  private var url = ""
  def setUrl(url: String) = this.url = url
  def getUrl = this.url

  override def getDirectUrl = url
}