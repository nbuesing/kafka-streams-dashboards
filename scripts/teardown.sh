#!/bin/sh

alias dc='docker compose'

(cd applications; dc down)
(cd monitoring; dc down)
(cd cluster; dc down)
#docker network rm ksd
