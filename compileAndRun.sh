#! /bin/bash

JETTY_RUNNER_FILE='jetty-runner.jar'
JETTY_RUNNER_URL='https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/9.4.19.v20190610/jetty-runner-9.4.19.v20190610.jar'

mv job.json_IMPORTED job.json

set -e

if ! [ -f "${JETTY_RUNNER_FILE}" ]; then
    echo "Trying to download jetty-runner"
    curl "${JETTY_RUNNER_URL}" > "${JETTY_RUNNER_FILE}"
fi

export HANS_DATABASE_PATH="database_test.db"
export HANS_CONFIGURATION_PATH="job.json"

mvn -DskipTests clean package && java -Xmx300m -Xms300m -jar "${JETTY_RUNNER_FILE}" target/hans.war
