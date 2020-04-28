package com.flixdb.core

import java.util.UUID.randomUUID

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.testkit.scaladsl.TestSink
import org.json4s.JsonAST.JString
import org.json4s.{DefaultFormats, JValue}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.{GenericContainer, KafkaContainer}

class TestCdcStreamingToKafka extends AnyFunSuiteLike with BeforeAndAfterAll with ScalaFutures with Matchers {

  import com.typesafe.config.ConfigFactory

  val postgreSQLContainer = {
    val container = new GenericContainer("sebastianharko/postgres104:latest")
    container.addExposedPort(5432)
    container.start()
    container
  }

  val kafkaContainer: KafkaContainer = {
    val container = new KafkaContainer("4.1.2")
    container.start()
    container
  }

  val testConfig = ConfigFactory.parseString(s"""
       |postgres.host = "${postgreSQLContainer.getContainerIpAddress}"
       |postgres.port = ${postgreSQLContainer.getMappedPort(5432)}
       |postgres-cdc.host = "${postgreSQLContainer.getContainerIpAddress}"
       |postgres-cdc.port = ${postgreSQLContainer.getMappedPort(5432)}
       |kafka.bootstrap.servers = "${kafkaContainer.getBootstrapServers}"""".stripMargin)

  val regularConfig = ConfigFactory.load

  val mergedConfig = testConfig.withFallback(regularConfig)

  implicit val system = ActorSystem("flixdb", config = mergedConfig)

  val kafkaSettings = KafkaSettings(system)

  val flixDbConfiguration = FlixDbConfiguration(system)

  val event1: EventEnvelope = EventEnvelope(
    eventId = randomUUID().toString,
    subStreamId = "account-0",
    eventType = "com.megacorp.AccountCreated",
    sequenceNum = 0,
    data = """{"owner": "Silvia Cruz"}""",
    stream = "accounts",
    tags = List("megacorp"),
    timestamp = 42L
  )

  val event2: EventEnvelope = EventEnvelope(
    eventId = randomUUID().toString,
    subStreamId = "account-0",
    eventType = "com.megacorp.AccountUpgraded",
    sequenceNum = 1,
    data = """{"owner": "Silvia Cruz"}""",
    stream = "accounts",
    tags = List("megacorp"),
    timestamp = 43L
  )

  val event3: EventEnvelope = EventEnvelope(
    eventId = randomUUID().toString,
    subStreamId = s"account-0",
    eventType = "com.megacorp.AccountSuspended",
    sequenceNum = 2,
    data = """{"owner": "Silvia Cruz"}""",
    stream = "accounts",
    tags = List("megacorp"),
    timestamp = 42L
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    CdcStreamingToKafka(system)
  }

  test("We can write some events") {
    val postgreSQL = PostgreSQL(system)
    postgreSQL.createTablesIfNotExists("default").futureValue shouldBe Done
    postgreSQL.appendEvents("default", List(event1, event2)).futureValue shouldBe Done
    postgreSQL.appendEvents("default", List(event3)).futureValue shouldBe Done
  }

  test("The events we wrote appear in the change data capture topic in Kafka") {

    Consumer
      .plainSource(
        kafkaSettings.getBaseConsumerSettings.withGroupId("scalatest"),
        Subscriptions.topics(flixDbConfiguration.cdcKafkaStreamName)
      )
      .map(_.value())
      .map {
        case json: String => {
          import org.json4s.jackson._
          implicit val formats = DefaultFormats
          val j: JValue = parseJson(json)
          j
        }
      }
      .runWith(TestSink.probe[JValue])
      .request(3)
      // TODO: add additional checks
      .expectNextChainingPF {
        case j: JValue if (j \ "changeType") == JString("RowInserted") =>
      }
      .expectNextChainingPF {
        case j: JValue if (j \ "changeType") == JString("RowInserted") =>
      }
      .expectNextChainingPF {
        case j: JValue if (j \ "changeType") == JString("RowInserted") =>
      }

  }

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
    kafkaContainer.stop()
    postgreSQLContainer.stop()
  }

}
