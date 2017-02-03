package services

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.{DescribeTagsRequest, Filter}
import com.amazonaws.util.EC2MetadataUtils

import scala.collection.JavaConverters._


object AWS {

  val region = "eu-west-1"

  lazy val EC2Client = AmazonEC2ClientBuilder.standard().withRegion(region).build()
  lazy val CloudWatch = AmazonCloudWatchAsyncClientBuilder.standard().withRegion(region).build()

}

trait AwsInstanceTags {
  lazy val instanceId = Option(EC2MetadataUtils.getInstanceId)

  def readTag(tagName: String) = {
    instanceId.flatMap { id =>
      val tagsResult = AWS.EC2Client.describeTags(
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

