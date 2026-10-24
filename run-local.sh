#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${GOOGLE_MAPS_API_KEY:-}" ]]; then
    if [[ ! -t 0 ]]; then
        printf '%s\n' "GOOGLE_MAPS_API_KEY is required when running interactively; set it in the environment for non-interactive runs." >&2
        exit 1
    fi
    read -r -s -p "Google Maps API key: " GOOGLE_MAPS_API_KEY
    printf '\n'
fi

if [[ -z "${BEDDING_PLANTS_DB_PASSWORD:-}" ]]; then
    if [[ ! -t 0 ]]; then
        printf '%s\n' "BEDDING_PLANTS_DB_PASSWORD is required when running interactively; set it in the environment for non-interactive runs." >&2
        exit 1
    fi
    read -r -s -p "Development H2 database password: " BEDDING_PLANTS_DB_PASSWORD
    printf '\n'
fi

if [[ -z "${GOOGLE_MAPS_API_KEY}" || -z "${BEDDING_PLANTS_DB_PASSWORD}" ]]; then
    printf '%s\n' "Both environment variables must be non-empty." >&2
    exit 1
fi

export GOOGLE_MAPS_API_KEY BEDDING_PLANTS_DB_PASSWORD
exec ./mvnw clean spring-boot:run "$@"
