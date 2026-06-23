from typing import Any, List, Optional
from pydantic import BaseModel, Field


class RecommendTextRequest(BaseModel):
    prompt: str = Field(..., min_length=1, description="El texto a buscar en el catálogo de decoración")
    limit: int = Field(5, ge=1, le=20, description="El número máximo de recomendaciones")
    categoria_id: Optional[int] = Field(None, description="Filtro opcional por ID de categoría")


class RecommendItem(BaseModel):
    id: int
    nombre: str
    tipo_articulo: str
    stock_total: int
    score: float


class RecommendResponse(BaseModel):
    items: List[RecommendItem]
    query_type: str
    total_resultados: int
    model_version: str
    tiempo_ms: float


class ReprocessResponse(BaseModel):
    articulo_id: int
    estado: str
    mensaje: str


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    db_connected: bool
    redis_connected: bool
    articulos_con_embedding: int
    articulos_pendientes: int
    version: str


class RespuestaAPI(BaseModel):
    status: str
    data: Any
    message: Optional[str] = None
