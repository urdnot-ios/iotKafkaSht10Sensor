package com.urdnot.iot

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.{Database, InfluxDB, Point}
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import com.urdnot.iot.dataObjects.shtReading
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContextExecutor, Future}


object outdoorSht10 extends Directives with utils with LazyLogging {
  implicit val system: ActorSystem = ActorSystem("sht10_processor")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = materializer.executionContext
  val log: Logger = Logger("sht10")


  val consumerConfig: Config = system.settings.config.getConfig("akka.kafka.consumer")
  val envConfig: Config = system.settings.config.getConfig("env")
  val bootstrapServers: String = consumerConfig.getString("kafka-clients.bootstrap.servers")
  val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)

  //Influxdb
  val influxdb: InfluxDB = InfluxDB.connect(envConfig.getString("influx.host"), envConfig.getInt("influx.port"))
  val database: Database = influxdb.selectDatabase(envConfig.getString("influx.database"))

  Consumer.committableSource(consumerSettings, Subscriptions.topics(envConfig.getString("kafka.topic")))
    .runForeach { x =>
      val record = x.record.value()
      val rawJson = record.map(_.toChar).mkString.replace("\'", "\"").replace("L", "")
      val parsedJson = Json.parse(rawJson)
      val sensor = "sht10"
      val host = "pi-weather"

      val jsonRecord = shtReading(
        (parsedJson \ "timestamp").asOpt[Long],
        (parsedJson \ "dew_point").asOpt[Double],
        (parsedJson \ "humidity").asOpt[Double],
        (parsedJson \ "temperature_f").asOpt[Double],
        (parsedJson \ "temperature_c").asOpt[Double]
      )

      val pointSht10 = Point(sensor, jsonRecord.timeStamp.get)
        .addTag("sensor", sensor)
        .addTag("host", host)
        .addField("tempF", jsonRecord.temperatureFarenheit.get)
        .addField("tempC", jsonRecord.temperatureCelcius.get)
        .addField("humidity", jsonRecord.humidity.get)
        .addField("dewPointF", celsiusToFahrenheit(jsonRecord.dewPoint.get))
        .addField("dewPointC", jsonRecord.dewPoint.get)
      Future(database.write(pointSht10, precision = Precision.MILLISECONDS))

    }
}
