# iotKafkaSht10Sensor

Working on a refresh

The source data flows from an outdoor sht10 sensor to a Kafka topic. This code picks it up from there, verifies and parses it, then loads it in an InfluxDB for consumption.

##### TODO (8/2020):
1. Upgrade to Scala 2.13.2
2. Swap out Play JSON for circe
3. Improve error handling and future returns (My Akka Strems is pretty basic)
4. Add tests with testcontainers