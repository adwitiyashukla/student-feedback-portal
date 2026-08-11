from __future__ import annotations

from fastapi.testclient import TestClient


class TestHealth:
    def test_health_is_public(self, client: TestClient) -> None:
        response = client.get("/api/v1/health")
        assert response.status_code == 200

        body = response.json()
        assert body["status"] == "UP"
        assert body["model_loaded"] is True


class TestAuthentication:
    def test_analyze_rejects_missing_key(self, client: TestClient) -> None:
        response = client.post("/api/v1/analyze", json={"title": "x", "text": "the mess is bad"})
        assert response.status_code == 401

    def test_analyze_rejects_wrong_key(self, client: TestClient) -> None:
        response = client.post(
            "/api/v1/analyze",
            json={"title": "x", "text": "the mess is bad"},
            headers={"X-API-Key": "not-the-key"},
        )
        assert response.status_code == 401

    def test_model_info_requires_key(self, client: TestClient) -> None:
        assert client.get("/api/v1/model/info").status_code == 401


class TestAnalyze:
    def test_returns_the_full_contract(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post(
            "/api/v1/analyze",
            json={
                "title": "Mess food quality has dropped",
                "text": "The dinner in the hostel mess is undercooked and several students fell ill.",
            },
            headers=auth_headers,
        )
        assert response.status_code == 200

        body = response.json()
        assert set(body) == {
            "sentiment_label", "sentiment_score", "category", "category_confidence",
            "priority", "keywords", "model_version",
        }
        assert body["sentiment_label"] == "NEGATIVE"
        assert body["category"] == "HOSTEL"
        assert -1.0 <= body["sentiment_score"] <= 1.0
        assert 0.0 <= body["category_confidence"] <= 1.0

    def test_escalates_safety_reports(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post(
            "/api/v1/analyze",
            json={
                "title": "Workshop safety",
                "text": "The lathe machines have no chip guards, this is dangerous and someone will be injured.",
            },
            headers=auth_headers,
        )
        assert response.json()["priority"] == "URGENT"

    def test_recognises_praise(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post(
            "/api/v1/analyze",
            json={
                "title": "Thanks to the IT team",
                "text": "The network outage was restored very quickly and updates were excellent.",
            },
            headers=auth_headers,
        )
        body = response.json()
        assert body["sentiment_label"] == "POSITIVE"
        assert body["priority"] == "LOW"

    def test_rejects_empty_text(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post("/api/v1/analyze", json={"title": "x", "text": ""},
                               headers=auth_headers)
        assert response.status_code == 422

    def test_rejects_missing_text(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        assert client.post("/api/v1/analyze", json={"title": "x"},
                           headers=auth_headers).status_code == 422

    def test_title_is_optional(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post("/api/v1/analyze",
                               json={"text": "The library reading room closes far too early"},
                               headers=auth_headers)
        assert response.status_code == 200


class TestBatch:
    def test_processes_every_item_in_order(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        response = client.post(
            "/api/v1/analyze/batch",
            json={"items": [
                {"title": "Bus", "text": "The route 7 bus is late every single morning"},
                {"title": "Praise", "text": "The lecturer explains everything brilliantly"},
            ]},
            headers=auth_headers,
        )
        assert response.status_code == 200

        body = response.json()
        assert body["processed"] == 2
        assert body["results"][0]["category"] == "TRANSPORT"
        assert body["results"][1]["sentiment_label"] == "POSITIVE"

    def test_rejects_empty_batch(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        assert client.post("/api/v1/analyze/batch", json={"items": []},
                           headers=auth_headers).status_code == 422


class TestModelInfo:
    def test_describes_the_model(self, client: TestClient, auth_headers: dict[str, str]) -> None:
        body = client.get("/api/v1/model/info", headers=auth_headers).json()
        assert len(body["categories"]) == 10
        assert body["training_examples"] > 0
        assert body["vocabulary_size"] > 0


class TestDocumentation:
    def test_openapi_schema_is_served(self, client: TestClient) -> None:
        schema = client.get("/openapi.json").json()
        assert "/api/v1/analyze" in schema["paths"]
