from decimal import Decimal

import pytest
from fastapi.testclient import TestClient

from advisor.app import app
from advisor.catalog import parse_price, read_spreadsheet
from advisor.retrieval import advise
from advisor.schemas import AdviceRequest, Course, Turn


@pytest.fixture
def catalog():
  return [
    Course(
      slug="python-co-ban",
      title="Python cơ bản",
      price=399000,
      level="Cơ bản",
      category="Lập trình",
      description="Biến và hàm Python",
      outcomes=["Tự động hóa công việc"],
    ),
    Course(
      slug="java-spring-boot",
      title="Java Spring Boot",
      price=699000,
      level="Trung cấp",
      category="Lập trình",
      description="Xây REST API với Java",
    ),
    Course(
      slug="figma", title="Thiết kế UI UX Figma", price=499000, level="Cơ bản", category="Thiết kế"
    ),
    Course(
      slug="angular", title="Angular frontend", price=599000, level="Cơ bản", category="Lập trình"
    ),
  ]


def ask(catalog, message, **kwargs):
  return advise(AdviceRequest(message=message, courses=catalog, **kwargs), [])


def test_budget_and_beginner_do_not_recommend_unrelated_expensive_courses(catalog):
  reply = ask(catalog, "Mình mới bắt đầu lập trình, ngân sách dưới 500 nghìn")
  assert [course.slug for course in reply.courses] == ["python-co-ban"]
  assert reply.courses[0].price == Decimal(399000)


def test_filters_remember_budget_but_new_topic_overrides_previous(catalog):
  history = [Turn(role="user", content="Mình muốn học Python dưới 500k")]
  reply = ask(catalog, "Còn Figma thì sao", history=history)
  assert reply.courses[0].slug == "figma"
  assert all(course.price < 500000 for course in reply.courses)
  assert ask(catalog, "Tôi muốn học Kubernetes", history=history).courses == []


def test_follow_up_keeps_topic_and_no_match_does_not_break_budget(catalog):
  history = [Turn(role="user", content="Tôi muốn học Python")]
  assert [course.slug for course in ask(catalog, "Dưới 500k", history=history).courses] == [
    "python-co-ban"
  ]
  assert ask(catalog, "Java dưới 500k").courses == []
  assert ask(catalog, "Có khóa nào dưới 0 đồng").courses == []


def test_page_context_and_current_topic(catalog):
  reply = ask(catalog, "Khóa này giá bao nhiêu", courseSlug="java-spring-boot")
  assert reply.courses[0].slug == "java-spring-boot"
  assert (
    ask(catalog, "Python giá bao nhiêu", courseSlug="java-spring-boot").courses[0].slug
    == "python-co-ban"
  )


def test_live_prices_override_excel_and_policies_are_not_invented(catalog):
  imported = [catalog[0].model_copy(update={"price": Decimal(1)})]
  reply = advise(AdviceRequest(message="Python", courses=catalog), imported)
  assert reply.courses[0].price == Decimal(399000)
  assert ask(catalog, "Khóa học này có hoàn tiền không").courses == []
  assert "chưa có chính sách" in ask(catalog, "Có cấp chứng chỉ không").message


@pytest.mark.parametrize(
  "value, expected", [(499000, 499000), ("499.000 ₫", 499000), ("499,000 VND", 499000), (0, 0)]
)
def test_prices(value, expected):
  assert parse_price(value) == expected


@pytest.mark.parametrize("value", [True, -1, "1.5tr", "=SUM(A1:A2)", "NaN", float("inf"), 1.5])
def test_invalid_prices(value):
  with pytest.raises(ValueError):
    parse_price(value)


def test_csv_import_rejects_duplicates_and_partial_bad_import(tmp_path):
  path = tmp_path / "courses.csv"
  path.write_text("Mã khóa học,Tên khóa học,Giá\npython,Python,399000\n", encoding="utf-8-sig")
  assert read_spreadsheet(path)[0].price == Decimal(399000)
  path.write_text("slug,title,price\npython,Python,399000\npython,Python,1\n", encoding="utf-8")
  with pytest.raises(ValueError, match="Trùng"):
    read_spreadsheet(path)
  path.write_text("slug,title,price\npython,Python,=1+1\n", encoding="utf-8")
  with pytest.raises(ValueError, match="công thức"):
    read_spreadsheet(path)


def test_service_requires_configured_token_and_valid_request(monkeypatch, catalog):
  monkeypatch.setenv("AI_SERVICE_TOKEN", "test-secret")
  with TestClient(app) as client:
    assert client.get("/health").status_code == 200
    body = {"message": "Python", "courses": [course.model_dump(mode="json") for course in catalog]}
    assert client.post("/v1/advise", json=body).status_code == 401
    response = client.post("/v1/advise", json=body, headers={"X-Advisor-Token": "test-secret"})
    assert response.status_code == 200
    assert response.json()["courses"][0]["slug"] == "python-co-ban"
    assert (
      client.post(
        "/v1/advise", json={"message": " "}, headers={"X-Advisor-Token": "test-secret"}
      ).status_code
      == 422
    )
