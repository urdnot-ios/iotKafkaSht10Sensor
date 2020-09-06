#!/bin/zsh

# did you change the version number?

sbt clean
sbt assembly
sbt docker:publishLocal
docker image tag iotkafkasht10sensor:latest intel-server-03:5000/iotkafkasht10sensor
docker image push intel-server-03:5000/iotkafkasht10sensor
# Server side:
# kubectl apply -f /home/appuser/deployments/iotKafkaSht10.yaml
# If needed:
# kubectl delete deployment iot-kafka-windvane
# For troubleshooting
# kubectl exec --stdin --tty iot-kafka-windvane -- /bin/bash