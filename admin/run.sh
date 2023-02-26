#!/bin/sh
set -e

cd "$(dirname "$0")"
gradle assemble > /dev/null

. ./.classpath.sh

MAIN="io.kineticedge.ks101.tools.Main"

java -cp "${CP}" $MAIN "$@"
