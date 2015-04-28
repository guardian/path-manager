package com.gu.pathmanager

import com.amazonaws.auth.{BasicAWSCredentials, AnonymousAWSCredentials, AWSCredentials}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{Item, DynamoDB}
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeTagsRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.util.EC2MetadataUtils
import scala.collection.JavaConverters._


object MigrationAWS {

  lazy val region = Region getRegion Regions.EU_WEST_1

  lazy val EC2Client = region.createClient(classOf[AmazonEC2Client], null, null)

}

trait AwsInstanceTags {
  lazy val instanceId = Option(EC2MetadataUtils.getInstanceId)

  def readTag(tagName: String) = {
    instanceId.flatMap { id =>
      val tagsResult = MigrationAWS.EC2Client.describeTags(
        new DescribeTagsRequest().withFilters(
          new Filter("resource-type").withValues("instance"),
          new Filter("resource-id").withValues(id),
          new Filter("key").withValues(tagName)
        )
      )
      tagsResult.getTags.asScala.find(_.getKey == tagName).map(_.getValue)
    }
  }
}

object Dynamo extends AwsInstanceTags {

  lazy val stageTablePrefix = readTag("Stage").getOrElse("DEV")

  lazy val dynamoDb = new DynamoDB(instanceId match {
    case Some(_) => MigrationAWS.region.createClient(classOf[AmazonDynamoDBClient], null, null)
    case None => {
      val c = new AmazonDynamoDBClient(new BasicAWSCredentials("local", "local"))

      c.setEndpoint("http://localhost:10005")
      c
    }
  })

  private lazy val sequenceTableName = s"$stageTablePrefix-pathManager-sequence"
  private lazy val pathsTableName = s"$stageTablePrefix-pathManager-paths"

  lazy val sequenceTable = dynamoDb.getTable(sequenceTableName)
  lazy val pathsTable = dynamoDb.getTable(pathsTableName)
}

