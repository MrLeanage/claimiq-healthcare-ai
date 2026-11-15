FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
RUN groupadd --gid 10001 claimiq \
    && useradd --uid 10001 --gid claimiq --no-create-home claimiq
COPY --chown=claimiq:claimiq ai-service/app ./app
USER claimiq
EXPOSE 8090
CMD ["python", "-m", "app.server", "--host", "0.0.0.0", "--port", "8090"]
