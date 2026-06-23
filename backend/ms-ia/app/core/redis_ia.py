from typing import Optional

from redis import asyncio as redis_async

from app.core.config import (
    REDIS_IA_HOST,
    REDIS_IA_PORT,
    REDIS_IA_DB,
    REDIS_IA_PASSWORD,
)
from app.core.logging import get_logger

logger = get_logger("RedisIA")


redis_ia: Optional[redis_async.Redis] = None


async def init_redis_ia() -> None:
    global redis_ia
    if redis_ia is None:
        redis_ia = redis_async.Redis(
            host=REDIS_IA_HOST,
            port=REDIS_IA_PORT,
            db=REDIS_IA_DB,
            password=REDIS_IA_PASSWORD,
            decode_responses=False,
        )
        await redis_ia.ping()
        logger.info(f"Conexión exitosa a Redis IA en {REDIS_IA_HOST}:{REDIS_IA_PORT}")


async def close_redis_ia() -> None:
    global redis_ia
    if redis_ia is not None:
        await redis_ia.close()
        redis_ia = None
        logger.info("Conexión a Redis IA cerrada.")


def get_redis_ia() -> redis_async.Redis:
    if redis_ia is None:
        raise RuntimeError("Redis IA client is not initialized")
    return redis_ia
