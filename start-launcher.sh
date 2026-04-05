#!/bin/bash

set -e

MODE="${1:-local}"
shift || true

echo "=========================================="
echo "Lancement du launcher SAE401"
echo "Mode: $MODE"
echo "=========================================="

mvn clean install -pl common,serverSide,clientSide,launcher -am -DskipTests

# Lancement
mvn -f launcher/pom.xml exec:java -Dexec.mainClass="fr.iutgon.sae401.launcher.LauncherMain" -Dexec.args="$MODE $*"