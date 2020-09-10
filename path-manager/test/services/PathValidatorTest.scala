package services

import org.scalatestplus.play._

class PathValidatorTest extends PlaySpec {
  "Path Validator" must {
    "accept hyphenated alphanumeric paths" in {
      PathValidator.isValid("path/to/something") must be (true) 
      PathValidator.isInvalid("path/to/something") must be (false)
      PathValidator.isValid("path/to/something-else") must be (true) 
      PathValidator.isInvalid("path/to/something-else") must be (false)
      PathValidator.isValid("global/2018/foo-bar") must be (true) 
      PathValidator.isInvalid("global/2018/foo-bar") must be (false)
    }

    "accept a path with a hyphenated starting section" in {
      PathValidator.isValid("uk-news/2018/feb/07/foo-bar") must be (true) 
      PathValidator.isInvalid("uk-news/2018/feb/07/foo-bar") must be (false)
    }

    "reject a path when upper cased" in {
      PathValidator.isValid("PATH/TO/SOMETHING") must be (false) 
      PathValidator.isInvalid("PATH/TO/SOMETHING") must be (true)
    }

    "reject a path starting with a hyphen" in {
      PathValidator.isValid("-in/valid") must be (false) 
      PathValidator.isInvalid("-in/valid") must be (true)
      PathValidator.isValid("-in-valid/path") must be (false) 
      PathValidator.isInvalid("-in-valid/path") must be (true)
    }

    "reject a path starting with a slash" in {
      PathValidator.isValid("/some/content") must be (false) 
      PathValidator.isInvalid("/some/content") must be (true)
    }

    "reject a path with two adjacent slashes" in {
      PathValidator.isValid("path//to/something") must be (false) 
      PathValidator.isInvalid("path//to/something") must be (true)
    }

    "reject paths with white space" in {
      PathValidator.isValid("path/to/some thing") must be (false) 
      PathValidator.isInvalid("path/to/some thing") must be (true)
      PathValidator.isValid("path/to/some thing else") must be (false) 
      PathValidator.isInvalid("path/to/some thing else") must be (true)
    }

    "reject paths with single quotes" in {
      PathValidator.isValid("path/to/'something'") must be (false) 
      PathValidator.isInvalid("path/to/'something'") must be (true)
      PathValidator.isValid("path/to/'something'-else") must be (false) 
      PathValidator.isInvalid("path/to/'something'-else") must be (true)
    }

    "reject paths with a combination of single quotes and white space" in {
      PathValidator.isValid("path/to/'something' else-news") must be (false) 
      PathValidator.isInvalid("path/to/'something' else-news") must be (true)
    }

    "reject a path with a hyphen after a slash" in {
      PathValidator.isValid("path/to/-something") must be (false) 
      PathValidator.isInvalid("path/to/-something") must be (true)
    }
  }
}
