import os
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(dotenv_path=BASE_DIR.parent / ".env")

# CLIP models
CLIP_IMAGE_MODEL = os.getenv("CLIP_IMAGE_MODEL", "openai/clip-vit-base-patch32")
CLIP_TEXT_MODEL = os.getenv("CLIP_TEXT_MODEL", "sentence-transformers/clip-ViT-B-32-multilingual-v1")

# PostgreSQL Database config
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/decoraciones")

# Redis configuration
REDIS_IA_HOST = os.getenv("REDIS_IA_HOST", "localhost")
REDIS_IA_PORT = int(os.getenv("REDIS_IA_PORT", "6380"))
REDIS_IA_DB = int(os.getenv("REDIS_IA_DB", "0"))
REDIS_IA_PASSWORD = os.getenv("REDIS_IA_PASSWORD", None)

# Redis Channels
REDIS_CHANNEL_PENDING = os.getenv("REDIS_CHANNEL_PENDING", "ia:embedding:pending")
REDIS_CHANNEL_RESULT = os.getenv("REDIS_CHANNEL_RESULT", "ia:embedding:result")

# General Settings
BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://localhost:8080/api")
MAX_FILE_SIZE_MB = int(os.getenv("MAX_FILE_SIZE_MB", "10"))
DEFAULT_TOP_K = int(os.getenv("DEFAULT_TOP_K", "5"))

