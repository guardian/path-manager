package services

import java.util.concurrent.atomic.{AtomicReference, AtomicLong}

import play.api.libs.json.Json

object PathStore {

  val identifierSeq = new AtomicLong(2000000)
  val pathRepository: AtomicReference[List[PathRecord]] = new AtomicReference[List[PathRecord]](Nil)

  def registerNew(path: String, system: String) = {
    if (pathRepository.get.exists(_.path == path)) {
      Left("path already in use")
    } else {
      val id = identifierSeq.getAndIncrement
      val pathRecord = PathRecord(path, "canonical", id, system)
      val shortUrlPathRecord = PathRecord("simulatedShort/" + path, "short", id, system)

      pathRepository.set(pathRecord :: pathRepository.get())
      Right(List(pathRecord, shortUrlPathRecord))
    }
  }

  def register(path: String, id: Long, system: String) = {
    if (pathRepository.get.exists{ p => p.path == path && p.identifier != id}) {
      Left("path already in use")
    } else {
      val pathRecord = PathRecord(path, "canonical", id, system)
      val shortUrlPathRecord = PathRecord("simulatedShort/" + path, "short", id, system)

      pathRepository.set(pathRecord :: (pathRepository.get().filterNot(_.identifier == id)))
      Right(List(pathRecord, shortUrlPathRecord))
    }
  }

  def updateCanonical(newPath: String, existingPath: String, id: Long) = {
        
    if (pathRepository.get.exists( rec => rec.path == newPath && rec.identifier != id)) {
      Left("path already in use")
    } else if (pathRepository.get.exists( rec => rec.path == existingPath && rec.identifier != id)) {
      Left(s"$existingPath is not owned by $id")
    } else {
      pathRepository.get.find{ rec => rec.identifier == id && rec.`type` == "canonical"}.map { existing =>
        val newRecord = existing.copy(path = newPath)
        pathRepository.set(newRecord :: (pathRepository.get().filterNot(_ == existing)))
        List(newRecord)
      }.toRight(s"unable to find canonical record for $id")
    }
  }

  def getPathDetails(path: String) = {
    pathRepository.get().find(_.path == path)
  }

}

case class PathRecord(path: String, `type`: String, identifier: Long, system: String) {
  def asJson = Json.obj(
    "path" -> path,
    "identifier" -> identifier,
    "type" -> `type`,
    "system" -> system
  )
}
