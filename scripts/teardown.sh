#!/bin/sh

(cd applications; docker-compose down)
(cd monitoring; docker-compose down)
(cd cluster; docker-compose down)
#docker network rm ksd
