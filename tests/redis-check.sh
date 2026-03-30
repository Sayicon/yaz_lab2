#!/usr/bin/env bash
# redis-check.sh — FAZ 1-A
# Redis bağlantısını ping/pong ile doğrular.
# Kullanım: ./tests/redis-check.sh [host] [port]
#   Varsayılan: host=localhost, port=6379

set -euo pipefail

REDIS_HOST="${1:-localhost}"
REDIS_PORT="${2:-6379}"

echo "========================================"
echo " Redis Bağlantı Kontrolü"
echo " Host: $REDIS_HOST  Port: $REDIS_PORT"
echo "========================================"

# redis-cli ile PING komutu gönder
RESPONSE=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" PING 2>/dev/null || echo "ERROR")

if [ "$RESPONSE" = "PONG" ]; then
    echo "[PASS] Redis yanıtı: $RESPONSE"
    echo "Redis bağlantısı başarılı."
    exit 0
else
    echo "[FAIL] Redis yanıtı: '$RESPONSE' (beklenen: PONG)" >&2
    echo "Redis'e bağlanılamadı veya redis-cli kurulu değil." >&2
    exit 1
fi
