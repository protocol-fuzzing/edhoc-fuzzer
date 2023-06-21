#!/usr/bin/env bash

# Adapted from https://github.com/eriptic/uoscore-uedhoc/blob/dev/samples/cert_hierarchy/generate_ca_hierarchy.sh

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly SCRIPT_DIR
BASE_DIR="$(dirname -- "${SCRIPT_DIR}")"
readonly BASE_DIR
readonly CERT_CNF="${SCRIPT_DIR}/cert.cnf"
readonly AUTH_DIR="${BASE_DIR}/experiments/authentication"

# perform everything in AUTH_DIR
cd "${AUTH_DIR}"
export common_name
keep_root_ca_dirs=false

usage() {
  cat << END
  Usage: ${0##*/} [-opt]
  Options:
    -a  Keep generated root_ca directory
    -c  Clean all generated directories
    -h  Show usage message
END
  exit 0
}

cleanup () {
    rm -rf ./root_ca
    rm -rf ./mapper
    rm -rf ./sul
}

mk_root_ca_dirs () {
    cleanup
    mkdir ./root_ca
    mkdir ./root_ca/newcerts
    echo 123456 > ./root_ca/serial
    touch ./root_ca/index.txt
}

rm_root_ca_dirs () {
    if ( ! ${keep_root_ca_dirs} )
    then
        echo "Remove root_ca directory"
        rm -rf ./root_ca
    else
        echo "Keep root_ca directory"
    fi
}

gen_root_ca () {
    # Root CA - self signed
    set -e

    # generate root key
    echo "Generate P-256 private key for root CA"
    openssl ecparam -name prime256v1 -genkey -noout -out root_ca/priv.pem
    # generate csr
    echo "Generate CSR for self-signed root CA certificate"
    common_name=root_ca
    openssl req -config "${CERT_CNF}" -new -sha256 -key root_ca/priv.pem -out root_ca/csr.pem
    # self-sign certificate
    echo "Generate self signed root CA certificate"
    openssl req -x509 -sha256 -days 365 -key root_ca/priv.pem -in root_ca/csr.pem \
                -out root_ca/x509_cert.pem
    echo "Remove CSR for self-signed root CA certificate"
    rm root_ca/csr.pem
    echo ----------------

    set +e
}

gen_x509_cert () {
    set -e

    tgt_dir="${1}"
    # generate csr
    echo "Generate CSR for x509 certificate"
    common_name="${2}"
    openssl req -config "${CERT_CNF}" -new -sha256 -key "${tgt_dir}"/priv.pem -out "${tgt_dir}"/csr.pem
    # sign x509 certificate
    echo "Generate x509 certificate"
    openssl ca -config "${CERT_CNF}" -days 365 -notext -md sha256 -batch -in "${tgt_dir}"/csr.pem \
               -out "${tgt_dir}"/x509_cert.pem
    openssl x509 -in "${tgt_dir}"/x509_cert.pem -outform der -out "${tgt_dir}"/x509_cert.der
    # remove csr
    echo "Remove CSR for x509 certificate"
    rm "${tgt_dir}"/csr.pem

    set +e
}

gen_x509_cert_for_x25519 () {
    set -e

    tgt_dir="${1}"
    # generate temp ed25519 private key for signing
    echo "Generate temporary ed25519 private key"
    openssl genpkey -algorithm ed25519 -out "${tgt_dir}"/temp_priv.pem
    # generate csr
    echo "Generate CSR for x509 certificate"
    common_name="${2}"
    openssl req -config "${CERT_CNF}" -new -sha256 -key "${tgt_dir}"/temp_priv.pem -out "${tgt_dir}"/csr.pem
    # sign x509 certificate
    echo "Generate x509 certificate"
    openssl x509 -req -in "${tgt_dir}"/csr.pem -CA root_ca/x509_cert.pem -CAkey root_ca/priv.pem \
                 -CAserial root_ca/serial -force_pubkey "${tgt_dir}"/pub.pem -out "${tgt_dir}"/x509_cert.pem
    openssl x509 -in "${tgt_dir}"/x509_cert.pem -outform der -out "${tgt_dir}"/x509_cert.der
    # remove csr
    echo "Remove CSR for x509 certificate"
    rm "${tgt_dir}"/csr.pem
    # remove temp ed25519 private key
    rm "${tgt_dir}"/temp_priv.pem

    set +e
}

gen_ed25519 () {
    set -e

    tgt_dir="${1}"
    cn="${2}"
    # generate private key
    echo "Generate private key"
    openssl genpkey -algorithm ed25519 -out "${tgt_dir}"/priv.pem
    openssl pkey -in "${tgt_dir}"/priv.pem -outform der -out "${tgt_dir}"/priv.der

    # generate public key
    echo "Generate public key"
    openssl pkey -in "${tgt_dir}"/priv.pem -pubout -out "${tgt_dir}"/pub.pem
    openssl pkey -in "${tgt_dir}"/priv.pem -pubout -outform der -out "${tgt_dir}"/pub.der

    # generate x509 certificate
    gen_x509_cert "${tgt_dir}" "${cn}"

    set +e
}

gen_p256 () {
    set -e

    tgt_dir="${1}"
    cn="${2}"
    # generate private key
    echo "Generate private key"
    openssl ecparam -name secp256r1 -genkey -out "${tgt_dir}"/priv.pem
    openssl pkcs8 -topk8 -nocrypt -in "$p256_dir"/priv.pem -out "${tgt_dir}"/npriv.pem
    mv "${tgt_dir}"/npriv.pem "${tgt_dir}"/priv.pem
    openssl pkcs8 -topk8 -nocrypt -in "${tgt_dir}"/priv.pem -outform der -out "${tgt_dir}"/priv.der

    # generate public key
    echo "Generate public key"
    openssl ec -in "${tgt_dir}"/priv.pem -pubout -out "${tgt_dir}"/pub.pem
    openssl ec -in "${tgt_dir}"/priv.pem -pubout -outform der -out "${tgt_dir}"/pub.der

    # generate x509 certificate
    gen_x509_cert "${tgt_dir}" "${cn}"

    set +e
}

gen_x25519 () {
    set -e

    tgt_dir="${1}"
    cn="${2}"
    # generate private key
    echo "Generate private key"
    openssl genpkey -algorithm x25519 -out "${tgt_dir}"/priv.pem
    openssl pkey -in "${tgt_dir}"/priv.pem -outform der -out "${tgt_dir}"/priv.der

    # generate public key
    echo "Generate public key"
    openssl pkey -in "${tgt_dir}"/priv.pem -pubout -out "${tgt_dir}"/pub.pem
    openssl pkey -in "${tgt_dir}"/priv.pem -pubout -outform der -out "${tgt_dir}"/pub.der

    # generate x509 certificate
    gen_x509_cert_for_x25519 "${tgt_dir}" "${cn}"

    set +e
}

gen_all () {
    name="${1}"
    tgt_outer_dir="${2}"

    # SIG
    tgt_sig_dir=${tgt_outer_dir}/sig
    mkdir -p "${tgt_sig_dir}"

    # ed25519
    ed25519_dir=${tgt_sig_dir}/ed25519
    mkdir "${ed25519_dir}"

    echo "Generate ed25519 keys and x509 certificate for ${name}"
    gen_ed25519 "${ed25519_dir}" "${name}"_ed25519_sig
    echo ----------------

    # p256
    p256_dir=${tgt_sig_dir}/p256
    mkdir "${p256_dir}"

    echo "Generate p256 keys and x509 certificate (SIG) for ${name}"
    gen_p256 "${p256_dir}" "${name}"_p256_sig
    echo ----------------

    # STAT
    tgt_stat_dir=${tgt_outer_dir}/stat
    mkdir -p "${tgt_stat_dir}"

    # x25519
    x25519_dir=${tgt_stat_dir}/x25519
    mkdir "${x25519_dir}"

    echo "Generate x25519 keys and x509 certificate for ${name}"
    gen_x25519 "${x25519_dir}" "${name}"_x25519_stat
    echo ----------------

    # p256
    p256_dir=${tgt_stat_dir}/p256
    mkdir "${p256_dir}"

    echo "Generate p256 keys and x509 certificate (STAT) for ${name}"
    gen_p256 "${p256_dir}" "${name}"_p256_stat
    echo ----------------
}


while getopts :ach flag
do
  case "${flag}" in
    a) keep_root_ca_dirs=true ;;
    c) cleanup; echo "Cleaned all directories"; exit 0 ;;
    : | \? | h | *) usage ;;
  esac
done

mk_root_ca_dirs

# Root CA
gen_root_ca

# Mapper
gen_all mapper mapper

# SUL
gen_all sul sul

rm_root_ca_dirs
