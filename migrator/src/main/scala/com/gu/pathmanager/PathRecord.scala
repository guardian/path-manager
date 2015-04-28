package com.gu.pathmanager

case class PathRecord(path: String, identifier: Long, `type`: String, system: String) {
  def toJsonString = s"""{"path":"${path}","identifier":${identifier},"type":"${`type`}","system":"${system}"}"""
}