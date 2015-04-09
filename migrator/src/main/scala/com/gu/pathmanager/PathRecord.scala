package com.gu.pathmanager

import com.amazonaws.services.dynamodbv2.document.Item

case class PathRecord(path: String, identifier: Long, `type`: String, system: String) {

  def asDynamoItem = new Item()
    .withString("path", path)
    .withLong("identifier", identifier)
    .withString("type", `type`)
    .withString("system", system)
}

object PathRecord {

  def apply(item: Item): PathRecord = PathRecord(
    path = item.getString("path"),
    `type` = item.getString("type"),
    identifier = item.getLong("identifier"),
    system = item.getString("system")
  )
}