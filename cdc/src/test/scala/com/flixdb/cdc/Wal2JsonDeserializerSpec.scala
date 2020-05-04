package com.flixdb.cdc

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers

class Wal2JsonDeserializerSpec extends AnyFunSuite with matchers.should.Matchers {

  import Wal2Json._
  import spray.json._

  test("Can deserialize basic Wal2Json output from insert") {

    val sampleOutput1 =
      """|{
         | "timestamp": "2020-05-03 18:36:24.960302+00",
         | "change": [
         |   {
         |     "kind": "insert",
         |     "schema": "public",
         |     "table": "users",
         |     "columnnames": ["id", "name"],
         |     "columntypes": ["integer", "character varying(255)"],
         |     "columnvalues": [1, "Hello"]
         |   }
         | ]
         |}""".stripMargin

    val json = sampleOutput1.parseJson

    val wal2JsonR00t =
      json.convertTo[Wal2JsonR00t]

    wal2JsonR00t.timestamp shouldBe an[Instant]
    wal2JsonR00t.change.size shouldBe 1
    val change = wal2JsonR00t.change.head
    change.kind shouldBe "insert"
    change.schema shouldBe "public"
    change.table shouldBe "users"
    change.columnNames shouldBe List("id", "name")
    change.columnTypes shouldBe List("integer", "character varying(255)")
    change.columnValues shouldBe List("1", "Hello")
    change.oldKeys shouldBe OldKeys()
  }

  test("Can deserialize a more complicated Wal2Json output from insert") {

    val sampleOutput2 =
      """
        |{
        | "timestamp": "2020-05-03 19:39:13.616952+00",
        | "change":[
        |   {
        |     "kind": "insert",
        |     "schema": "public",
        |     "table": "users",
        |     "columnnames":
        |       ["id", "name", "is_person", "tags", "other_names"],
        |     "columntypes":
        |       ["integer", "character varying(255)", "boolean", "character varying(255)", "character varying[]"],
        |     "columnvalues":[1, "Seb" ,true, "null", "{Sebastian}"]
        |   }
        | ]
        |}""".stripMargin
    val json = sampleOutput2.parseJson

    val wal2JsonR00t =
      json.convertTo[Wal2JsonR00t]

    wal2JsonR00t.timestamp shouldBe an[Instant]
    wal2JsonR00t.change.size shouldBe 1
    val change = wal2JsonR00t.change.head
    change.kind shouldBe "insert"
    change.schema shouldBe "public"
    change.table shouldBe "users"
    change.columnNames shouldBe List("id", "name", "is_person", "tags", "other_names")
    change.columnTypes.size shouldBe 5
    change.columnValues.size shouldBe 5
    change.columnValues shouldBe List("1", "Seb", "true", "null", "{Sebastian}")
    change.oldKeys shouldBe OldKeys()
  }

  test("Can deserialize Wal2Json output from delete") {
    val output = """{
                  |  "timestamp": "2020-05-03 21:57:56.482981+00",
                  |  "change": [
                  |    {
                  |      "kind": "delete",
                  |      "schema": "public",
                  |      "table": "users",
                  |      "oldkeys": {
                  |        "keynames": [
                  |          "id",
                  |          "name",
                  |          "is_person",
                  |          "other_names"
                  |        ],
                  |        "keytypes": [
                  |          "integer",
                  |          "character varying(255)",
                  |          "boolean",
                  |          "character varying[]"
                  |        ],
                  |        "keyvalues": [
                  |          1,
                  |          "Sebastian",
                  |          true,
                  |          "{Sebastian}"
                  |        ]
                  |      }
                  |    }
                  |  ]
                  |}""".stripMargin

    val json = output.parseJson

    val wal2JsonR00t =
      json.convertTo[Wal2JsonR00t]

    wal2JsonR00t.timestamp shouldBe an[Instant]
    wal2JsonR00t.change.size shouldBe 1
    val change = wal2JsonR00t.change.head
    change.kind shouldBe "delete"
    change.schema shouldBe "public"
    change.table shouldBe "users"
    change.columnNames shouldBe Nil
    change.columnTypes shouldBe Nil
    change.columnValues shouldBe Nil
    change.oldKeys.keyValues shouldBe List("1", "Sebastian", "true", "{Sebastian}")
    change.oldKeys.keyNames shouldBe List("id", "name", "is_person", "other_names")
    change.oldKeys.keyTypes should have size 4

  }

}