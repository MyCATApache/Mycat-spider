/**
 * @(#) AutoBrand.scala 2015年3月4日
 * TURBO CRAWLER高性能网络爬虫
 */
package turbo.crawler.sample

import java.util.Date
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.GenerationType
import javax.persistence.OneToMany
import turbo.crawler.Fetchable
import javax.persistence.CascadeType

/**
 * @author Administrator
 *
 */
@Entity
@Table(name = "AUTO_BRAND")
class AutoBrand extends Fetchable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private var id = 0
  def setId(id: Int) = this.id = id
  def getId = id

  @Column(name = "NAME", nullable = false)
  private var name = ""
  def setName(name: String) = this.name = name
  def getName = name

  @Column(name = "URL", nullable = false)
  private var url = ""
  def setUrl(url: String) = this.url = url
  def getUrl = this.url

  @Column(name = "FETCHED_AT", nullable = false)
  private var fetchedAt: Date = null
  def setFetchedAt(date: Date) = this.fetchedAt = date
  def getFetchedAt = fetchedAt

  @OneToMany(targetEntity = classOf[AutoCar], cascade = Array[CascadeType] { CascadeType.ALL })
  private var autos: java.util.List[AutoCar] = new java.util.ArrayList[AutoCar]()
  def setAutos(autos: java.util.List[AutoCar]) = this.autos = autos
  def getAutos = this.autos

  override def getDirectUrl = ""
}