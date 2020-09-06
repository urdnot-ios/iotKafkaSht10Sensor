package com.urdnot.iot.sht10

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.jawn.decode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataProcessing extends DataStructures {

  def parseRecord(record: Array[Byte]): Future[Either[String, ShtReading]] = Future {
    implicit val decoder: Decoder[ShtReading] = deriveDecoder[ShtReading]
//    println(record.map(_.toChar).mkString)
    decode[ShtReading](record.map(_.toChar).mkString) match {
      case Right(x) => Right(x)
      case Left(x) => Left("couldn't parse: " + record.map(_.toChar).mkString + " -- " + x)
    }
  }
}
