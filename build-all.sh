#!/bin/bash -e

# run in sequences the different maven calls needed to fully build Argus IDE from scratch

# not as good as the readlink version, but it is not working on os X
ROOT_DIR=$(dirname $0)
cd ${ROOT_DIR}
ROOT_DIR=${PWD}

if [ -z "$*" ]
then
  MVN_ARGS="-Pscala-2.11.x -Peclipse-luna -Dscala.version=2.11.6 -e clean install"
  MVN_P2_ARGS="-Dtycho.localArtifacts=ignore -Pscala-2.11.x -Peclipse-luna -Dscala.version=2.11.6 -e clean verify"
else
  MVN_ARGS="$*"
  MVN_P2_ARGS="-Dtycho.localArtifacts=ignore $*"
fi

echo "Running with: mvn3 ${MVN_P2_ARGS}"

# the parent project
echo "Building parent project in $ROOT_DIR"
cd ${ROOT_DIR}
mvn3 ${MVN_ARGS}

# set custom configuration files
echo "Setting custom configuration files"
mvn3 ${MVN_ARGS} -Pset-version-specific-files antrun:run

# set the versions if required
cd ${ROOT_DIR}
if [ -n "${SET_VERSIONS}" ]
then
  echo "setting versions"
  mvn3 ${MVN_P2_ARGS} -Pset-versions exec:java
else
  echo "Not running UpdateArgusIDEManifests."
fi

# the plugins
echo "Building plugins"
cd ${ROOT_DIR}/org.argus-ide.cit.build
mvn3 ${MVN_P2_ARGS}

