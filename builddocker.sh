#!/bin/sh
set -ex

#
# Build all docker images
#

docker build -t datachat .
docker build -t datachat-web -f ./web/Dockerfile .
