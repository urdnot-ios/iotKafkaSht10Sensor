#!/usr/bin/env bash

# did you change the version number?

sbt clean
sbt assembly
sbt docker:publishLocal
docker image tag iotkafkasht10sensor intel-server-03:5000/iotkafkasht10sensor/latest
ssh -t appuser@intel-server-03 sudo docker stop sht10
ssh -t appuser@intel-server-03 sudo docker rm sht10
ssh -t appuser@intel-server-03 sudo docker image rmi intel-server-03:5000/iotkafkasht10sensor/latest
docker image push intel-server-03:5000/iotkafkasht10sensor:latest
ssh -t appuser@intel-server-03 sudo docker run -m 300m --name=sht10 --network=host --restart=always -e KAFKA_SERVERS=pi-server-03:9092,intel-server-01:9092,intel-server-04:9092 -e TOPIC_START=latest -e INFLUX_HOST=intel-server-03 -e KAFKA_CONSUMER_GROUP=connectorGroup -d intel-server-03:5000/iotkafkasht10sensor/latest
