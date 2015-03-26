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
package turbo.crawler.db

import java.util.Hashtable

import com.avaje.ebean.EbeanServer

import javax.xml.parsers.DocumentBuilderFactory
import turbo.crawler.JQSupport
import turbo.crawler.Logable
import turbo.crawler.StringAdapter

/**
 * DB操作描述
 * @author mclaren
 *
 */
class DBOfEbean(uiquename: String, driver: String, url: String, username: String, password: String, ddlAutoRun: Boolean, maxThreads: Int, entities: Array[Class[_]]) extends StringAdapter {

  val jdbcDriver = verify(driver, isNotEmpty _, "jdbc驱动不能为空")

  val jdbcUrl = verify(url, isNotEmpty _, "数据库URL不能为空")

  val jdbcUsername = verify(username, isNotEmpty _, "数据库用户名不能为空")

  val jdbcPassword = verify(password, isNotEmpty _, "数据库密码不能为空")

  val jdbcDdlAutoRun = ddlAutoRun

  val name = verify(uiquename, isNotEmpty _, "EbeanServer名称不能为空")

  val entityTypes = entities

  val dbMaxThreads = maxThreads
  /**
   * 验证参数
   */
  def verify(argument: String, f: String => Boolean, err: String): String = if (!f(argument)) throw new IllegalArgumentException(err) else argument

  def ebean(): EbeanServer = {
    EbeanRef.getEbeanServer(this)
  }
}

object db extends Logable with JQSupport with StringAdapter {
  var map = new Hashtable[String, EbeanServer]()

  var loaded = false

  this.define()

  /**
   * 定义一个数据访问层
   */
  def define(uiquename: String, driver: String, url: String, username: String, password: String, ddlAutoRun: Boolean, maxThreads: Int, entities: Array[Class[_]]): DBOfEbean = {
    var db = new DBOfEbean(uiquename, driver, url, username, password, ddlAutoRun, maxThreads, entities)
    //    map += (uiquename -> db.ebean)
    map.put(uiquename, db.ebean)
    db
  }

  /**
   * 从默认的配置文件中定义数据访问层
   */
  def define(): Unit = {
    if (!loaded) {
      var in = this.getClass.getClassLoader.getResourceAsStream("crawler.db.xml")
      if (in == null) {
        this.logger.warn("未找到crawler.db.xml")
      } else {
        /**
         * <connectors>
         *   <connector name="">
         *     <property name="driver"></property>
         *     <property name="username"></property>
         *     <property name="password"></property>
         *     <property name="url"></property>
         *     <property name="entities"></property>
         *   </connector>
         * </connectors>
         */

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in)
        $(doc).filter("connector").filter(connector => isNotEmpty(connector.attr("name"))).foreach(connector => {
          println("CName " + connector.attr("name"))
          var name = connector.attr("name")
          var props = Map[String, String]()
          connector.filter("property").foreach(x => {
            props = props.+((x.attr("name"), x.text()))
          })
          var driver = props.get("driver").get
          var username = props.get("username").get
          var password = props.get("password").get
          var ddlAutoRun = java.lang.Boolean.parseBoolean(props.get("ddlAutoRun").get)
          var url = props.get("url").get
          var maxThreads = props.get("maxThreads").get.toInt
          var entities = props.get("entities").get.split(",").map(x => Thread.currentThread().getContextClassLoader.loadClass(x.trim())).array
          this.define(name, driver, url, username, password, ddlAutoRun, maxThreads, entities)
        })
      }
      this.loaded = true
    }
  }
  def apply(name: String): RdbmsRef = {
    new RdbmsRef(name)
  }

  def find(name: String): EbeanServer = {
    if (!map.containsKey(name)) {
      throw new IllegalArgumentException("未知DB:" + name + ",请确认你实现初始化过这个DB")
    }

    map.get(name)
  }

  def main(args: Array[String]): Unit = {
    define()
  }
}

/**
 * RDBMS Reference
 */
class RdbmsRef(dbname: String) extends Logable with StringAdapter {
  var ebean = db.find(dbname)

  def save(po: AnyRef): Unit = {
    ebean.save(po)
  }

  def update(po: AnyRef): Unit = ebean.update(po)
}