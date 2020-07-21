package services

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import play.api.{Logger, Logging}

object Dynamo extends AwsInstanceTags with Logging {

  val LOCAL_PORT = 10005

  lazy val stageTablePrefix = readTag("Stage").getOrElse("DEV")

  lazy val dynamoDb = new DynamoDB( instanceId match {
    case Some(_) =>
      AmazonDynamoDBClientBuilder.standard().withRegion(AWS.region).build()

    case None => {
      val c = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("local", "local")))
        .withEndpointConfiguration(new EndpointConfiguration(s"http://localhost:$LOCAL_PORT", "local"))
        .build()

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

  private def createSequenceTableIfMissing(client: AmazonDynamoDB) = {
    val dynamo = new DynamoDB(client)

    if(!tableExists(sequenceTableName, dynamo)) {
      logger.info("creating sequence table")
      val table = dynamo.createTable(
        new CreateTableRequest()
          .withTableName(sequenceTableName)
          .withAttributeDefinitions( new AttributeDefinition("sequenceName", ScalarAttributeType.S) )
          .withKeySchema(new KeySchemaElement("sequenceName", KeyType.HASH))
          .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)) // ignored locally
      )
      table.waitForActive()
    } else {
      logger.info("sequence table already exists")
    }
  }

  private def initialiseSequences(client: AmazonDynamoDB) = {
    val table = new DynamoDB(client).getTable(sequenceTableName)
    if (Option(table.getItem("sequenceName", "ids")).isEmpty){
      logger.info("initialising id sequence")
      table.putItem(new Item().withString("sequenceName", "ids").withLong("value", 2000000L))
    }
  }

  private def createPathsTableIfMissing(client: AmazonDynamoDB) = {
    val dynamo = new DynamoDB(client)

    if(!tableExists(pathsTableName, dynamo)) {
      logger.info("creating paths table")
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
      logger.info("paths table already exists")
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

