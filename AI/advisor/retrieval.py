import re
from collections import Counter
from decimal import Decimal

import torch
from torch.nn import functional as F

from .catalog import normalize
from .schemas import AdviceReply, AdviceRequest, Suggestion

STOP_WORDS = set(
  "toi minh ban em anh chi muon can hoc khoa cac mot nhung co nao gi ve cho voi va la thi de duoc tu den o tren khong nhe giup tim tuvan goi y phu hop gia bao nhieu tien ngan sach duoi toi da tam khoang nghin ngan trieu tr k vnd dong moi bat dau chua biet lap trinh co ban trung cap nang cao so sanh xem mo vao luon do nay do ay".split()
)
ALIASES = {
  "lap trinh": "python java angular",
  "backend": "java spring api mysql",
  "frontend": "angular web component",
  "giao dien": "angular frontend",
  "do hoa": "illustrator vector",
  "quang cao": "marketing",
  "thiet ke": "design figma illustrator",
  "tu dong hoa": "python",
}


def words(text: str, expand=True) -> list:
  text = normalize(text)
  for alias, expansion in ALIASES.items() if expand else []:
    if re.search(r"\b" + re.escape(alias) + r"\b", text):
      text += " " + expansion
  return [word for word in re.findall(r"[a-z]+", text) if word not in STOP_WORDS and len(word) > 1]


def budget(text: str):
  text = normalize(text)
  minimum, maximum, strict = None, None, False
  for match in re.finditer(r"(\d+(?:[.,]\d+)*)\s*(trieu|tr|nghin|ngan|k|vnd|dong|₫)?\b", text):
    number, unit = match.groups()
    if re.fullmatch(r"\d{1,3}(?:[.,]\d{3})+", number):
      number = number.replace(".", "").replace(",", "")
    else:
      number = number.replace(",", ".")
    try:
      value = Decimal(number)
    except ArithmeticError:
      continue
    if unit in ("trieu", "tr"):
      value *= 1_000_000
    elif unit in ("nghin", "ngan", "k"):
      value *= 1000
    elif not unit and value < 1000:
      continue
    prefix = text[max(0, match.start() - 30) : match.start()]
    if re.search(r"(?:tu|tren|it nhat)\s*$", prefix):
      minimum = value
    else:
      maximum = value
      strict = bool(re.search(r"duoi\s*$", prefix))
  return minimum, maximum, strict


def rule_intent(message: str) -> str:
  text = normalize(message)
  if any(
    term in text for term in ("hoan tien", "cam ket", "chung chi", "bao hanh", "giam gia", "uu dai")
  ):
    return "policy"
  if any(term in text for term in ("mo khoa", "vao hoc", "bat dau hoc", "dang ky", "mua khoa")):
    return "open"
  if any(term in text for term in ("gia bao", "bao nhieu tien", "hoc phi", "gia cua")):
    return "price"
  if any(term in text for term in ("lo trinh", "noi dung", "hoc duoc gi", "dau ra", "bai hoc")):
    return "syllabus"
  if re.fullmatch(
    r"\s*(?:(?:xin )?chao(?: ban| khoa| tro ly)?|hello|hi)(?: ban| nhe| oi)?[!.\s]*", text
  ):
    return "greeting"
  return "recommend"


def similarities(query: str, courses: list) -> torch.Tensor:
  documents = [Counter(words(course.document(), expand=False)) for course in courses]
  vocabulary = sorted(set().union(*(set(document) for document in documents)))
  if not vocabulary:
    return torch.zeros(len(courses))
  counts = torch.tensor(
    [[document.get(word, 0) for word in vocabulary] for document in documents], dtype=torch.float32
  )
  inverse_frequency = torch.log((1 + len(courses)) / (1 + (counts > 0).sum(dim=0))) + 1
  matrix = F.normalize(torch.log1p(counts) * inverse_frequency, dim=1)
  query_counts = Counter(words(query))
  vector = torch.tensor([query_counts.get(word, 0) for word in vocabulary], dtype=torch.float32)
  vector = F.normalize(torch.log1p(vector) * inverse_frequency, dim=0)
  return matrix @ vector


def merge_catalog(live: list, imported: list) -> list:
  # Website data wins for every business field. Excel can enrich descriptive fields.
  combined = {course.slug: course for course in imported}
  for course in live:
    extra = combined.get(course.slug)
    combined[course.slug] = course.model_copy(
      update={
        "audience": extra.audience if extra else course.audience,
        "prerequisites": extra.prerequisites if extra else course.prerequisites,
      }
    )
  return list(combined.values())


def advise(request: AdviceRequest, imported: list, model=None) -> AdviceReply:
  courses = merge_catalog(request.courses, imported)
  intent = rule_intent(request.message)
  engine = "pytorch-tfidf"
  if model is not None:
    predicted, confidence = model.intent(request.message)
    # Explicit business requests take precedence; model scores are experimental.
    if intent == "recommend" and confidence > 0.5 and predicted in ("price", "syllabus"):
      intent = predicted
    engine += "+tiny-gpt"
  if not courses:
    return AdviceReply(
      message="Mình chưa có danh mục khóa học. Bạn hãy thử lại sau nhé.",
      intent=intent,
      courses=[],
      engine=engine,
    )
  if intent == "policy":
    return AdviceReply(
      message="Danh mục hiện chưa có chính sách đã xác nhận cho yêu cầu này. Bạn vui lòng liên hệ đội tư vấn để được kiểm tra, mình chưa thể cam kết thay nhà cung cấp.",
      intent=intent,
      courses=[],
      engine=engine,
    )
  if intent == "greeting":
    return AdviceReply(
      message="Chào bạn! Bạn muốn học kỹ năng gì, đang ở trình độ nào và dự kiến ngân sách bao nhiêu?",
      intent=intent,
      courses=[],
      engine=engine,
    )
  if "danh muc" in normalize(request.message):
    categories = sorted({course.category for course in courses if course.category})
    message = "Hiện có các danh mục: " + ", ".join(categories) + ". Bạn quan tâm lĩnh vực nào?"
    return AdviceReply(message=message, intent="recommend", courses=[], engine=engine)

  previous = [turn.content for turn in request.history if turn.role == "user"]
  message = normalize(request.message)
  constraints = [None, None, False]
  for question in [*previous, request.message]:
    normalized = normalize(question)
    if "khong gioi han" in normalized or "bo ngan sach" in normalized:
      constraints = [None, None, False]
    parsed = budget(question)
    if parsed[0] is not None or parsed[1] is not None:
      constraints = parsed
  minimum, maximum, strict = constraints
  level = None
  for question in [*previous, request.message]:
    normalized = normalize(question)
    if any(term in normalized for term in ("moi bat dau", "nguoi moi", "chua biet", "co ban")):
      level = "co ban"
    if "trung cap" in normalized or "da biet" in normalized:
      level = "trung cap"
    if "nang cao" in normalized:
      level = "nang cao"
    if "bat ky trinh do" in normalized:
      level = None
  scores = similarities(request.message, courses)
  # A new explicit topic replaces the previous topic; a short follow-up preserves it.
  query_words = words(request.message)
  if float(scores.max()) <= 0 and previous and not query_words:
    scores = similarities(" ".join(previous[-3:]), courses)
  context = next((course for course in courses if course.slug == request.courseSlug), None)
  if context and (
    intent in ("price", "syllabus", "open")
    or any(term in message for term in ("khoa nay", "khoa do"))
  ):
    # A named topic in the new message can still override the current page.
    named = any(
      word in words(request.message)
      for course in courses
      if course.slug != context.slug
      for word in words(course.title)
      if word in ("java", "python", "angular", "figma", "illustrator", "marketing")
    )
    if not named:
      scores = torch.tensor([1.0 if course.slug == context.slug else 0.0 for course in courses])
  has_topic = float(scores.max()) > 0
  if not has_topic and query_words:
    return AdviceReply(
      message="Mình chưa tìm thấy khóa học khớp với chủ đề bạn hỏi. Bạn có thể nói rõ kỹ năng muốn học hoặc xem các danh mục hiện có nhé.",
      intent=intent,
      courses=[],
      engine=engine,
    )
  ordered = sorted(
    range(len(courses)), key=lambda index: (-float(scores[index]), courses[index].price)
  )
  selected = []
  for index in ordered:
    course = courses[index]
    if has_topic and float(scores[index]) <= 0:
      continue
    if minimum is not None and course.price < minimum:
      continue
    if maximum is not None and (course.price >= maximum if strict else course.price > maximum):
      continue
    if level and level not in normalize(course.level):
      continue
    selected.append(course)
    if len(selected) == 3:
      break
  if not selected:
    return AdviceReply(
      message="Mình chưa tìm được khóa học đáp ứng đồng thời chủ đề, trình độ và ngân sách này. Bạn muốn đổi lĩnh vực, trình độ hay mức ngân sách?",
      intent=intent,
      courses=[],
      engine=engine,
    )
  if intent in ("price", "syllabus", "open") and not has_topic:
    return AdviceReply(
      message="Bạn muốn xem thông tin khóa học nào? Hãy cho mình tên khóa học hoặc mở trang chi tiết rồi hỏi lại nhé.",
      intent=intent,
      courses=[],
      engine=engine,
    )
  suggestions = []
  details = []
  for course in selected:
    reason = (
      course.subtitle or course.description[:220] or f"Khóa học trình độ {course.level.lower()}."
    )
    if course.outcomes:
      reason += " Bạn sẽ học: " + "; ".join(course.outcomes[:2]) + "."
    suggestions.append(
      Suggestion(
        slug=course.slug, title=course.title, price=course.price, level=course.level, reason=reason
      )
    )
    if intent == "syllabus":
      details.append(
        course.title
        + ": "
        + ("; ".join(course.outcomes) or course.description or "Chưa cập nhật nội dung chi tiết.")
      )
  if intent == "open":
    answer = "Mình đã tìm được khóa học bên dưới. Chọn Xem khóa học, Vào học nếu bạn đã đăng ký, hoặc Đăng ký để tiếp tục. Việc cấp quyền học được hệ thống kiểm tra riêng."
  elif intent == "price":
    answer = "Đây là giá hiện tại của các khóa học mình tìm được. Bạn có thể mở trang chi tiết để xác nhận trước khi đăng ký."
  elif intent == "syllabus":
    answer = "\n\n".join(details)
  else:
    answer = "Mình tìm được các khóa học dưới đây phù hợp với thông tin bạn cung cấp. Bạn muốn mình giải thích thêm về khóa nào?"
  # Include course names in server-held history so a follow-up can refer to them.
  if intent != "syllabus":
    answer += "\n" + " · ".join(course.title for course in selected)
  return AdviceReply(message=answer, intent=intent, courses=suggestions, engine=engine)
