package com.urdnot.iot.sht10

import com.urdnot.iot.sht10.DataProcessing.parseRecord
import org.scalatest.flatspec.AsyncFlatSpec

class ParseJsonSuite extends AsyncFlatSpec with DataStructures {

  val validJson1: Array[Byte] = """{"temperature_f": 64.52, "timestamp": 1599367914208, "humidity": 78.14, "dew_point": 12.46, "temperature_c": 18.09}""".stripMargin.getBytes("utf-8")
  val validJson2: Array[Byte] = """{"temperature_f": 64.56, "timestamp": 1599367928864, "humidity": 78.31, "dew_point": 12.5, "temperature_c": 18.11}""".stripMargin.getBytes("utf-8")
  val validJson3: Array[Byte] = """{"temperature_f": 64.54, "timestamp": 1599367933734, "humidity": 78.31, "dew_point": 12.5, "temperature_c": 18.1}""".stripMargin.getBytes("utf-8")
  val validJsonReply1: ShtReading =  ShtReading(timestamp = 1599367914208L, dew_point = 12.46, humidity = 78.14, temperature_f = 64.52, temperature_c = 18.09)
  val validJsonReply2: ShtReading =  ShtReading(timestamp = 1599367928864L, dew_point = 12.46, humidity = 78.31, temperature_f = 64.56, temperature_c = 18.11)
  val validJsonReply3: ShtReading =  ShtReading(timestamp = 1599367933734L, dew_point = 12.5, humidity = 78.31, temperature_f = 64.54, temperature_c = 18.1)
  val inValidJson: Array[Byte] = """{"temperature_f: 64.52, "timestamp": 1599367914208, "humidity": 78.14, "dew_point": 12.46, "temperature_c": 18.09}""".getBytes("utf-8")
  val errorReply = Left("""couldn't parse: {"temperature_f: 64.52, "timestamp": 1599367914208, "humidity": 78.14, "dew_point": 12.46, "temperature_c": 18.09} -- io.circe.ParsingFailure: expected : got 'timest...' (line 1, column 26)""")

  behavior of "parsedJson"
  it should "Parse out the JSON data structure from the JSON string" in {
    val futureJson1 = parseRecord(validJson1)
    val futureJson2 = parseRecord(validJson2)
    val futureJson3 = parseRecord(validJson3)
    futureJson1 map { x =>
      assert(x == Right(validJsonReply1))
    }
    futureJson2 map { x =>
      assert(x == Right(validJsonReply2))
    }
    futureJson3 map { x =>
      assert(x == Right(validJsonReply3))
    }
  }
  it should "Return an error when there is a bad JSON string" in {
    val futureJson = parseRecord(inValidJson)
    futureJson map { x =>
      assert(x == errorReply)
    }
  }
}

