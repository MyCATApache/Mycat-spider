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

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * 全局SEQUENCE，建议采用MYCAT-SERVER的全局SEQUENCE功能
 * @author mclaren
 */
trait IdGenerator {
  def gernateId(dbname: String, sequenceId: Long) = this.synchronized {
    val ebean = db(dbname).ebean
    val sequence = ebean.find(classOf[SEQUENCE]).where().eq("id", sequenceId).findUnique()
    if (sequence == null) throw new IllegalArgumentException("No such sequence [" + sequenceId + "] found")

    val v = sequence.getNextVal
    sequence.setNextVal(v + sequence.getStep)
    sequence.setValue(v)
    ebean.update(sequence)
    v
  }
}

@Entity
@Table(name = "GLOBAL_SEQUENCE")
class SEQUENCE {
  @Id
  private var id = 0L
  def setId(id: Long) = this.id = id
  def getId = this.id

  @Column(name = "CURRENT_VAL")
  private var value = 0L
  def setValue(v: Long) = this.value = v
  def getValue = this.value

  @Column(name = "STEP")
  private var step = 1L
  def setStep(s: Long) = this.step = s
  def getStep = this.step

  @Column(name = "NEXT_VAL")
  private var nextVal = 0L
  def setNextVal(v: Long) = this.nextVal = v
  def getNextVal = this.nextVal
}
