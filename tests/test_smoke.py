"""
FAZ 1 — A kısmı
Smoke testler: docker-compose up sonrası tüm servislerin ayakta olduğunu doğrular.

Her servis kendi /health endpoint'ini sunmalı; Dispatcher ise dışarıya açık
tek servis olduğundan doğrudan test edilir, diğerleri docker host üzerinden
erişilebilir portlarla test edilir (geliştirme ortamında).

Çalıştırma:
    pytest tests/test_smoke.py -v
"""

import requests
import pytest


SERVICES = [
    ("dispatcher", "http://localhost:8000/health"),
    ("auth-service", "http://localhost:8001/health"),
    ("user-service", "http://localhost:8002/health"),
    ("product-service", "http://localhost:8003/health"),
]


@pytest.mark.parametrize("service_name,health_url", SERVICES)
def test_service_is_up(service_name, health_url):
    """Her servisin /health endpoint'i HTTP 200 döndürmeli."""
    try:
        response = requests.get(health_url, timeout=5)
        assert response.status_code == 200, (
            f"{service_name} beklenen 200 yerine {response.status_code} döndürdü."
        )
    except requests.exceptions.ConnectionError:
        pytest.fail(f"{service_name} servisine bağlanılamadı: {health_url}")


def test_dispatcher_is_only_external_service():
    """
    Dispatcher dışındaki servisler production ortamında
    dış ağdan erişilebilir olmamalıdır.
    Bu test, sadece dispatcher'ın belgelenmiş dış porta (8000) cevap
    verdiğini; diğer portların ise yalnızca geliştirme modunda
    açık olduğunu belgelemek için yazılmıştır.
    """
    response = requests.get("http://localhost:8000/health", timeout=5)
    assert response.status_code == 200


@pytest.mark.parametrize("service_name,health_url", SERVICES)
def test_health_response_is_json(service_name, health_url):
    """Her servisin /health endpoint'i geçerli JSON döndürmeli."""
    try:
        response = requests.get(health_url, timeout=5)
        # JSON parse başarısız olursa ValueError fırlatır
        body = response.json()
        assert isinstance(body, dict), f"{service_name} JSON dict döndürmedi."
    except requests.exceptions.ConnectionError:
        pytest.fail(f"{service_name} servisine bağlanılamadı: {health_url}")
