package services

import com.whisk.docker.impl.spotify.DockerKitSpotify
import com.whisk.docker.scalatest.DockerTestKit
import com.whisk.docker.{DockerContainer, DockerKit}
import org.scalatest.TestSuite

trait DockerDynamoTestBase extends TestSuite with DockerKit with DockerTestKit with DockerKitSpotify {

  private val dynamoContainer = DockerContainer("amazon/dynamodb-local")
    .withPorts(8000-> Some(Dynamo.LOCAL_PORT))

  override def dockerContainers: List[DockerContainer] = dynamoContainer :: super.dockerContainers

}
