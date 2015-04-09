package com.gu.pathmanager

import java.io.{FileNotFoundException, FileInputStream}
import java.sql.DriverManager
import java.util.Properties
import javax.sql.DataSource
import oracle.jdbc.pool.OracleDataSource
import scalikejdbc._


class Migrator(conf: DbConfiguration) {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource(conf)))

  def doSomeSql = DB readOnly { implicit session =>
    sql"select count(*) as c from page_draft".map(rs => rs.long("c")).single().apply().get
  }


  private def dataSource(conf: DbConfiguration): DataSource = {
    val oracleDataSource = new OracleDataSource

    oracleDataSource.setUser(conf.user)
    oracleDataSource.setPassword(conf.password)
    oracleDataSource.setDriverType("thin")
    oracleDataSource.setServerName(conf.dbAddress)
    oracleDataSource.setPortNumber(1521)
    oracleDataSource.setServiceName(conf.dbService)

    val properties = new Properties
    properties.put("v$session.program", "path-manager-migrator")
    oracleDataSource.setConnectionProperties(properties)


    println(s"Connecting as ${conf.user} to ${conf.dbAddress} / ${conf.dbService}")
    oracleDataSource
  }
}


object Migrator {

  def main(args: Array[String]) {
    val migrator = new Migrator(loadProperties)

    println(s"there are ${migrator.doSomeSql} draft pages")

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
      dbAddress = readProperty("databaseAddress"),
      dbService = readProperty("databaseService"),
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

case class DbConfiguration(dbAddress: String, dbService: String, user: String, password: String)
