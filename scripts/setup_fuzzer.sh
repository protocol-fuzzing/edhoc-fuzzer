#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly SCRIPT_DIR
BASE_DIR="$(dirname -- "${SCRIPT_DIR}")"
readonly BASE_DIR

setup_psf() {
    # setup protocol-state-fuzzer library

    set -e
    cd "${BASE_DIR}"
    git clone "https://github.com/protocol-fuzzing/protocol-state-fuzzer.git"
    cd protocol-state-fuzzer
    mvn install

    cd "${BASE_DIR}"
    rm -rf ./protocol-state-fuzzer/
    set +e
}

setup_cf_edhoc() {
    # setup cf-edhoc library

    readonly PATCH_FILE="${SCRIPT_DIR}/cf-edhoc.patch"
    readonly COMMIT_HASH="d728368ac44dabceff2b4a2c5fcd757552e65f9e"

    set -e
    cd "${BASE_DIR}"
    git clone "https://github.com/rikard-sics/californium.git"
    cd californium
    git checkout edhoc
    git checkout ${COMMIT_HASH}
    git apply "${PATCH_FILE}"
    mvn package -DskipTests -am -pl cf-edhoc
    JAR_FILE=$(ls ./cf-edhoc/target/cf-edhoc-*-SNAPSHOT.jar)

    mvn install:install-file \
        -Dfile="${JAR_FILE}" \
        -DgroupId=se.ri.org.eclipse.californium \
        -DartifactId=cf-edhoc \
        -Dversion=0.0.0 \
        -Dpackaging=jar

    cd "${BASE_DIR}"
    rm -rf ./californium/
    set +e
}

setup_fuzzer() {
    # package project and create symlink

    set -e
    cd "${BASE_DIR}"
    mvn clean package
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
