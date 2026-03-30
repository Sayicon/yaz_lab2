#!/usr/bin/env bash
# smoke-test.sh — FAZ 1-A
# docker-compose up sonrası tüm servislerin /health endpoint'ini kontrol eder.
# Kullanım: ./tests/smoke-test.sh

set -euo pipefail

PASS=0
FAIL=0
TIMEOUT=5  # saniye

check() {
    local name="$1"
    local url="$2"
    local http_code

    http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "000")

    if [ "$http_code" -eq 200 ] 2>/dev/null; then
        echo "[PASS] $name → $url (HTTP $http_code)"
        PASS=$((PASS + 1))
    else
        echo "[FAIL] $name → $url (HTTP $http_code)"
        FAIL=$((FAIL + 1))
    fi
}

echo "========================================"
echo " Smoke Test — Servis Sağlık Kontrolleri"
echo "========================================"

# Dispatcher (dışa açık tek servis)
check "Dispatcher"       "http://localhost:8080/actuator/health"

# Auth Service (internal ağda, host'tan erişilebilir olması için docker-compose ports tanımlıysa)
check "Auth Service"     "http://localhost:8081/actuator/health"

# User Service
check "User Service"     "http://localhost:8082/actuator/health"

# Product Service
check "Product Service"  "http://localhost:8083/actuator/health"

echo "========================================"
echo " Sonuç: $PASS PASS / $FAIL FAIL"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    echo "HATA: $FAIL servis yanıt vermiyor!" >&2
    exit 1
fi

echo "Tüm servisler ayakta."
exit 0
