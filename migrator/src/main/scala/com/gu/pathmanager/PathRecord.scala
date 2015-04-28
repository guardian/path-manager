package com.gu.pathmanager

import com.amazonaws.services.dynamodbv2.document.Item

case class PathRecord(path: String, identifier: Long, `type`: String, system: String) {
  def toJsonString = s"""{"path":"${path}","identifier":${identifier},"type":"${`type`}","system":"${system}"}"""

  def asDynamoItem = new Item()
    .withString("path", path)
    .withLong("identifier", identifier)
    .withString("type", `type`)
    .withString("system", system)

  val stx = '\u0002'
  val etx = '\u0003'
  val lf = '\u000A'

  def asDynamoImportLine = s"""path${etx}{"s":"${path}"}${stx}identifier${etx}{"n":"${identifier}"}${stx}type${etx}{"s":"${`type`}"}${stx}system${etx}{"s":"${system}"}${lf}"""

}

object PathRecord {

  def apply(item: Item): PathRecord = PathRecord(
    path = item.getString("path"),
    `type` = item.getString("type"),
    identifier = item.getLong("identifier"),
    system = item.getString("system")
  )
}
