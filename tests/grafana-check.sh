#!/bin/bash
# FAZ 5 - A: Grafana container'ı ayakta mı ve Prometheus datasource'u bağlı mı?
# Kullanım: ./tests/grafana-check.sh
# Gereksinim: docker-compose up ile sistem ayakta olmalı

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASS="admin"
PASS=0
FAIL=0

check() {
    local desc="$1"
    local result="$2"
    if [ "$result" = "ok" ]; then
        echo "  [OK] $desc"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $desc — $result"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Grafana Smoke Test (Faz 5-A) ==="

# 1. Grafana ayakta mı?
status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$GRAFANA_URL/api/health")
if [ "$status" = "200" ]; then
    check "Grafana container ayakta (/api/health → 200)" "ok"
else
    check "Grafana container ayakta (/api/health → 200)" "HTTP $status"
fi

# 2. Grafana'ya login olunabiliyor mu?
login_status=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 \
    -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_URL/api/org")
if [ "$login_status" = "200" ]; then
    check "Grafana admin login başarılı" "ok"
else
    check "Grafana admin login başarılı" "HTTP $login_status"
fi

# 3. Prometheus datasource tanımlı mı?
ds_body=$(curl -s --max-time 5 \
    -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_URL/api/datasources")
if echo "$ds_body" | grep -qi "prometheus"; then
    check "Prometheus datasource tanımlı" "ok"
else
    check "Prometheus datasource tanımlı" "datasource bulunamadı"
fi

# 4. Prometheus datasource erişilebilir mi? (health check)
ds_health=$(curl -s --max-time 5 \
    -u "$GRAFANA_USER:$GRAFANA_PASS" \
    -X GET "$GRAFANA_URL/api/datasources/proxy/1/api/v1/query?query=up" \
    -o /dev/null -w "%{http_code}")
if [ "$ds_health" = "200" ]; then
    check "Prometheus datasource erişilebilir (query çalışıyor)" "ok"
else
    check "Prometheus datasource erişilebilir (query çalışıyor)" "HTTP $ds_health"
fi

echo ""
echo "Sonuç: $PASS passed, $FAIL failed"

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
