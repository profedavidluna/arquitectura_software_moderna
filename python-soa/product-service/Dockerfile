# =============================================================================
# Product Service - Dockerfile
# =============================================================================
# Multi-stage build for a lightweight Python service container.
# Uses python:3.11-slim for minimal image size while maintaining compatibility.
# =============================================================================

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies required by asyncpg
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ ./app/

# Expose service port
EXPOSE 9091

# Run with uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9091"]
