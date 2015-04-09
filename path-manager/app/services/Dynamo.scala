package services

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{Item, DynamoDB}
import com.amazonaws.services.dynamodbv2.model._
import play.api.Logger

object Dynamo extends AwsInstanceTags {

  lazy val stageTablePrefix = readTag("Stage").getOrElse("DEV")

  lazy val dynamoDb = new DynamoDB( instanceId match {
    case Some(_) => AWS.region.createClient(classOf[AmazonDynamoDBClient], null, null)
    case None => {
      val c = new AmazonDynamoDBClient(new BasicAWSCredentials("local", "local"))

      c.setEndpoint("http://localhost:10005")

      createSequenceTableIfMissing(c)
      initialiseSequences(c)
      createPathsTableIfMissing(c)
      c
    }
  })

  private lazy val sequenceTableName = s"$stageTablePrefix-pathManager-sequence"
  private lazy val pathsTableName = s"$stageTablePrefix-pathManager-paths"

  lazy val sequenceTable = dynamoDb.getTable(sequenceTableName)
  lazy val pathsTable = dynamoDb.getTable(pathsTableName)

  private def createSequenceTableIfMissing(client: AmazonDynamoDBClient) {
    val dynamo = new DynamoDB(client)

    if(!tableExists(sequenceTableName, dynamo)) {
      Logger.info("creating sequence table")
      val table = dynamo.createTable(
        new CreateTableRequest()
          .withTableName(sequenceTableName)
          .withAttributeDefinitions( new AttributeDefinition("sequenceName", ScalarAttributeType.S) )
          .withKeySchema(new KeySchemaElement("sequenceName", KeyType.HASH))
          .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)) // ignored locally
      )
      table.waitForActive()
    } else {
      Logger.info("sequence table already exists")
    }
  }

  private def initialiseSequences(client: AmazonDynamoDBClient) {
    val table = new DynamoDB(client).getTable(sequenceTableName)
    if (Option(table.getItem("sequenceName", "ids")).isEmpty){
      Logger.info("initialising id sequence")
      table.putItem(new Item().withString("sequenceName", "ids").withLong("value", 2000000L))
    }
  }

  private def createPathsTableIfMissing(client: AmazonDynamoDBClient) {
    val dynamo = new DynamoDB(client)

    if(!tableExists(pathsTableName, dynamo)) {
      Logger.info("creating paths table")
      val table = dynamo.createTable(
        new CreateTableRequest()
          .withTableName(pathsTableName)
          .withAttributeDefinitions(
            new AttributeDefinition("path", ScalarAttributeType.S),
            new AttributeDefinition("identifier", ScalarAttributeType.N),
            new AttributeDefinition("type", ScalarAttributeType.S)
          )
          .withKeySchema(new KeySchemaElement("path", KeyType.HASH))
          .withGlobalSecondaryIndexes(
            new GlobalSecondaryIndex()
              .withIndexName("id-index")
              .withKeySchema(new KeySchemaElement("identifier", KeyType.HASH), new KeySchemaElement("type", KeyType.RANGE) )
              .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
              .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
          )
          .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)) // ignored locally
      )
      table.waitForActive()
    } else {
      Logger.info("paths table already exists")
    }
  }

  def tableExists(name: String, dynamo: DynamoDB) = {
    try {
      dynamo.getTable(name).describe()
      true
    } catch {
      case e: ResourceNotFoundException => false
    }
  }
}
