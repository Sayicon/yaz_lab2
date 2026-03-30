"""
FAZ 1 — A kısmı
Redis bağlantı testi: ping/pong protokolüyle bağlantının açık olduğunu doğrular.

Çalıştırma (docker-compose up sonrası):
    pytest tests/test_redis_connection.py -v
"""

import pytest
import redis


def test_redis_ping(redis_config):
    """Redis'e bağlanıp PING gönderildiğinde PONG alınmalı."""
    client = redis.Redis(
        host=redis_config["host"],
        port=redis_config["port"],
        socket_connect_timeout=5,
        decode_responses=True,
    )
    result = client.ping()
    assert result is True, "Redis PING'e PONG dönmedi."


def test_redis_set_and_get(redis_config):
    """Redis'te key-value yazılıp okunabilmeli."""
    client = redis.Redis(
        host=redis_config["host"],
        port=redis_config["port"],
        socket_connect_timeout=5,
        decode_responses=True,
    )
    client.set("faz1_test_key", "faz1_test_value", ex=60)
    value = client.get("faz1_test_key")
    assert value == "faz1_test_value"
    # Temizlik
    client.delete("faz1_test_key")


def test_redis_connection_error_handling():
    """Yanlış porta bağlanma girişimi uygun hata fırlatmalı."""
    client = redis.Redis(
        host="localhost",
        port=19999,  # kullanılmayan port
        socket_connect_timeout=2,
    )
    with pytest.raises(redis.exceptions.ConnectionError):
        client.ping()
