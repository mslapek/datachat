#!/bin/sh
set -ex

#
# Build all docker images
#

docker build -t datachat-webserver .
(cd web && docker build -t datachat-web .)
