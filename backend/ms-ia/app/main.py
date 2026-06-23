from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes.recommendations import recommend_router, internal_router
from app.core.redis_ia import init_redis_ia, close_redis_ia, get_redis_ia
from app.services.clip_service import init_clip_service, get_clip_service
from app.services.db_service import init_db_service, get_db_service
from app.workers.embedding_worker import start_embedding_worker, stop_embedding_worker
from app.models.schemas import HealthResponse
from app.core.logging import get_logger

logger = get_logger("Main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Visual Recommendation Engine microservice...")
    
    logger.info("Initializing CLIPService...")
    init_clip_service()
    
    logger.info("Initializing DBService...")
    init_db_service()
    
    logger.info("Initializing Redis IA Connection...")
    await init_redis_ia()
    
    logger.info("Starting background EmbeddingWorker...")
    start_embedding_worker()
    
    logger.info("Microservice startup completed successfully. ✓")
    try:
        yield
    finally:
        logger.info("Shutting down Visual Recommendation Engine microservice...")
        logger.info("Stopping EmbeddingWorker...")
        await stop_embedding_worker()
        
        logger.info("Closing Redis connection...")
        await close_redis_ia()
        
        logger.info("Closing DB connection...")
        try:
            get_db_service().close()
        except Exception as e:
            logger.error(f"Error closing DB connection: {e}")
        
        logger.info("Microservice shutdown completed. ✓")


app = FastAPI(
    title="Motor de Recomendacion Visual (CLIP)",
    description="Microservicio de recomendación visual por imagen y texto utilizando CLIP y pgvector",
    version="1.0.0",
    lifespan=lifespan
)

origins = [
    "http://localhost:4200",
    "http://127.0.0.1:4200",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Main recommendation API
app.include_router(recommend_router, prefix="/api")

# Internal route for Spring Boot communication
app.include_router(internal_router)


@app.get("/api/v1/health", response_model=HealthResponse)
async def health_check():
    # Model loaded check
    model_loaded = False
    try:
        get_clip_service()
        model_loaded = True
    except Exception:
        pass

    # DB connection check
    db_connected = False
    articulos_con_embedding = 0
    articulos_pendientes = 0
    try:
        db = get_db_service()
        # Ping and get stats
        articulos_con_embedding, articulos_pendientes = db.get_health_stats()
        db_connected = True
    except Exception:
        pass

    # Redis connection check
    redis_connected = False
    try:
        redis = get_redis_ia()
        await redis.ping()
        redis_connected = True
    except Exception:
        pass

    status_str = "ok" if (model_loaded and db_connected and redis_connected) else "degraded"

    return HealthResponse(
        status=status_str,
        model_loaded=model_loaded,
        db_connected=db_connected,
        redis_connected=redis_connected,
        articulos_con_embedding=articulos_con_embedding,
        articulos_pendientes=articulos_pendientes,
        version="1.0.0"
    )
