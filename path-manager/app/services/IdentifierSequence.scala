package services

import com.amazonaws.services.dynamodbv2.document.{Item, AttributeUpdate}
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.model.ReturnValue

object IdentifierSequence {

  def getNextId: Long = {
    val incResult = Dynamo.sequenceTable.updateItem(
      new UpdateItemSpec().withPrimaryKey("sequenceName", "ids").withAttributeUpdate(new AttributeUpdate("value").addNumeric(1)).withReturnValues(ReturnValue.ALL_NEW)
    )
    incResult.getItem.getLong("value")
  }

  // debug methods
  def getCurrentId: Long = {
    Dynamo.sequenceTable.getItem("sequenceName", "ids").getLong("value")
  }

  def setCurrentId(v: Long) = {
    Dynamo.sequenceTable.putItem(new Item().withString("sequenceName", "ids").withLong("value", v))
  }
}
