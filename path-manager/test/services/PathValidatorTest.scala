package services

import org.scalatestplus.play._

class PathValidatorTest extends PlaySpec {
  "Path Validator" must {
    "accept hyphenated alphanumeric paths" in {
      PathValidator.isValid("path/to/something") must be(true)
      PathValidator.isValid("path/to/something-else") must be(true)
      PathValidator.isValid("global/2018/foo-bar") must be(true)
    }

    "accept a path with a hyphenated starting section" in {
      PathValidator.isValid("uk-news/2018/feb/07/foo-bar") must be(true)
    }

    "reject a path when upper cased" in {
      PathValidator.isValid("PATH/TO/SOMETHING") must be(false)
    }

    "reject a path starting with a hyphen" in {
      PathValidator.isValid("-in/valid") must be(false)
      PathValidator.isValid("-in-valid/path") must be(false)
    }

    "reject a path starting with a slash" in {
      PathValidator.isValid("/some/content") must be(false)
    }

    "reject a path with two adjacent slashes" in {
      PathValidator.isValid("path//to/something") must be(false)
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

    "reject a path with a hyphen after a slash" in {
      PathValidator.isValid("path/to/-something") must be(false)
    }
  }
}
