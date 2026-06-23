import time
import json
from fastapi import APIRouter, File, UploadFile, Query, HTTPException, status
from typing import Dict, Any, List
import uuid

from app.models.schemas import (
    RecommendTextRequest,
    RecommendResponse,
    RecommendItem,
    ReprocessResponse,
    RespuestaAPI
)
from app.services.clip_service import get_clip_service
from app.services.db_service import get_db_service
from app.core.redis_ia import get_redis_ia
from app.core.config import REDIS_CHANNEL_PENDING, MAX_FILE_SIZE_MB
from app.core.logging import get_logger

logger = get_logger("RecommendationsRoute")

# Public router for clients
recommend_router = APIRouter(prefix="/v1/recommend", tags=["recommendations"])

# Internal router for Java backend communication
internal_router = APIRouter(prefix="/internal", tags=["internal"])


def manejar_error_inesperado(e: Exception) -> None:
    trace_id = str(uuid.uuid4())[:8].upper()
    logger.error(f"Error {trace_id}: {str(e)}", exc_info=True)
    raise HTTPException(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        detail={
            "status": "error",
            "code": "E500",
            "message": f"Ha ocurrido un error inesperado. Por favor, contacte a soporte con el código {trace_id}.",
            "trace_id": trace_id
        }
    )


@recommend_router.post("/image", response_model=RespuestaAPI)
async def recommend_by_image(
    file: UploadFile = File(...),
    limit: int = Query(5, ge=1, le=20),
    categoria_id: int = Query(None)
) -> Dict[str, Any]:
    start_time = time.time()
    
    # 1. Validation
    # Check extension
    filename = file.filename.lower()
    if not (filename.endswith('.jpg') or filename.endswith('.jpeg') or filename.endswith('.png') or filename.endswith('.webp')):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail={
                "status": "fail",
                "message": "El archivo no es una imagen válida (debe ser JPG, JPEG, PNG o WEBP)"
            }
        )

    # Check size
    content = await file.read()
    if len(content) > MAX_FILE_SIZE_MB * 1024 * 1024:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail={
                "status": "fail",
                "message": f"El archivo supera el límite de {MAX_FILE_SIZE_MB}MB"
            }
        )

    try:
        # 2. Encode image
        clip_service = get_clip_service()
        query_vector = clip_service.encode_image(content)

        # 3. DB Search
        db_service = get_db_service()
        db_results = db_service.search_similar_articles(query_vector, limit, categoria_id)

        # Format items
        items = [
            RecommendItem(
                id=row["id"],
                nombre=row["nombre"],
                tipo_articulo=row["tipo_articulo"],
                stock_total=row["stock_total"],
                score=round(float(row["score"]), 4)
            ) for row in db_results
        ]

        elapsed_ms = (time.time() - start_time) * 1000
        response_data = RecommendResponse(
            items=items,
            query_type="image",
            total_resultados=len(items),
            model_version="clip-vit-base-patch32",
            tiempo_ms=round(elapsed_ms, 2)
        )

        return {
            "status": "success",
            "data": response_data.model_dump(),
            "message": "Recomendaciones generadas exitosamente."
        }

    except Exception as e:
        manejar_error_inesperado(e)


@recommend_router.post("/text", response_model=RespuestaAPI)
async def recommend_by_text(payload: RecommendTextRequest) -> Dict[str, Any]:
    start_time = time.time()
    try:
        # 1. Encode text
        clip_service = get_clip_service()
        query_vector = clip_service.encode_text(payload.prompt)

        # 2. DB Search
        db_service = get_db_service()
        db_results = db_service.search_similar_articles(query_vector, payload.limit, payload.categoria_id)

        # Format items
        items = [
            RecommendItem(
                id=row["id"],
                nombre=row["nombre"],
                tipo_articulo=row["tipo_articulo"],
                stock_total=row["stock_total"],
                score=round(float(row["score"]), 4)
            ) for row in db_results
        ]

        elapsed_ms = (time.time() - start_time) * 1000
        response_data = RecommendResponse(
            items=items,
            query_type="text",
            total_resultados=len(items),
            model_version="clip-ViT-B-32-multilingual-v1",
            tiempo_ms=round(elapsed_ms, 2)
        )

        return {
            "status": "success",
            "data": response_data.model_dump(),
            "message": "Recomendaciones generadas exitosamente."
        }

    except ValueError as ve:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail={
                "status": "fail",
                "message": str(ve)
            }
        )
    except Exception as e:
        manejar_error_inesperado(e)


@internal_router.post("/reprocess/{articulo_id}", response_model=RespuestaAPI)
async def reprocess_article(articulo_id: int) -> Dict[str, Any]:
    try:
        db_service = get_db_service()
        conn = db_service.get_connection()
        
        # Check if article has a principal image in the database
        query = """
        SELECT id, url
        FROM imagen_articulo
        WHERE articulo_id = %s AND es_principal = true;
        """
        with conn.cursor() as cur:
            cur.execute(query, (articulo_id,))
            row = cur.fetchone()
            
        if not row:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={
                    "status": "fail",
                    "message": "Artículo no encontrado o sin imagen principal"
                }
            )
            
        imagen_id, imagen_url = row
        
        # Construct absolute image URL if stored as relative path
        from app.core.config import BACKEND_BASE_URL
        if imagen_url.startswith(('http://', 'https://')):
            absolute_image_url = imagen_url
        else:
            absolute_image_url = f"{BACKEND_BASE_URL.rstrip('/')}/{imagen_url.lstrip('/')}"
        
        # Publish event to REDIS_CHANNEL_PENDING to trigger the background processing
        redis = get_redis_ia()
        event = {
            "articulo_id": articulo_id,
            "imagen_id": imagen_id,
            "imagen_url": absolute_image_url,
            "es_principal": True,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        }
        await redis.publish(REDIS_CHANNEL_PENDING, json.dumps(event))
        logger.info(f"Published reprocessing event to Redis: {event}")
        
        response_data = ReprocessResponse(
            articulo_id=articulo_id,
            estado="PROCESANDO",
            mensaje="Solicitud de re-procesamiento aceptada"
        )
        
        return {
            "status": "success",
            "data": response_data.model_dump(),
            "message": "Reprocesamiento iniciado correctamente."
        }
        
    except HTTPException:
        raise
    except Exception as e:
        manejar_error_inesperado(e)
