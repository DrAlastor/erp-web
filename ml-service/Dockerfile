FROM python:3.12-slim
WORKDIR /srv
COPY pyproject.toml ./
COPY app ./app
COPY sql ./sql
RUN pip install --no-cache-dir .
RUN useradd --create-home --uid 1001 ml
USER ml
EXPOSE 8000
# Solo en la red interna de Docker: el puerto 8000 NO se publica hacia afuera.
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
