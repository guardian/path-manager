package services

import org.scalatestplus.play._

class PathValidatorTest extends PlaySpec {
  "Path Validator" must {
    "accept hyphenated alphanumeric paths" in {
      PathValidator.isValid("path/to/something") must be(true)
      PathValidator.isValid("path/to/something-else") must be(true)
      PathValidator.isValid("global/2018/foo-bar") must be(true)
      PathValidator.isValid("p/2nfpb") must be(true)
    }

    "reject a path starting with a hyphen" in {
      PathValidator.isValid("-in/valid") must be(false)
    }

    "reject a path starting with a slash" in {
      PathValidator.isValid("/some/content") must be(false)
    }

    "reject paths with white space" in {
      PathValidator.isValid("path/to/some thing") must be(false)
      PathValidator.isValid("path/to/some thing else") must be(false)
    }

    "reject paths with single quotes" in {
      PathValidator.isValid("path/to/'something'") must be(false)
      PathValidator.isValid("path/to/'something'-else") must be(false)
    }

    "reject paths with a combination of single quotes and white space" in {
      PathValidator.isValid("path/to/'something' else-news") must be(false)
    }
  }
}
