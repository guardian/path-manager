package com.gu.pathmanager

import com.squareup.okhttp._

class PathManagerConnection(pathManagerBaseUrl: String) {

  val httpclient = new OkHttpClient()

  val JSON = MediaType.parse("application/json; charset=utf-8")

  def register(proposedPathRecord: PathRecord) = {
    val id = proposedPathRecord.identifier

    val body = RequestBody.create(JSON, proposedPathRecord.toJsonString)
    val req = new Request.Builder().url(pathManagerBaseUrl + s"paths/$id").put(body).build()
    val resp = httpclient.newCall(req).execute()

    if(!resp.isSuccessful) {
      val body = resp.body().string()
      throw new Exception(body)
    }
  }


  def getCurrentSeqValue: Long = {
    val req = new Request.Builder().url(pathManagerBaseUrl + "showCurrentSequence").build()
    val resp = httpclient.newCall(req).execute()

    resp.body().string().toLong
  }

  def setCurrentSeqValue(v: Long) {
    val body = new FormEncodingBuilder().add("val", s"$v").build()
    val req = new Request.Builder().url(pathManagerBaseUrl + "updateIdSeq").post(body).build()
    val resp = httpclient.newCall(req).execute()

    if (!resp.isSuccessful) {
      throw new Exception(s"failed to update sequence to $v")
    }
  }
}