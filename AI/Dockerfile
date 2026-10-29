FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt ./
# CPU wheels avoid downloading CUDA libraries for the default development service.
RUN pip install --no-cache-dir torch==2.6.0 --index-url https://download.pytorch.org/whl/cpu \
  && pip install --no-cache-dir -r requirements.txt
COPY advisor ./advisor
COPY data ./data
RUN useradd --create-home advisor && mkdir -p artifacts && chown advisor:advisor artifacts
USER advisor
EXPOSE 8000
CMD ["uvicorn", "advisor.app:app", "--host", "0.0.0.0", "--port", "8000"]
