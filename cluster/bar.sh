#!/usr/bin/env bash

DOCKER_HOME=~/Library/Containers/com.docker.docker/Data/docker.sock

find "$DOCKER_HOME" -mindepth 1 -maxdepth 1 -type d | grep -vFf <(
  docker ps -aq | xargs docker inspect | jq -r '.[]|.Mounts|.[]|.Name|select(.)'
)
