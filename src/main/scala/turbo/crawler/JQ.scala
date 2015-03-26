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
package turbo.crawler

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.Element

/**
 * @author Administrator
 *
 */
/**
 * JQUERY 兼容库
 *
 * @author mclaren
 *
 */
class JQ(nd: List[Node], init: JQ => Unit) extends JQSupport with StringAdapter {
  /**
   * DOM对象
   */
  var dom = nd.filter(x => x != null)

  /**
   * 执行初始化
   */
  init(this)

  def this(nd: List[Node]) = this(nd, (jq: JQ) => {})

  def this(nd: Node, init: JQ => Unit) = this(List(nd), init)
  /**
   * 过滤器
   */
  def filter(expr: String): List[JQ] = {
    if (expr.startsWith("#")) {
      //ID查询
      List[JQ]($(dom2Document(this.dom(0)).getElementById(expr.substring(1).split(" ")(0))))
    } else if (expr.startsWith(".")) {
      //CLASS查询
      var elements = List[Node]()
      this.dom.foreach(nd => elements = elementsbyTagname(nd, expr.split("->")(1).substring(1).trim(), expr.split("->")(0).substring(1).trim()) ::: elements)

      var rts = List[JQ]()

      elements.foreach { nd => rts = $(nd) :: rts }
      rts
    } else {
      //TAG查询
      var elements = List[Node]()
      this.dom.foreach { nd => elements = elementsbyTagname(nd, expr, null) ::: elements }
      var rts = List[JQ]()

      elements.foreach { nd => rts = $(nd) :: rts }
      rts
    }
  }

  /**
   * 获取一个JQ对象
   */
  def first(): JQ = if (this.dom.isEmpty) this else new JQ(List[Node](this.dom(0)))

  /**
   * 获取第一个DOM对象
   */
  def firstDom(f: List[Node] => Node): Node = f(first().dom)

  /**
   * 获取节点属性
   */
  def attr(attrName: String): String = {
    ((nd: Node) => {
      nd match {
        case e: Element => e.getAttribute(attrName)
        case _ => ""
      }
    })(firstDom(nds => if (nds.isEmpty) null else nds(0)))
  }

  /**
   * 获取节点文本内容
   */
  def text(): String = {
    var texts = List[String]()
    this.dom.foreach { n => texts = ((t: String) => { if (isEmpty(t)) "" else t.trim })(n.getTextContent) :: texts }
    texts.mkString
  }

  /**
   * 通过Tag name以及class name进行匹配
   */
  def elementsbyTagname(that: Node, tagname: String, classname: String): List[Node] = {
    val children = (() => {
      if (isEmpty(tagname)) {
        that.getChildNodes
      } else {
        that match {
          case e: Element => e.getElementsByTagName(tagname)
          case d: Document => d.getElementsByTagName(tagname)
          case _ => throw new IllegalArgumentException("DOM类型错误!")
        }
      }
    })()

    var matches = List[Node]()
    for (i <- 0 to children.getLength) {
      matches = children.item(i) :: matches
    }

    if (isEmpty(classname)) matches else
      matches.filter(node => {
        node match {
          case e: Element => equal(classname, e.getAttribute("class"))
          case _ => false
        }
      })
  }

  /**
   * DOM NODE 转换 ELEMENT
   */
  def dom2element(nd: Node): Element = {
    nd match {
      case m: Element => m
      case _ => throw new ClassCastException("需要类型:org.w3c.Element，但实际为:" + nd.getClass)
    }
  }

  def dom2Document(nd: Node): Document = {
    nd match {
      case d: Document => d
      case _ => throw new ClassCastException("需要类型:org.w3c.Document,但实际为:" + nd.getClass)
    }
  }
}

trait JQSupport {
  def $(nd: Node): JQ = new JQ(nd, (jq: JQ) => {})
}