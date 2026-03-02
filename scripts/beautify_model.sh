#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
readonly SCRIPT_DIR
REPL_FILE="${SCRIPT_DIR}/replacements.txt"
REPL_FILE_RA="${SCRIPT_DIR}/replacementsRA.txt"
readonly PY_SCRIPT="${SCRIPT_DIR}/beautify_model.py"

start_edge_label=""
RA_FLAG=""
# uncomment next line to remove all edges with transitions '_ / UNSUPPORTED_MESSAGE'
# remove_edge_pattern="o_UNSUPPORTED_MESSAGE"

if [ ${#} = 0 ]; then
    echo "Usage: ${0##*/} [-cI|--clientInitiator] [-cR|--clientResponder] [-RA|--registerAutomata] dot_model"
else
    while [[ "${1}" =~ ^- ]]; do case ${1} in
        -cI | --clientInitiator )
            start_edge_label='"TIMEOUT / EDHOC_MESSAGE_1"'
            ;;
        -cR | --clientResponder )
            start_edge_label='"TIMEOUT / COAP_EMPTY_MESSAGE"'
            ;;
        -RA | --registerAutomata )
            REPL_FILE=${REPL_FILE_RA}
            RA_FLAG='--register-automata'
            ;;
        * )
            echo "Unsupported option ${1}"
            ;;
        esac
        shift
    done

    if [[ ! ${#} -eq 1 ]]; then
        echo "Expected a .dot file after options"
        exit 1
    fi

    if [[ ! -e ${1} ]]; then
        echo "File ${1} does not exist"
        exit 1
    fi

    echo "RA_FLAG: ${RA_FLAG} REPL_FILE: ${REPL_FILE}"

    python3 "${PY_SCRIPT}" "${1}" -r "${REPL_FILE}" --start-edge-label "${start_edge_label}" ${RA_FLAG} #--remove-edge-pattern "${remove_edge_pattern}"
fi
