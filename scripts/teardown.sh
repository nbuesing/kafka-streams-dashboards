#!/bin/sh

alias dc='docker compose'

(cd applications; dc down -v)
(cd monitoring; dc down -v)
(cd cluster; dc down -v)
#docker network rm ksd
