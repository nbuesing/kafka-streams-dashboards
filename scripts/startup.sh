#!/bin/sh

alias d='docker'
alias dc='docker compose'
alias dn='docker network'

if ! [ -x "$(command -v docker)" ]; then
    echo "docker is not installed." >&2
    exit 1
fi

#if ! [ -x "$(command -v dc)" ]; then
#    echo "dc is not installed." >&2
#    exit 1
#fi

docker info > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "docker server is not running." >&2
  exit
fi

#
# creates a network unique to this project that can be shared between docker compose instances
# kafka-streams-dashboard -> ksd
#
NETWORK=$(docker network inspect -f '{{.Name}}' ksd 2>/dev/null)
if [ "$NETWORK" != "ksd" ]; then
  (docker network create ksd >/dev/null)
fi

(cd cluster; dc up -d --wait)

./gradlew build

(cd builder; ../gradlew run)
(cd monitoring; dc up -d)
(cd applications; dc up -d stream)
sleep 1
(cd applications; dc up -d)

if [ $(uname) == "Darwin" ]; then
  open http://localhost:3000
fi

(cd publisher; ../gradlew run)
