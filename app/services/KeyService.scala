package services

object KeyService {

  // TODO actually think about keys / signing requests, security in general
  def validRequest(system: String, key: String) = {
    system == key
  }
}
