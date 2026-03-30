"""
FAZ 1 — A kısmı
Dispatcher /health endpoint testi.

Gereksinim: Dispatcher'ın /health endpoint'i HTTP 200 döndürmeli
ve yanıt gövdesi status alanını içermeli.

Çalıştırma (docker-compose up sonrası):
    pytest tests/test_dispatcher_health.py -v
"""

import requests
import pytest


def test_health_returns_200(dispatcher_url):
    """GET /health → 200 OK döndürmeli."""
    response = requests.get(f"{dispatcher_url}/health", timeout=5)
    assert response.status_code == 200


def test_health_response_has_status_field(dispatcher_url):
    """GET /health yanıtı JSON ve 'status' alanı içermeli."""
    response = requests.get(f"{dispatcher_url}/health", timeout=5)
    assert response.status_code == 200
    body = response.json()
    assert "status" in body


def test_health_status_is_ok(dispatcher_url):
    """GET /health → status değeri 'ok' olmalı."""
    response = requests.get(f"{dispatcher_url}/health", timeout=5)
    body = response.json()
    assert body["status"] == "ok"
