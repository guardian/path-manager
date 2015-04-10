package com.gu.pathmanager

import com.squareup.okhttp.{FormEncodingBuilder, RequestBody, Request, OkHttpClient}

class PathManagerConnection(pathManagerBaseUrl: String) {

  val httpclient = new OkHttpClient()

  def register(proposedPathRecord: PathRecord) = {
    //do noting currently

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

class PathInUseException(m: String) extends Exception(m)