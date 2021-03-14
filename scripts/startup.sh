#!/bin/sh


#
# creates a network unique to this project that can be shared between docker compose instances
# kafka-streams-monitoring -> ksm
#
NETWORK=$(docker network inspect -f '{{.Name}}' ksm 2>/dev/null)
if [ "$NETWORK" != "ksd" ]; then
  (docker network create ksd >/dev/null)
fi

# start up brokers in a controled order to keep IPs as expected -- todo add IP to cluster ?

(cd cluster; docker-compose up -d zookeeper)
(cd cluster; docker-compose up -d broker-1)
(cd cluster; docker-compose up -d broker-2)
(cd cluster; docker-compose up -d broker-3)
(cd cluster; docker-compose up -d broker-4)
sleep 2
(cd cluster; docker-compose up -d)
sleep 2
(cd builder; gradle run)
(cd monitoring; docker-compose up -d)
(cd streams; gradle build)
(cd applications; docker-compose up -d)
(cd publisher; gradle run)
