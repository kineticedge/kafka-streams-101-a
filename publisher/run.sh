#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ks101.publisher.Main"

java -cp "${CP}" $MAIN "$@"
