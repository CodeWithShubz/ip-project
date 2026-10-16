#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo "Error: .env file is missing."
    exit 1
fi

set -a
source .env
set +a

sudo systemctl start mysql
exec mvn spring-boot:run
