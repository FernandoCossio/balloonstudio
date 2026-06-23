import json
import asyncio
import urllib.request
from datetime import datetime, timezone
from app.core.config import REDIS_CHANNEL_PENDING, REDIS_CHANNEL_RESULT
from app.core.redis_ia import get_redis_ia
from app.services.clip_service import get_clip_service
from app.services.db_service import get_db_service
from app.core.logging import get_logger

logger = get_logger("EmbeddingWorker")

# Global task reference to manage background execution
_worker_task = None
_should_run = True


async def publish_result(articulo_id: int, imagen_id: int, estado: str, error: str = None) -> None:
    """
    Publishes a processing update/result event to Redis.
    """
    try:
        redis = get_redis_ia()
        event = {
            "articulo_id": articulo_id,
            "imagen_id": imagen_id,
            "estado": estado,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "error": error
        }
        message = json.dumps(event)
        await redis.publish(REDIS_CHANNEL_RESULT, message)
        logger.info(f"Published result to '{REDIS_CHANNEL_RESULT}': {event}")
    except Exception as e:
        logger.error(f"Failed to publish result to Redis: {e}", exc_info=True)


def download_image_bytes(url: str, timeout: int = 30) -> bytes:
    """
    Downloads image content from a URL synchronously in a thread-safe manner using urllib.
    """
    logger.info(f"Downloading image from: {url}")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "ms-ia-recommender-worker/1.0"}
    )
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return response.read()


async def process_pending_event(event_data: dict) -> None:
    """
    Main pipeline: download image -> encode -> save to DB -> report status.
    """
    articulo_id = event_data.get("articulo_id")
    imagen_id = event_data.get("imagen_id")
    imagen_url = event_data.get("imagen_url")
    es_principal = event_data.get("es_principal", False)

    if not articulo_id or not imagen_id or not imagen_url:
        logger.error(f"Malformed event received: {event_data}")
        return

    if not es_principal:
        logger.info(f"Skipping article ID {articulo_id} image ID {imagen_id} because it's not principal.")
        return

    # 1. Send PROCESANDO status
    await publish_result(articulo_id, imagen_id, "PROCESANDO")

    db_service = get_db_service()
    loop = asyncio.get_running_loop()

    try:
        # Update image state in database to PROCESANDO
        await loop.run_in_executor(None, db_service.update_image_state, imagen_id, "PROCESANDO")

        # 2. Download image bytes (run in executor to avoid blocking event loop)
        image_bytes = await loop.run_in_executor(None, download_image_bytes, imagen_url)

        # 3. Generate embedding
        clip_service = get_clip_service()
        # run in executor because model inference blocks
        embedding = await loop.run_in_executor(None, clip_service.encode_image, image_bytes)

        # 4. Save to DB
        updated = await loop.run_in_executor(None, db_service.update_article_embedding, articulo_id, embedding)

        if updated:
            # Update image state in database to PROCESADO
            await loop.run_in_executor(None, db_service.update_image_state, imagen_id, "PROCESADO")
            # 5. Send PROCESADO status
            await publish_result(articulo_id, imagen_id, "PROCESADO")
        else:
            await loop.run_in_executor(None, db_service.update_image_state, imagen_id, "FALLIDO")
            await publish_result(
                articulo_id,
                imagen_id,
                "FALLIDO",
                error="Article not found or soft-deleted in database."
            )

    except Exception as e:
        logger.error(f"Error processing image for article ID {articulo_id}: {e}", exc_info=True)
        try:
            await loop.run_in_executor(None, db_service.update_image_state, imagen_id, "FALLIDO")
        except Exception as db_err:
            logger.error(f"Failed to set image state to FALLIDO in DB: {db_err}")
        await publish_result(articulo_id, imagen_id, "FALLIDO", error=str(e))


async def worker_loop() -> None:
    """
    Asynchronous event subscriber loop.
    """
    global _should_run
    redis = get_redis_ia()
    pubsub = redis.pubsub()
    await pubsub.subscribe(REDIS_CHANNEL_PENDING)
    logger.info(f"EmbeddingWorker subscribed to channel: {REDIS_CHANNEL_PENDING}")

    try:
        while _should_run:
            try:
                # Read message with a timeout so we check _should_run flag periodically
                message = await pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
                if message is None:
                    await asyncio.sleep(0.1)
                    continue

                logger.info(f"Received raw message from Redis: {message}")
                data_str = message.get("data")
                if isinstance(data_str, bytes):
                    data_str = data_str.decode("utf-8")

                event_data = json.loads(data_str)
                # Process the event in background so we don't block subscription loop
                asyncio.create_task(process_pending_event(event_data))

            except asyncio.CancelledError:
                break
            except Exception as e:
                logger.error(f"Error in worker event loop: {e}", exc_info=True)
                await asyncio.sleep(2)
    finally:
        await pubsub.unsubscribe(REDIS_CHANNEL_PENDING)
        await pubsub.close()
        logger.info("EmbeddingWorker unsubscribed and closed.")


def start_embedding_worker() -> None:
    global _worker_task, _should_run
    _should_run = True
    if _worker_task is None or _worker_task.done():
        _worker_task = asyncio.create_task(worker_loop())
        logger.info("Background EmbeddingWorker task started.")


async def stop_embedding_worker() -> None:
    global _worker_task, _should_run
    _should_run = False
    if _worker_task is not None and not _worker_task.done():
        _worker_task.cancel()
        try:
            await _worker_task
        except asyncio.CancelledError:
            pass
        _worker_task = None
        logger.info("Background EmbeddingWorker task stopped.")
