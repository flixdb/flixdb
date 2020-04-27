package com.flixdb.core

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class TestJsonSupport extends JsonSupport with AnyFunSuiteLike with BeforeAndAfterAll with Matchers {

  import spray.json._

  test("DTOs: we can serialize an 'Event'") {

    val e1 = Dtos.Event(
      eventId = "1af2948a-d4dd-48b0-8ca0-cb0fe7562b3d",
      eventType = "com.megacorp.AccountCreated",
      entityId = "account-123",
      sequenceNum = 1,
      data = """{"owner":"John Smith"}""",
      stream = "account-events",
      tags = List("megacorp-events"),
      timestamp = 42L
    )

    val json: JsValue = e1.toJson
    val r = json.compactPrint;
    r shouldBe """{"data":{"owner":"John Smith"},"entityId":"account-123","eventId":"1af2948a-d4dd-48b0-8ca0-cb0fe7562b3d","eventType":"com.megacorp.AccountCreated","sequenceNum":1,"stream":"account-events","tags":["megacorp-events"],"timestamp":42}"""
  }

  test("DTOs: we can deserialize json to 'PostEvent'") {

    val json: JsValue =
      """|{"data":{"owner":"John Smith"},  
         |"eventType":"com.megacorp.AccountCreated",
         |"eventId":"1af2948a-d4dd-48b0-8ca0-cb0fe7562b3d",
         |"sequenceNum":1,   
         |"tags":["megacorp-events"]
         |}""".stripMargin.parseJson

    val data: JsObject = json.asJsObject.fields("data").asJsObject
    data.fields("owner") shouldBe JsString("John Smith")

    val e = json.convertTo[Dtos.PostEvent]

    e.eventId shouldBe "1af2948a-d4dd-48b0-8ca0-cb0fe7562b3d"
    e.eventType shouldBe "com.megacorp.AccountCreated"
    e.sequenceNum shouldBe 1
    e.data shouldBe """{"owner":"John Smith"}"""
    e.tags shouldBe Option(List("megacorp-events"))
  }

}