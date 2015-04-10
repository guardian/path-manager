package com.gu.pathmanager

import java.io.{FileNotFoundException, FileInputStream}
import java.sql.DriverManager
import java.util.Properties
import javax.sql.DataSource
import oracle.jdbc.pool.OracleDataSource
import scalikejdbc._


class Migrator(conf: MigratorConfiguration) {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource(conf)))

  val pathManager = new PathManagerConnection(conf.pathManagerUrl)

  def countPages = DB readOnly { implicit session =>
    sql"select count(*) as c from page_draft".map(rs => rs.int("c")).single().apply().get
  }
  
  def migratePaths {
    val total = countPages
    var count = 0
    var errors: List[MigrationError] = Nil

    DB readOnly { implicit session =>
      sql"select id, cms_path from page_draft".foreach{ rs =>

        val id = rs.long("id")
        val path = rs.string("cms_path").replace("/Guardian/", "")
        val record = PathRecord(path, id, "canonical", "r2")

        try {
          pathManager.register(record)
        } catch {
          case e: Exception => {
            errors = MigrationError(path, id, e.getMessage) :: errors
          }
        }

        count = count + 1
        if(count % 1000 == 0) println(s"migrated $count of $total pages. There are ${errors.size} errors")
      }
    }
    println("finished path ingestion.")
    println(s"$count paths injested")
    println("errors")
    errors foreach{error => println(s"\t$error")}
  }

  def updateSequence: Unit = {
    DB readOnly { implicit session =>
      println("updating sequence...")
      val r2Val = sql"select page_draft_seq.nextval as c from dual".map(rs => rs.int("c")).single().apply().get
      val seqVal = pathManager.getCurrentSeqValue
      println(s"current seq values: r2 -> $r2Val, dynamo -> $seqVal")

      val desiredVal = Math.max(seqVal, r2Val + 10000) // use larger of current seq or R2 value + 20000 (to give us grace between migrating and cutting R2 over)
      println(s"desired sequence value $desiredVal")
      if (desiredVal != seqVal) {
        pathManager.setCurrentSeqValue(desiredVal)
        println("updated sequence value")
      } else {
        println("leaving sequence value alone")
      }
    }
  }


  private def dataSource(conf: MigratorConfiguration): DataSource = {
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

    val (migratePaths, migrateSeq) = determineMigrations(args)

    if(migratePaths) migrator.migratePaths

    if(migrateSeq) migrator.updateSequence

  }

  def determineMigrations(args: Array[String]) = args.headOption match {
    case Some("paths") => (true, false)
    case Some("seq") => (false, true)
    case _ => (true, true)
  }

  def loadProperties = try {

    val props = new Properties()
    props.load(new FileInputStream("migrator.properties"))

    def readProperty(name: String) = {
      Option(props.getProperty(name)).getOrElse{
        println(s"unable to read property $name, ensure this is set in migrator.properties")
        sys.exit(1)
      }
    }

    MigratorConfiguration(
      dbAddress = readProperty("databaseAddress"),
      dbService = readProperty("databaseService"),
      user = readProperty("user"),
      password = readProperty("password"),
      pathManagerUrl = readProperty("pathManagerUrl")
    )

  } catch {
    case fnf: FileNotFoundException => {
      println("could not load migrator.properties")
      sys.exit(1)
    }
  }
}

case class MigratorConfiguration(dbAddress: String, dbService: String, user: String, password: String, pathManagerUrl: String)

case class MigrationError(path: String, id: Long, message: String)
