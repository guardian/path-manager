package com.gu.pathmanager

import org.apache.commons.lang3.StringUtils.replaceChars

object ShortUrlEncoder {

  private val UNSAFE_CHARS: String = "1lo0i"
  private val SAFE_CHARS: String = "xyzvt"

  def generateShortUrlPath(pageId: Long) = "p/" + encodePageIdToPathCode(pageId)

  def encodePageIdToPathCode(pageId: Long) = {
    typographicallyEncode(base27encode(pageId))
  }

  def decodePathCodeToPageId(code: String) = {
    base27decode(typographicallyDecode(code))
  }

  private def base27encode(pageId: Long) = {
    java.lang.Long.toString(pageId, 27)
  }

  private def base27decode(encodedPageId: String) = {
    java.lang.Long.parseLong(encodedPageId, 27)
  }

  private def typographicallyEncode(text: String) = {
    replaceChars(text.toLowerCase, UNSAFE_CHARS, SAFE_CHARS)
  }

  private def typographicallyDecode(text: String) = {
    replaceChars(text.toLowerCase, SAFE_CHARS, UNSAFE_CHARS)
  }
}
