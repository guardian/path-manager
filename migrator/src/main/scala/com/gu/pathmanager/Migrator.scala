package com.gu.pathmanager

import java.io.{FileNotFoundException, FileInputStream}
import java.util.Properties


object Migrator {

  def main(args: Array[String]) {
    println(loadProperties)

  }

  def loadProperties = try {

    val props = new Properties()
    props.load(new FileInputStream("db.properties"))

    def readProperty(name: String) = {
      Option(props.getProperty(name)).getOrElse{
        println(s"unable to read property $name, ensure this is set in db.properties")
        sys.exit(1)
      }
    }

    DbConfiguration(
      url = readProperty("url"),
      user = readProperty("user"),
      password = readProperty("password")
    )

  } catch {
    case fnf: FileNotFoundException => {
      println("could not load db.properties")
      sys.exit(1)
    }
  }
}

case class DbConfiguration(url: String, user: String, password: String)
