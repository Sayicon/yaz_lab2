import os
import pytest

# Dispatcher URL — docker-compose'da dışa açık tek servis
DISPATCHER_URL = os.getenv("DISPATCHER_URL", "http://localhost:8000")

# Internal servis URL'leri (smoke testler için docker host'tan erişim)
AUTH_URL      = os.getenv("AUTH_URL",      "http://localhost:8001")
USER_URL      = os.getenv("USER_URL",      "http://localhost:8002")
PRODUCT_URL   = os.getenv("PRODUCT_URL",   "http://localhost:8003")

# Redis
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))


@pytest.fixture
def dispatcher_url():
    return DISPATCHER_URL


@pytest.fixture
def service_urls():
    return {
        "dispatcher": DISPATCHER_URL,
        "auth":       AUTH_URL,
        "user":       USER_URL,
        "product":    PRODUCT_URL,
    }


@pytest.fixture
def redis_config():
    return {"host": REDIS_HOST, "port": REDIS_PORT}
