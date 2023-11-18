#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly SCRIPT_DIR
BASE_DIR="$(dirname -- "${SCRIPT_DIR}")"
readonly BASE_DIR
readonly MODEL_DIR="${BASE_DIR}/experiments/models"
readonly SOURCES_DIR="${MODEL_DIR}/sources"
readonly PATCH_DIR="${MODEL_DIR}/patches"
readonly SERVERS_DIR="${MODEL_DIR}/servers"
readonly CLIENTS_DIR="${MODEL_DIR}/clients"

mkdir -p "${SOURCES_DIR}" "${SERVERS_DIR}" "${CLIENTS_DIR}"

setup_edhoc_rs() {
  # edhoc-rs
  COMMIT_HASH="337c47ca684b60f5dbf09828ec7705afac0e79f4"

  set -e
  echo "Setting up EDHOC-Rust in ${SOURCES_DIR}"
  cd "${SOURCES_DIR}"
  git clone https://github.com/openwsn-berkeley/edhoc-rs.git
  cd edhoc-rs
  git checkout ${COMMIT_HASH}
  cargo build --release
  TGT_DIR=${SOURCES_DIR}/edhoc-rs/target/release

  cd "${MODEL_DIR}"
  mkdir -p "${SERVERS_DIR}"/edhoc-rs "${CLIENTS_DIR}"/edhoc-rs
  ln -sf "${TGT_DIR}"/coapserver "${SERVERS_DIR}"/edhoc-rs/coapserver
  ln -sf "${TGT_DIR}"/coapclient "${CLIENTS_DIR}"/edhoc-rs/coapclient
  echo "Successfully set up EDHOC-Rust"
  set +e
}

setup_rise() {
  # rise
  COMMIT_HASH="d728368ac44dabceff2b4a2c5fcd757552e65f9e"
  PREFIX="${SOURCES_DIR}/californium/cf-edhoc/src"
  POSTFIX="java/org/eclipse/californium/edhoc"
  CF_EDHOC_MAIN_DIR="${PREFIX}/main/${POSTFIX}"
  CF_EDHOC_TEST_DIR="${PREFIX}/test/${POSTFIX}"
  APP_PROFILE_BUILDER="${CF_EDHOC_TEST_DIR}/AppProfileBuilder.java"
  EDHOC_CLIENT="${CF_EDHOC_TEST_DIR}/EdhocClient.java"
  EDHOC_SERVER="${CF_EDHOC_TEST_DIR}/EdhocServer.java"

  set -e
  echo "Setting up RISE in ${SOURCES_DIR}"
  cd "${SOURCES_DIR}"
  git clone https://github.com/rikard-sics/californium.git
  cd californium
  git checkout edhoc
  git checkout ${COMMIT_HASH}
  git apply "${PATCH_DIR}"/rise.patch
  cp "${APP_PROFILE_BUILDER}" "${EDHOC_CLIENT}" "${EDHOC_SERVER}" "${CF_EDHOC_MAIN_DIR}"
  mvn package -DskipTests -am -pl cf-edhoc

  cd "${MODEL_DIR}"
  mkdir -p "${SERVERS_DIR}"/rise "${CLIENTS_DIR}"/rise
  APP_JAR=$(ls "${SOURCES_DIR}"/californium/cf-edhoc/target/cf-edhoc-*-SNAPSHOT.jar)
  ln -sf "${APP_JAR}" "${SERVERS_DIR}"/rise/cf-edhoc.jar
  ln -sf "${APP_JAR}" "${CLIENTS_DIR}"/rise/cf-edhoc.jar
  echo "Successfully set up RISE"
  set +e
}

setup_sifis_home() {
  # sifis-home
  COMMIT_HASH="9956c8cf9a6f8cb3ab09e48842ceafeb9d2a790e"

  set -e
  echo "Setting up SIFIS-HOME in ${SOURCES_DIR}"
  cd "${SOURCES_DIR}"
  git clone https://github.com/sifis-home/wp3-solutions.git
  SF_HOME_DIR="${SOURCES_DIR}/wp3-solutions"

  cd "${SF_HOME_DIR}"
  git checkout ${COMMIT_HASH}
  git apply "${PATCH_DIR}"/sifis-home.patch

  # install in local repository, in order for the edhoc-applications'
  # dependencies to be found
  cd "${SF_HOME_DIR}"/californium-extended
  mvn install -DskipTests -am -pl cf-edhoc

  cd "${SF_HOME_DIR}"/edhoc-applications
  mvn -DskipTests compile assembly:single

  cd "${MODEL_DIR}"
  mkdir -p "${SERVERS_DIR}"/sifis-home "${CLIENTS_DIR}"/sifis-home
  APP_JAR=$(ls "${SF_HOME_DIR}"/edhoc-applications/target/*-jar-with-dependencies.jar)
  ln -sf "${APP_JAR}" "${SERVERS_DIR}"/sifis-home/edhoc-applications.jar
  ln -sf "${APP_JAR}" "${CLIENTS_DIR}"/sifis-home/edhoc-applications.jar
  echo "Successfully set up SIFIS-HOME"
  set +e
}

setup_uoscore_uedhoc() {
  # uoscore-uedhoc
  COMMIT_HASH="9c18a3503ad905e79e2dbe847cb14c1650524eee"

  set -e
  echo "Setting up uOSCORE-uEDHOC in ${SOURCES_DIR}"
  cd "${SOURCES_DIR}"
  git clone --recurse-submodules https://github.com/eriptic/uoscore-uedhoc.git
  UOE_DIR="${SOURCES_DIR}/uoscore-uedhoc"
  cd "${UOE_DIR}"
  git checkout ${COMMIT_HASH}
  git apply "${PATCH_DIR}"/uoscore-uedhoc.patch
  make
  make -C samples/linux_edhoc/initiator
  make -C samples/linux_edhoc/responder
  make -C samples/linux_edhoc_oscore/initiator_client
  make -C samples/linux_edhoc_oscore/responder_server

  cd "${MODEL_DIR}"
  mkdir -p "${SERVERS_DIR}"/uoscore-uedhoc "${CLIENTS_DIR}"/uoscore-uedhoc
  SAMPLES_DIR="${UOE_DIR}/samples"
  LE_DIR="${SAMPLES_DIR}/linux_edhoc"
  LEO_DIR="${SAMPLES_DIR}/linux_edhoc_oscore"
  ln -sf "${LE_DIR}"/responder/build/responder "${SERVERS_DIR}"/uoscore-uedhoc/linux-edhoc-responder
  ln -sf "${LE_DIR}"/initiator/build/initiator "${CLIENTS_DIR}"/uoscore-uedhoc/linux-edhoc-initiator
  ln -sf "${LEO_DIR}"/responder_server/build/responder_server "${SERVERS_DIR}"/uoscore-uedhoc/linux-edhoc-oscore-responder-server
  ln -sf "${LEO_DIR}"/initiator_client/build/initiator_client "${CLIENTS_DIR}"/uoscore-uedhoc/linux-edhoc-oscore-initiator-client
  echo "Successfully set up uOSCORE-uEDHOC"
  set +e
}

setup_all_suls() {
  setup_edhoc_rs
  setup_rise
  setup_sifis_home
  setup_uoscore_uedhoc
  exit 0
}

usage() {
  cat << END
  Usage: ${0##*/} [option] SUL
  Options:
    -a, --all  : Try to setup all SULs
    -h, --help : Show usage
  SULs:
    edhoc-rs
    rise
    sifis-home
    uoscore-uedhoc
END
  exit 0
}

if [[ ${#} = 0 ]]
then
  usage
else
  while [[ "${1}" =~ ^- ]]; do case ${1} in
    -a | --all ) setup_all_suls ;;
    -h | --help) usage ;;
    * ) echo "Unsupported option ${1}"; usage ;;
  esac done

  for sul in "${@}"; do case ${sul} in
    edhoc-rs ) setup_edhoc_rs ;;
    rise ) setup_rise ;;
    sifis-home ) setup_sifis_home ;;
    uoscore-uedhoc ) setup_uoscore_uedhoc ;;
    * ) echo "SUL not recognized: ${sul}" ;;
  esac done
fi
