#!/bin/sh
set -e

#
# Initializes database from docker-compose.yml with sample data.
#

cat sample_db.sql | docker exec -i postgres psql -U postgres
