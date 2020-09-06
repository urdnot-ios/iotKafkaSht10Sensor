package com.urdnot.iot.sht10

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import DataProcessing.parseRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object outdoorSht10 extends  LazyLogging  with DataStructures {
  implicit val system: ActorSystem = ActorSystem("sht10_processor")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val log: Logger = Logger("sht10")


  val consumerConfig: Config = system.settings.config.getConfig("akka.kafka.consumer")
  val envConfig: Config = system.settings.config.getConfig("env")
  val bootstrapServers: String = consumerConfig.getString("kafka-clients.bootstrap.servers")
  val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)

  val INFLUX_URL: String = "http://" + envConfig.getString("influx.host") + ":" + envConfig.getInt("influx.port") + envConfig.getString("influx.route")
  val INFLUX_USERNAME: String = envConfig.getString("influx.username")
  val INFLUX_PASSWORD: String = envConfig.getString("influx.password")
  val INFLUX_DB: String = envConfig.getString("influx.database")
  val INFLUX_MEASUREMENT: String = "sht10"

  implicit val materializer: ActorMaterializer = ActorMaterializer()
    Consumer
    .plainSource(consumerSettings, Subscriptions.topics(envConfig.getString("kafka.topic")))
    .map { consumerRecord =>
      parseRecord(consumerRecord.value())
        .onComplete {
          case Success(x) => x match {
            case Right(valid) =>
              val data = {
                s"""$INFLUX_MEASUREMENT,host=pi-weather,sensor=sht10 dewPointC=${valid.dew_point},dewPointF=${celsiusToFahrenheit(valid.dew_point)},humidity="${valid.humidity}",tempC=${valid.temperature_c},tempF=${valid.temperature_f} ${valid.timestamp}000000""".stripMargin
              }
              val runInflux: Future[HttpResponse] = Http().singleRequest(HttpRequest(
                method = HttpMethods.POST,
                uri = Uri(INFLUX_URL).withQuery(
                  Query(
                    "bucket" -> INFLUX_DB,
                    "precision" -> "ns"
                  )
                ),
                headers = Seq(Authorization(
                  BasicHttpCredentials(INFLUX_USERNAME, INFLUX_PASSWORD))),
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, data)
              ))
              runInflux.onComplete{{
                case Success(res) => res
                case Failure(e)   => sys.error("something went wrong: " + e.getMessage)
              }}
              valid
            case Left(invalid) => println(invalid)
          }
          case Failure(exception) => println(exception)
        }
    }
    .toMat(Sink.ignore)(DrainingControl.apply)
    .run()
}
