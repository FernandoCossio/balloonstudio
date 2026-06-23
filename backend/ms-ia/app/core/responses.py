from typing import Any, Optional, List, Dict
from pydantic import BaseModel


class ErrorValidacion(BaseModel):
    campo: str
    codigo: str
    mensaje: str


class RespuestaExitosa(BaseModel):
    status: str = "success"
    data: Dict[str, Any]
    message: Optional[str] = None


class RespuestaFallo(BaseModel):
    status: str = "fail"
    data: Dict[str, Any]
    message: str


class RespuestaError(BaseModel):
    status: str = "error"
    code: str
    message: str
    trace_id: Optional[str] = None


def crear_respuesta_exitosa(
    data: Any,
    message: Optional[str] = None,
    data_key: Optional[str] = "resultado"
) -> Dict[str, Any]:
    if data_key is None:
        data_field = data
    else:
        data_field = {data_key: data}

    return {
        "status": "success",
        "data": data_field,
        "message": message
    }


def crear_respuesta_fallo(
    errores: List[ErrorValidacion],
    message: str = "Error en la validación de datos",
    data_extra: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    data = {
        "errores_validacion": [error.model_dump() for error in errores]
    }

    return {
        "status": "fail",
        "data": data,
        "message": message
    }




def crear_respuesta_error(
    code: str,
    message: str,
    trace_id: Optional[str] = None
) -> Dict[str, Any]:
    response = {
        "status": "error",
        "code": code,
        "message": message
    }
    
    if trace_id:
        response["trace_id"] = trace_id
    
    return response
