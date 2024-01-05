#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly SCRIPT_DIR
BASE_DIR="$(dirname -- "${SCRIPT_DIR}")"
readonly BASE_DIR

setup_psf() {
    # setup protocol-state-fuzzer library

    COMMIT_HASH="86da423ac2269f5d7b61503ae8b8c2f45a37f570"

    set -e
    cd "${BASE_DIR}"
    git clone "https://github.com/protocol-fuzzing/protocol-state-fuzzer.git"
    cd protocol-state-fuzzer
    git checkout ${COMMIT_HASH}
    mvn install

    cd "${BASE_DIR}"
    rm -rf ./protocol-state-fuzzer/
    set +e
}

setup_cf_edhoc() {
    # setup cf-edhoc library

    PATCH_FILE="${SCRIPT_DIR}/cf-edhoc.patch"
    COMMIT_HASH="4cacfe37d5e213d03c0ab0a8dacaaeb156a704d2"

    set -e
    cd "${BASE_DIR}"
    git clone "https://github.com/rikard-sics/californium.git"
    cd californium
    git checkout edhoc
    git checkout ${COMMIT_HASH}
    git apply "${PATCH_FILE}"
    mvn package -DskipTests -am -pl cf-edhoc
    JAR_FILE=$(ls ./cf-edhoc/target/cf-edhoc-*-SNAPSHOT.jar)
    POM_FILE=$(ls ./pom.xml)

    mvn install:install-file \
        -Dfile="${JAR_FILE}" \
        -DgroupId=se.ri.org.eclipse.californium \
        -DartifactId=cf-edhoc \
        -Dversion=0.0.0 \
        -Dpackaging=jar \
        -DpomFile="${POM_FILE}"

    cd "${BASE_DIR}"
    rm -rf ./californium/
    set +e
}

setup_fuzzer() {
    # package project and create symlink

    set -e
    cd "${BASE_DIR}"
    mvn clean verify
    ln -sf "${BASE_DIR}"/target/edhoc-fuzzer-*-jar-with-dependencies.jar edhoc-fuzzer.jar
    set +e
}

usage() {
  cat << END
  Usage: ${0##*/} [-opt]
  Options (library setup prior to EDHOC-Fuzzer):
    -p  Fetch and setup only protocol-state-fuzzer library
    -e  Fetch and setup only cf-edhoc library
    -l  Fetch and setup protocol-state-fuzzer and cf-edhoc libraries
    -h  Show usage message
END
  exit 0
}


while getopts :pelh flag
do
  case "${flag}" in
    p) setup_psf ;;
    e) setup_cf_edhoc ;;
    l) setup_psf; setup_cf_edhoc ;;
    : | \? | h | *) usage ;;
  esac
done

setup_fuzzer
