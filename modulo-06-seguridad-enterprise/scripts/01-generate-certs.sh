#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${ROOT_DIR}/certs"

mkdir -p "${CERT_DIR}"

rm -f "${CERT_DIR}"/*.pem "${CERT_DIR}"/*.p12 "${CERT_DIR}"/*.srl

openssl genrsa -out "${CERT_DIR}/ca.key" 2048
openssl req -x509 -new -nodes -key "${CERT_DIR}/ca.key" -sha256 -days 3650 \
  -out "${CERT_DIR}/ca.crt" \
  -subj "/CN=JoeDayz Module 6 CA"

openssl genrsa -out "${CERT_DIR}/inventory-service.key" 2048
openssl req -new -key "${CERT_DIR}/inventory-service.key" \
  -out "${CERT_DIR}/inventory-service.csr" \
  -subj "/CN=inventory-service"
openssl x509 -req -in "${CERT_DIR}/inventory-service.csr" \
  -CA "${CERT_DIR}/ca.crt" \
  -CAkey "${CERT_DIR}/ca.key" \
  -CAcreateserial \
  -out "${CERT_DIR}/inventory-service.crt" \
  -days 825 -sha256

openssl genrsa -out "${CERT_DIR}/order-service-client.key" 2048
openssl req -new -key "${CERT_DIR}/order-service-client.key" \
  -out "${CERT_DIR}/order-service-client.csr" \
  -subj "/CN=order-service"
openssl x509 -req -in "${CERT_DIR}/order-service-client.csr" \
  -CA "${CERT_DIR}/ca.crt" \
  -CAkey "${CERT_DIR}/ca.key" \
  -CAcreateserial \
  -out "${CERT_DIR}/order-service-client.crt" \
  -days 825 -sha256

openssl pkcs12 -export \
  -out "${CERT_DIR}/inventory-service.p12" \
  -inkey "${CERT_DIR}/inventory-service.key" \
  -in "${CERT_DIR}/inventory-service.crt" \
  -certfile "${CERT_DIR}/ca.crt" \
  -password pass:changeit

openssl pkcs12 -export \
  -out "${CERT_DIR}/order-service-client.p12" \
  -inkey "${CERT_DIR}/order-service-client.key" \
  -in "${CERT_DIR}/order-service-client.crt" \
  -certfile "${CERT_DIR}/ca.crt" \
  -password pass:changeit

keytool -importcert -noprompt \
  -alias modulo6-ca \
  -file "${CERT_DIR}/ca.crt" \
  -keystore "${CERT_DIR}/platform-truststore.p12" \
  -storetype PKCS12 \
  -storepass changeit

echo "Certificados generados en ${CERT_DIR}"
