package services

import org.scalatestplus.play._
import ShortUrlEncoder._


class ShortUrlEncoderTest extends PlaySpec {

  "Short url encoder" must {

    "encode and decode page id to original value" in {
      decodePathCodeToPageId(encodePageIdToPathCode(0L)) must be(0L)
      decodePathCodeToPageId(encodePageIdToPathCode(12345L)) must be(12345L)
      decodePathCodeToPageId(encodePageIdToPathCode(23456L)) must be(23456L)
      decodePathCodeToPageId(encodePageIdToPathCode(34567L)) must be(34567L)
    }

    "decode and encode short path to original value" in {
      encodePageIdToPathCode(decodePathCodeToPageId("2b65e")) must be("2b65e")
      encodePageIdToPathCode(decodePathCodeToPageId("2b42f")) must be("2b42f")
      encodePageIdToPathCode(decodePathCodeToPageId("2b58j")) must be("2b58j")
    }
  }
}
