package services

import model.PathRecord
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play._
import org.scalatest.BeforeAndAfterEach

class PathStoreTest extends PlaySpec with DockerDynamoTestBase with BeforeAndAfterEach {

  val CANONICAL = "canonical"
  val ALIAS = "alias"

  val system = "test"
  val firstPath = "test/first/path"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    // delete any existing instance of firstPath
    PathStore.getPathDetails(firstPath).map(_.identifier).foreach(PathStore.deleteRecord)
  }

  "Path Store" must {

    "support creating a new path entry" in {

      PathStore.registerNew(firstPath, system) shouldBe Symbol("right")

      PathStore.getPathDetails(firstPath).fold(
        fail("newly created path not there after supposedly successful creation")
      )(
        _.path shouldBe firstPath
      )

    }

    "support updating an existing path entry with a new canonical path" in {

      PathStore.registerNew(firstPath, system) shouldBe Symbol("right")

      val newPath = "test/new/path"
      PathStore.getPathDetails(newPath).map(_.identifier).foreach(PathStore.deleteRecord)

      val someReservedPath = "test/reserved/path"
      PathStore.getPathDetails(someReservedPath).map(_.identifier).foreach(PathStore.deleteRecord)
      PathStore.registerNew(someReservedPath, system) // to simulate existing

      PathStore.getPathDetails(firstPath).map(_.identifier).fold(

        fail("first path wasn't actually created so can't test 'PathStore.updateCanonical'")

      )(id => {

        PathStore.updateCanonical(someReservedPath, id) shouldBe Left("path already in use")

        PathStore.updateCanonical(newPath, id) shouldBe Symbol("right")

        PathStore.getPathDetails(firstPath) shouldBe None

        PathStore.getPathDetails(newPath) shouldBe Some(PathRecord(newPath, id, CANONICAL, system))

      })

    }

    "support adding an alias to an existing path" in {

      PathStore.registerNew(firstPath, system) shouldBe Symbol("right")

      val newPath = "test/new/path"
      PathStore.getPathDetails(newPath).map(_.identifier).foreach(PathStore.deleteRecord)

      PathStore.getPathDetails(firstPath).map(_.identifier).fold(

        fail("first path wasn't actually created so can't test 'PathStore.registerAlias'")

      )(id => {

        PathStore.registerAlias(PathRecord(newPath, id, ALIAS, system))
        
        val paths = PathStore.getPathsById(id)

        paths.get(ALIAS).flatMap(_.headOption) shouldBe Some(PathRecord(newPath, id, ALIAS, system))
        
        paths.get(CANONICAL).flatMap(_.headOption) shouldBe Some(PathRecord(firstPath, id, CANONICAL, system))

      })

    }


    "support updating an existing path entry with a new canonical path and adding an alias" in {

      PathStore.registerNew(firstPath, system) shouldBe Symbol("right")

      val newPath = "test/new/path"
      PathStore.getPathDetails(newPath).map(_.identifier).foreach(PathStore.deleteRecord)

      val someReservedPath = "test/reserved/path"
      PathStore.getPathDetails(someReservedPath).map(_.identifier).foreach(PathStore.deleteRecord)
      PathStore.registerNew(someReservedPath, system) // to simulate existing

      PathStore.getPathDetails(firstPath).map(_.identifier).fold(

        fail("first path wasn't actually created so can't test 'PathStore.updateCanonicalWithAlias'")

      )(id => {

        PathStore.updateCanonicalWithAlias(someReservedPath, id) shouldBe Left("path already in use")

        PathStore.updateCanonicalWithAlias(newPath, id) shouldBe Symbol("right")

        PathStore.getPathDetails(firstPath) shouldBe Some(PathRecord(firstPath, id, ALIAS, system))

        PathStore.getPathDetails(newPath) shouldBe Some(PathRecord(newPath, id, CANONICAL, system))
        

      })

    }


    "support deleting all path entries" in {

      val pathsForDeletion = PathStore.registerNew(firstPath, system).fold(errorMessage => {
        fail(s"PathStore.registerNew resulted in a Left($errorMessage)")
        List()
      }, _.values.flatten)

      pathsForDeletion.size shouldBe 2 // one canonical and one short

      pathsForDeletion.map(_.path).map(PathStore.getPathDetails).foreach {
        _ should not be None
      }

      PathStore.getPathDetails(firstPath).map(_.identifier).fold(

        fail("newly created path not there after supposedly successful creation")

      ){id =>

        PathStore.deleteRecord(id)

        pathsForDeletion.map(_.path).map(PathStore.getPathDetails).foreach(
          _ shouldBe None
        )

      }

    }

  }

}
