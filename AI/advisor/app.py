import hmac
import os
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Optional

import torch
from fastapi import FastAPI, Header, HTTPException

from .catalog import load_catalog
from .model import load_model
from .retrieval import advise
from .schemas import AdviceReply, AdviceRequest


@asynccontextmanager
async def lifespan(app: FastAPI):
  torch.set_num_threads(int(os.getenv("AI_TORCH_THREADS", "2")))
  path = Path(os.getenv("AI_CATALOG_PATH", "data/catalog.json"))
  app.state.catalog = load_catalog(path) if path.exists() else []
  app.state.model = None
  checkpoint = os.getenv("AI_MODEL_PATH", "")
  if checkpoint:
    app.state.model, metadata = load_model(Path(checkpoint))
    if metadata.get("task") != "intents":
      raise ValueError("AI_MODEL_PATH phải là checkpoint đã huấn luyện task intents.")
  yield


app = FastAPI(title="Khoa course advisor", lifespan=lifespan)


@app.get("/health")
def health():
  return {
    "status": "ok",
    "modelLoaded": app.state.model is not None,
    "importedCourses": len(app.state.catalog),
  }


@app.post("/v1/advise", response_model=AdviceReply)
def advice(request: AdviceRequest, x_advisor_token: Optional[str] = Header(default=None)):
  token = os.getenv("AI_SERVICE_TOKEN", "")
  if token and not hmac.compare_digest(token, x_advisor_token or ""):
    raise HTTPException(status_code=401, detail="Invalid service token")
  if not request.message.strip():
    raise HTTPException(status_code=422, detail="Message must not be blank")
  return advise(request, app.state.catalog, app.state.model)
