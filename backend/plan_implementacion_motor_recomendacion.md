# Plan de Implementación: Motor de Recomendación Visual para Decoración de Eventos

> **Versión:** 2.0
> **Estado:** Diseño final aprobado — listo para implementación
> **Dominio:** Módulo de IA dentro del sistema de gestión de decoraciones de eventos
> **Principio rector:** Ninguna línea de código se escribe hasta que este documento esté completo y aprobado.

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Decisiones de diseño fundamentales](#2-decisiones-de-diseño-fundamentales)
3. [Arquitectura general del sistema](#3-arquitectura-general-del-sistema)
4. [Fase 0: Protocolo de fotografía](#4-fase-0-protocolo-de-fotografía)
5. [Fase 1: Prototipado en Google Colab](#5-fase-1-prototipado-en-google-colab)
6. [Fase 2: Arquitectura de producción](#6-fase-2-arquitectura-de-producción)
7. [Fase 3: Despliegue en nube](#7-fase-3-despliegue-en-nube)
8. [Fase 4: Evoluciones futuras](#8-fase-4-evoluciones-futuras)
9. [Cambios requeridos en el sistema principal (Java)](#9-cambios-requeridos-en-el-sistema-principal-java)
10. [Stack técnico consolidado](#10-stack-técnico-consolidado)
11. [Riesgos y mitigaciones](#11-riesgos-y-mitigaciones)
12. [Glosario técnico](#12-glosario-técnico)

---

## 1. Resumen ejecutivo

### Problema

El cliente de una empresa de decoración de eventos necesita encontrar artículos de inventario que se adapten a un estilo visual que tiene en mente. Ese proceso actualmente es manual.

### Solución

Un servicio de IA basado en el modelo CLIP que:
1. Genera automáticamente embeddings visuales cuando el admin sube imágenes de artículos.
2. Acepta como input una imagen de referencia o prompt de texto del cliente y devuelve los artículos del inventario ordenados por similitud visual y estilística.

### Contexto del sistema

Este módulo es un **servicio independiente** (Python / FastAPI) que se comunica con el sistema principal (Java / Spring Boot) exclusivamente a través de eventos Redis. No duplica datos del sistema principal. No tiene su propia base de datos de artículos.

### Restricciones conocidas

| Restricción | Detalle |
|---|---|
| Sin GPU en producción | Instancia cloud de tier gratuito (CPU únicamente) |
| Volumen de inventario | 100–500 artículos, crecimiento de 1–2 por semana |
| Almacenamiento de vectores | pgvector sobre PostgreSQL existente |
| Comunicación entre servicios | Redis Pub/Sub (a agregar a la infraestructura) |
| Idioma del cliente | Español (requiere modelo multilingüe para inputs de texto) |
| Créditos cloud disponibles | AWS, GCP y Azure |

---

## 2. Decisiones de diseño fundamentales

### 2.1 Estrategia de embedding: imagen-a-imagen

**Decisión:** los artículos del inventario se indexan como embeddings de imagen. El input del cliente (imagen o texto) se convierte al mismo espacio vectorial de 512 dimensiones y se compara por similitud coseno.

**Por qué no texto-a-texto:** CLIP fue entrenado en 400 millones de pares (imagen, caption en inglés). Su espacio vectorial es más rico y preciso en el eje imagen-imagen. Las descripciones textuales en español de los artículos producirían embeddings degradados con el modelo base.

**Por qué las imágenes del inventario son suficientes:** el campo `es_principal` en `ImagenArticulo` ya identifica la imagen representativa de cada artículo. Esa imagen es todo lo que CLIP necesita para construir el embedding.

### 2.2 Dos modelos, un solo espacio vectorial

| Encoder | Modelo | Uso |
|---|---|---|
| Imagen (inventario) | `openai/clip-vit-base-patch32` | Pre-cómputo offline al subir artículos |
| Imagen (cliente) | `openai/clip-vit-base-patch32` | Runtime por request |
| Texto (cliente, español) | `sentence-transformers/clip-ViT-B-32-multilingual-v1` | Runtime por request |

El modelo multilingüe tiene el mismo encoder de imagen que el base y un encoder de texto reentrenado para 50+ idiomas. Ambos producen vectores en el mismo espacio de 512 dimensiones, lo que los hace compatibles para la búsqueda por similitud.

### 2.3 pgvector sobre PostgreSQL, no ChromaDB

**Decisión:** los embeddings se almacenan en `ArticuloInventario.embedding_visual` como `vector(512)` nativo de pgvector.

| Criterio | pgvector | ChromaDB |
|---|---|---|
| Infraestructura adicional | Ninguna | Proceso y storage separados |
| Sincronización | El embedding vive en la misma fila del artículo | Requiere sync manual con la BD relacional |
| Consultas con filtros | `WHERE categoria_id = X ORDER BY embedding <=> $1` en una query | Filtros en ChromaDB + join manual en código |
| Consistencia | ACID, transaccional | Eventual, manual |
| Escala actual (500 art.) | Sequential scan < 5ms | Comparable |

### 2.4 Comunicación por eventos Redis, no polling

**Decisión:** Java publica un evento cuando el admin sube una imagen. El servicio de IA lo consume. El servicio de IA publica un evento de retorno con el resultado. Java lo consume y actualiza el estado.

**Por qué no polling:** el inventario crece 1–2 artículos por semana. Un worker consultando la BD cada N minutos desperdiciaría recursos y añadiría latencia innecesaria.

**Por qué no webhook síncrono:** el proceso de generación de embedding puede tardar varios segundos (descarga de imagen + inferencia en CPU). Un webhook síncrono bloquearía el request del admin.

### 2.5 Estado de procesamiento en ImagenArticulo

**Decisión:** el campo `procesado_ia` (booleano) se reemplaza por un campo `estado_ia` con los siguientes valores:

| Estado | Significado | Quién lo asigna |
|---|---|---|
| `PENDIENTE` | Imagen subida, evento publicado, esperando procesamiento | Java al guardar la imagen |
| `PROCESANDO` | El servicio de IA recibió el evento y está trabajando | Python al recibir el evento |
| `COMPLETADO` | Embedding generado y persistido exitosamente | Python vía evento de retorno → Java |
| `FALLIDO` | El procesamiento falló; se puede reintentar | Python vía evento de retorno → Java |

El estado `PROCESANDO` es crítico para el frontend: distingue "nadie ha visto esto aún" de "está en proceso". Si el servicio de IA cae entre `PROCESANDO` y `COMPLETADO`, el registro queda en `PROCESANDO` indefinidamente, lo cual es detectable y accionable (endpoint de re-procesamiento manual).

---

## 3. Arquitectura general del sistema

### Flujo de generación de embeddings (offline, por evento)

```
[Admin sube imagen desde panel]
        │
        ▼
[Java: guarda ImagenArticulo con estado_ia = PENDIENTE]
[Java: publica en Redis canal "ia:embedding:pending"]
        │
        ▼
[Python: recibe evento del canal]
[Python: publica en Redis canal "ia:embedding:result" con estado PROCESANDO]
        │
        ▼
[Java: recibe evento → UPDATE imagen_articulo SET estado_ia = PROCESANDO]
        │
        ▼
[Python: descarga imagen desde url]
[Python: CLIP encode_image → vector[512] → normalización L2]
        │
        ├── Éxito ──►  UPDATE articulo_inventario SET embedding_visual = $vector
        │               Publica evento: { estado: COMPLETADO }
        │
        └── Fallo ──►  Publica evento: { estado: FALLIDO, error: "..." }
        │
        ▼
[Java: recibe evento de retorno]
[Java: UPDATE imagen_articulo SET estado_ia = COMPLETADO | FALLIDO]
```

### Flujo de re-procesamiento manual

```
[Admin presiona "Re-analizar" en el panel]
        │
        ▼
[Java: POST interno a Python → /internal/reprocess/{articulo_id}]
[Java: UPDATE imagen_articulo SET estado_ia = PENDIENTE]
        │
        ▼
[Python: mismo flujo que el evento automático]
[Python: publica evento de retorno al terminar]
        │
        ▼
[Java: actualiza estado_ia según resultado]
```

### Flujo de recomendación (runtime, por request del cliente)

```
[Cliente envía imagen o texto]
        │
        ▼
[Python: CLIP encode → vector[512] normalizado]
        │
        ▼
[PostgreSQL + pgvector]
  SELECT ai.id, ai.nombre, ai.tipo_articulo, ai.stock_total,
         1 - (ai.embedding_visual <=> $query_vector) AS score
  FROM articulo_inventario ai
  WHERE ai.embedding_visual IS NOT NULL
    AND ai.is_deleted = false
  ORDER BY ai.embedding_visual <=> $query_vector
  LIMIT $top_k
        │
        ▼
[JSON con artículos + scores de similitud]
```

### Canales Redis

| Canal | Dirección | Publicador | Consumidor |
|---|---|---|---|
| `ia:embedding:pending` | Java → Python | Spring Boot | FastAPI subscriber |
| `ia:embedding:result` | Python → Java | FastAPI | Spring Boot subscriber |

### Estructura de los eventos

**Evento de entrada** (`ia:embedding:pending`):
```json
{
  "articulo_id": 42,
  "imagen_id": 17,
  "imagen_url": "https://storage.ejemplo.com/articulos/42/principal.jpg",
  "es_principal": true,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Evento de retorno** (`ia:embedding:result`):
```json
{
  "articulo_id": 42,
  "imagen_id": 17,
  "estado": "COMPLETADO",
  "timestamp": "2025-01-15T10:30:45Z",
  "error": null
}
```
```json
{
  "articulo_id": 42,
  "imagen_id": 17,
  "estado": "FALLIDO",
  "timestamp": "2025-01-15T10:30:45Z",
  "error": "Timeout al descargar la imagen después de 30s"
}
```

---

## 4. Fase 0: Protocolo de fotografía

La calidad del sistema de recomendación depende directamente de la calidad de las imágenes del inventario. CLIP extrae forma, color dominante, textura y proporciones. Un fondo blanco no es estético — es técnico: elimina ruido contextual del embedding.

### Equipamiento mínimo

| Elemento | Especificación mínima | Alternativa |
|---|---|---|
| Cámara | Smartphone 12MP+ | DSLR o mirrorless |
| Fondo | Tela o cartulina blanca mate 2×2m | Papel fotográfico blanco |
| Iluminación | Luz natural difusa (ventana grande) | 2 softboxes LED 45W |
| Soporte | Trípode | Superficie completamente estable |

### Configuración del espacio

```
      [Ventana / Luz difusa]
              │
              │  luz suave, sin sombras duras
              ▼
    ┌─────────────────────┐
    │   FONDO BLANCO      │  ← sin arrugas ni dobleces visibles
    │                     │
    │    [ARTÍCULO]       │  ← ocupa entre 60% y 80% del frame
    │                     │
    └─────────────────────┘
              ▲
         [Cámara]          ← altura según categoría del artículo
```

### Ángulos por categoría

| Categoría | Ángulo principal | Justificación |
|---|---|---|
| Sillas | 3/4 frontal (45°) | Muestra respaldo, patas y asiento simultáneamente |
| Mesas | Superior (picado 60°) | Revela forma y textura de la superficie |
| Mantelerías | Cenital (90°) | Muestra patrón, textura y color sin distorsión |
| Centros de mesa | Frontal a nivel del artículo | Captura altura y composición |
| Arcos y estructuras | Frontal completo | Muestra proporciones y forma general |
| Candelabros y lámparas | 3/4 frontal | Captura altura y diseño |
| Vajilla y cristalería | 3/4 superior | Muestra forma y transparencia |
| Telas y drapeados | Colgante frontal | Captura caída, textura y color |

### Especificaciones técnicas

| Parámetro | Valor |
|---|---|
| Resolución mínima de captura | 1200×1200 px |
| Resolución de entrega | 512×512 px |
| Formato | JPEG, calidad 90 |
| Espacio de color | sRGB |
| Artículo en frame | 60%–80% del área |
| Márgenes | Mínimo 10% en cada borde |

### Post-procesado obligatorio

```
[Foto original]
      │
      ▼
1. Recortar al artículo con margen 10%
      │
      ▼
2. Balancear exposición (fondo debe ser blanco, no gris)
      │
      ▼
3. Eliminar sombras duras si existen
      │
      ▼
4. Redimensionar a 512×512 px manteniendo proporción
   (padding blanco si el artículo es vertical, sin deformar)
      │
      ▼
5. Exportar como JPEG calidad 90
```

### Checklist de aceptación por imagen

- [ ] Fondo completamente blanco o muy próximo
- [ ] Artículo ocupa entre 60% y 80% del frame
- [ ] Imagen en foco, sin desenfoque de movimiento
- [ ] No hay objetos ajenos en el frame
- [ ] Resolución mínima de 512×512 px
- [ ] Ángulo corresponde al especificado para la categoría

### Cuando el artículo no se puede fotografiar

En orden de preferencia:

1. Imagen con fondo blanco del proveedor
2. Render 3D con Blender (gratuito, modelos disponibles en BlenderKit y Sketchfab)
3. Imagen de catálogo editorial con eliminación de fondo obligatoria (remove.bg, gratuito hasta 50/mes)

---

## 5. Fase 1: Prototipado en Google Colab

### Objetivo

Validar que el pipeline CLIP → pgvector produce recomendaciones de calidad aceptable antes de construir la infraestructura de producción. Corre completamente en Colab con GPU T4 gratuita.

### Dependencias

```
torch>=2.0.0
transformers>=4.35.0
sentence-transformers>=2.2.0
Pillow>=10.0.0
numpy>=1.24.0
psycopg2-binary>=2.9.0
pgvector>=0.2.0
```

### Módulos a implementar

#### `clip_encoder.py`

Responsabilidad única: cargar los modelos y exponer:
- `encode_image(path_o_bytes) → list[float]`
- `encode_text(text: str) → list[float]`

Ambos devuelven un vector de 512 dimensiones normalizado con L2.

Reglas de implementación:
- Usar `get_image_features()` y `get_text_features()` directamente, nunca `forward()`.
- `encode_image` usa `clip-vit-base-patch32`.
- `encode_text` usa `clip-ViT-B-32-multilingual-v1`.
- La normalización L2 es obligatoria antes de retornar cualquier vector.
- Ambos modelos se cargan una sola vez al instanciar la clase.

**Query expansion en `encode_text`:** el modelo multilingüe no tiene conocimiento implícito del dominio de decoración de eventos. Un prompt corto como "rústico y elegante" es ambiguo fuera de contexto. Se aplica un template de expansión antes de encodear:

```python
QUERY_TEMPLATE = (
    "decoración de eventos: {prompt}. "
    "Estilo visual, colores, materiales y ambiente."
)
```

El prompt del cliente se inserta en `{prompt}` antes de pasarlo al encoder. Esto ancla la búsqueda semántica al dominio correcto sin modificar lo que el usuario escribe. El template solo se aplica al encoder de texto, nunca al encoder de imagen.

**Filtro pre-retrieval en `EmbeddingStore.buscar()`:** el método acepta un parámetro opcional `filtro_tipo` que restringe la búsqueda a artículos de una categoría específica antes de calcular similitudes. Esto replica el `WHERE tipo_articulo = $tipo` de la query pgvector en producción. El filtro se aplica sobre la lista de candidatos antes de construir la matriz de embeddings, evitando comparaciones innecesarias.

```python
def buscar(self, query_embedding, top_k=5, filtro_tipo=None):
    articulos_candidatos = self._articulos
    if filtro_tipo:
        articulos_candidatos = [
            a for a in articulos_candidatos
            if a.metadata.get("tipo") == filtro_tipo
            or a.metadata.get("tipo_articulo") == filtro_tipo
        ]
```

#### `seeder_colab.py`

Script para poblar embeddings en la BD desde Colab durante la fase de validación. Consulta artículos con imagen principal y `estado_ia != COMPLETADO`, genera el embedding y hace UPDATE directo vía psycopg2.

#### `recommender_colab.py`

Expone `recommend_by_image(path, top_k)` y `recommend_by_text(prompt, top_k)`. Ejecuta la query pgvector y retorna lista de dicts con `id`, campos del artículo y `score`.

### Protocolo de validación cualitativa

La validación no es automática — la realiza alguien con criterio de decoración.

| # | Input | Expectativa mínima |
|---|---|---|
| T-01 | Imagen: boda estilo bohemio | Top-5 contiene ≥3 artículos de estilo rústico, natural o boho |
| T-02 | Imagen: evento corporativo (mesas blancas, moderno) | Top-5 contiene artículos formales o minimalistas |
| T-03 | Imagen: quinceañera (rosas, dorados, tul) | Top-5 contiene artículos románticos o con dorados |
| T-04 | Texto: "quiero algo rústico pero elegante" | Top-5 mezcla artículos de estilo rústico y elegante |
| T-05 | Texto: "decoración minimalista en blanco" | Top-5 incluye artículos minimalistas o blancos |
| T-06 | Imagen de un artículo del propio inventario | El artículo aparece en posición #1 con score > 0.95 |

**Criterio de aprobación:** mínimo 4 de 6 casos pasan. Si T-06 falla, la fase no se aprueba independientemente del resto — indica un error en el pipeline.

**Si los resultados son malos:** el problema suele estar en la calidad de las imágenes (fondos ruidosos, ángulos inconsistentes), no en el código. Revisar el dataset antes de tocar parámetros del modelo.

---

## 6. Fase 2: Arquitectura de producción

### Estructura del servicio Python

```
recommendation-service/
├── app/
│   ├── __init__.py
│   ├── main.py                  ← FastAPI app + lifespan
│   ├── config.py                ← settings con pydantic-settings
│   ├── models.py                ← schemas Pydantic request/response/eventos
│   ├── services/
│   │   ├── __init__.py
│   │   ├── clip_service.py      ← wrapper del modelo (singleton via lifespan)
│   │   ├── db_service.py        ← conexión PostgreSQL + queries pgvector
│   │   └── redis_service.py     ← publisher y subscriber de eventos
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── recommend.py         ← POST /api/v1/recommend/*
│   │   └── internal.py          ← POST /internal/reprocess/{articulo_id}
│   └── workers/
│       └── embedding_worker.py  ← suscriptor Redis, lógica de generación
├── tests/
│   ├── test_clip_service.py
│   ├── test_db_service.py
│   ├── test_recommend_endpoints.py
│   └── test_embedding_worker.py
├── Dockerfile
├── .dockerignore
├── requirements.txt
├── requirements-dev.txt
└── .env.example
```

### Patrón de carga del modelo (lifespan)

El modelo CLIP, la conexión a PostgreSQL y la conexión a Redis se inicializan **una sola vez** al arrancar el proceso FastAPI y se almacenan en `app.state`. El worker de embeddings corre en un task asíncrono en segundo plano dentro del mismo proceso.

Si cualquiera de los tres componentes falla en startup, el proceso debe fallar ruidosamente con log de error claro y no arrancar el servidor.

### Endpoints

#### `POST /api/v1/recommend/image`

**Request:** `multipart/form-data`
- `file`: imagen binaria (JPEG, PNG, WEBP). Máximo 10MB.
- `limit`: integer, opcional, default 5, máximo 20.
- `tipo_articulo`: string, opcional, filtro por tipo de artículo.

**Response:**
```json
{
  "items": [
    {
      "id": 42,
      "nombre": "Silla Tiffany dorada",
      "tipo_articulo": "SILLA",
      "stock_total": 120,
      "score": 0.87
    }
  ],
  "query_type": "image",
  "total_resultados": 5,
  "model_version": "clip-vit-base-patch32",
  "tiempo_ms": 312
}
```

#### `POST /api/v1/recommend/text`

**Request:**
```json
{
  "prompt": "quiero una decoración rústica pero elegante para boda campestre",
  "limit": 5,
  "tipo_articulo": "SILLA"
}
```

**Response:** misma estructura, con `"query_type": "text"` y `"model_version": "clip-ViT-B-32-multilingual-v1"`.

#### `POST /internal/reprocess/{articulo_id}`

**Uso:** el backend Java llama este endpoint cuando el admin presiona "Re-analizar" en el panel. No está expuesto públicamente.

**Response:**
```json
{
  "articulo_id": 42,
  "estado": "PROCESANDO",
  "mensaje": "Solicitud de re-procesamiento aceptada"
}
```

El resultado final llega vía evento Redis `ia:embedding:result`, no en la respuesta de este endpoint. El endpoint solo confirma que la solicitud fue recibida.

#### `GET /api/v1/health`

```json
{
  "status": "ok",
  "model_loaded": true,
  "db_connected": true,
  "redis_connected": true,
  "articulos_con_embedding": 342,
  "articulos_pendientes": 3,
  "version": "1.0.0"
}
```

### Comportamiento en errores

| Situación | HTTP Status | Mensaje |
|---|---|---|
| Imagen corrupta o no legible | 422 | `"El archivo no es una imagen válida"` |
| Imagen mayor a 10MB | 413 | `"El archivo supera el límite de 10MB"` |
| Prompt vacío | 422 | `"El prompt no puede estar vacío"` |
| `limit` fuera de rango [1,20] | 422 | `"El parámetro limit debe estar entre 1 y 20"` |
| Sin artículos con embedding | 503 | `"No hay artículos indexados. Procese las imágenes del inventario."` |
| Artículo no encontrado en reprocess | 404 | `"Artículo no encontrado o sin imagen principal"` |
| Error interno del modelo | 500 | `"Error interno al procesar el input"` |

### Query pgvector de recomendación

```sql
SELECT
    ai.id,
    ai.nombre,
    ai.tipo_articulo,
    ai.stock_total,
    1 - (ai.embedding_visual <=> %s::vector) AS score
FROM articulo_inventario ai
WHERE ai.embedding_visual IS NOT NULL
  AND ai.is_deleted = false
  AND (%s IS NULL OR ai.tipo_articulo = %s)
ORDER BY ai.embedding_visual <=> %s::vector
LIMIT %s;
```

El operador `<=>` es la distancia coseno de pgvector. `1 - distancia` convierte a score de similitud donde 1.0 = idénticos.

---

## 7. Fase 3: Despliegue en nube

### Plataforma: Google Cloud Run

**Tier gratuito:** 2,000,000 requests/mes y 360,000 GB-segundo de CPU/mes.

### Configuración del contenedor

| Parámetro | Valor | Razón |
|---|---|---|
| Memoria | 2 GB | CLIP ~600MB + PyTorch ~800MB de trabajo |
| CPU | 2 vCPU | Inferencia CLIP se beneficia de paralelismo |
| Timeout | 60 segundos | Margen para cold start |
| Concurrencia | 4 requests/instancia | Balance paralelismo/memoria |
| Instancias mínimas | 0 (escala a cero) | Conservar créditos |
| Puerto | 8080 | Estándar Cloud Run |

### El modelo va en la imagen Docker

El modelo se descarga durante el `docker build`, no en el arranque del contenedor. La imagen resultante pesa ~2.5 GB pero el cold start es de ~2-3 segundos en vez de 2-3 minutos.

### Variables de entorno requeridas

```bash
DATABASE_URL=postgresql://user:pass@host:5432/dbname
REDIS_URL=redis://host:6379
REDIS_CHANNEL_PENDING=ia:embedding:pending
REDIS_CHANNEL_RESULT=ia:embedding:result
LOG_LEVEL=INFO
MAX_FILE_SIZE_MB=10
DEFAULT_TOP_K=5
APP_VERSION=1.0.0
```

### Consideración de red

Cloud Run necesita acceso a PostgreSQL y Redis. Las opciones en orden de simplicidad:
1. **Cloud SQL** (PostgreSQL gestionado de GCP) con Cloud Run en la misma región — conexión directa.
2. **Redis en Cloud Memorystore** o instancia Redis externa — requiere configurar VPC connector en Cloud Run.

---

## 8. Fase 4: Evoluciones futuras

### 8.1 Fusión multimodal (imagen + texto simultáneo)

Permite que el cliente envíe imagen de referencia y texto modificador al mismo tiempo. Ejemplo: foto de una boda campestre + "pero con sillas doradas en vez de rústicas".

**Implementación:** interpolación lineal ponderada de embeddings, con query expansion aplicada al texto antes de encodear.

```
embedding_final = normalize(α × embedding_imagen + (1-α) × encode_text(QUERY_TEMPLATE.format(prompt)))
```

Valor recomendado: `α = 0.7` (imagen tiene más peso). Nuevo endpoint `POST /api/v1/recommend/multimodal`.

### 8.2 Incorporación de Reranking (Cross-Encoder)

CLIP es un **Bi-Encoder**: encodea la query y cada artículo por separado y luego compara los vectores. Esto es muy eficiente pero pierde el contexto cruzado — el modelo nunca "ve" el par (query, artículo) simultáneamente al decidir la similitud.

Un **Cross-Encoder** recibe el par completo `(query, candidato)` y produce un score de relevancia con mayor precisión semántica. El tradeoff es velocidad: no es viable correr un cross-encoder sobre todo el inventario en cada request.

**Pipeline de dos etapas:**

```
Etapa 1 — Retrieval (CLIP Bi-Encoder):
  query → vector[512] → pgvector → top-20 candidatos
  Ultra rápido: <5ms sobre 500 artículos

Etapa 2 — Reranking (Cross-Encoder):
  Para cada uno de los 20 candidatos:
    score = cross_encoder(query, candidato)
  Retornar top-5 reordenados por score del cross-encoder
  Más lento pero opera sobre 20 artículos, no sobre 500
```

**Prerrequisito:** esta mejora depende del Feedback loop (8.3). El cross-encoder se fine-tunea con los pares `(query, artículo_elegido)` acumulados. Sin esos datos de entrenamiento del dominio, un cross-encoder genérico no aportará mejora significativa. La secuencia correcta es: lanzar sistema base → acumular feedback → fine-tunear cross-encoder → incorporar al pipeline.

**Impacto esperado:** mejora la precisión del top-5 final, especialmente en casos donde CLIP recupera los candidatos correctos pero en orden subóptimo.

### 8.3 Feedback loop y fine-tuning

Registrar qué artículo elige finalmente el cliente después de recibir recomendaciones. Los pares `(query, artículo_elegido)` acumulados sirven para dos propósitos:

1. **Fine-tuning de CLIP:** especializa el modelo al dominio de decoraciones. Con ~500 pares el proceso corre en Colab (GPU T4) en menos de 1 hora.
2. **Datos de entrenamiento del Cross-Encoder (8.2):** pares positivos = `(query, artículo_elegido)`, pares negativos = `(query, artículo_mostrado_pero_no_elegido)`.

### 8.4 Índice ivfflat cuando el inventario escale

Para más de 5,000 artículos, activar el índice aproximado de pgvector:

```sql
CREATE INDEX ON articulo_inventario
USING ivfflat (embedding_visual vector_cosine_ops)
WITH (lists = 100);
```

Para 500 artículos el sequential scan es más rápido. El índice se activa cuando el tiempo de query supere los 100ms.

### 8.5 Migración a Qdrant

Si el inventario supera los 5,000 artículos y pgvector muestra limitaciones, la migración a Qdrant es directa: los embeddings generados por CLIP no cambian, solo cambia el cliente de base de datos vectorial.

---

## 9. Cambios requeridos en el sistema principal (Java)

### 9.1 Cambio en ImagenArticulo

Reemplazar el campo `procesado_ia` (booleano) por `estado_ia` con los valores `PENDIENTE`, `PROCESANDO`, `COMPLETADO`, `FALLIDO`.

**Entidad actualizada:**
```java
public enum EstadoIa {
    PENDIENTE, PROCESANDO, COMPLETADO, FALLIDO
}

// En ImagenArticulo.java
// Eliminar:
@Column(name = "procesado_ia")
private Boolean procesadoIa = false;

// Agregar:
@Enumerated(EnumType.STRING)
@Column(name = "estado_ia", length = 20)
private EstadoIa estadoIa = EstadoIa.PENDIENTE;
```

**Migración SQL:**
```sql
ALTER TABLE imagen_articulo
DROP COLUMN procesado_ia;

ALTER TABLE imagen_articulo
ADD COLUMN estado_ia VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
```

### 9.2 Cambio en ArticuloInventario

Reemplazar el campo `embedding_visual TEXT` por tipo nativo `vector(512)` de pgvector.

**Dependencia en pom.xml:**
```xml
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>
```

**Entidad actualizada:**
```java
import com.pgvector.PGvector;

// En ArticuloInventario.java
// Reemplazar:
@Column(name = "embedding_visual", columnDefinition = "TEXT")
private String embeddingVisual;

// Por:
@Column(name = "embedding_visual", columnDefinition = "vector(512)")
private PGvector embeddingVisual;
```

**Migración SQL:**
```sql
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE articulo_inventario
ALTER COLUMN embedding_visual TYPE vector(512)
USING NULL;
```

El campo queda en `NULL` para todos los artículos existentes. El worker de embeddings los procesará cuando tengan imagen principal disponible.

> **Nota:** el backend Java en la práctica nunca escribe `embedding_visual` — lo escribe el servicio de IA vía SQL directo con psycopg2. El mapeo JPA es para consistencia del modelo de dominio.

### 9.3 Agregar Redis (Spring Boot)

**Dependencia en pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Responsabilidades de Java sobre Redis:**

| Acción | Canal | Momento |
|---|---|---|
| Publicar evento de imagen subida | `ia:embedding:pending` | Después de guardar `ImagenArticulo` exitosamente |
| Suscribirse a eventos de retorno | `ia:embedding:result` | Al arrancar la aplicación |
| Actualizar `estado_ia` según retorno | — | Al recibir evento con `COMPLETADO` o `FALLIDO` |

Solo se publican eventos para imágenes con `es_principal = true`. Si el admin sube múltiples imágenes para un artículo, solo la principal dispara el proceso de embedding.

---

## 10. Stack técnico consolidado

| Componente | Herramienta | Versión mínima |
|---|---|---|
| Modelo embeddings (imagen) | `openai/clip-vit-base-patch32` | — |
| Modelo embeddings (texto ES) | `clip-ViT-B-32-multilingual-v1` | — |
| Framework deep learning | PyTorch | 2.0.0 |
| Librería del modelo | Hugging Face Transformers | 4.35.0 |
| Búsqueda vectorial | pgvector sobre PostgreSQL | 0.5.0 |
| Driver PostgreSQL (Python) | psycopg2-binary | 2.9.0 |
| Librería pgvector (Python) | pgvector | 0.2.0 |
| Backend API | FastAPI + Pydantic v2 | 0.104.0 |
| Servidor ASGI | Uvicorn | 0.24.0 |
| Bus de eventos | Redis Pub/Sub | 7.0 |
| Cliente Redis (Python) | redis-py | 5.0.0 |
| Cliente Redis (Java) | Spring Data Redis | (spring-boot managed) |
| Mapeo pgvector (Java) | pgvector-java | 0.1.6 |
| Contenedor | Docker | 24.0+ |
| Despliegue | Google Cloud Run | — |
| Prototipado | Google Colab (T4) | — |
| Procesamiento imágenes | Pillow | 10.0.0 |

---

## 11. Riesgos y mitigaciones

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Calidad deficiente de imágenes | Alta | Alto | Protocolo de fotografía + checklist de aceptación (Fase 0) |
| Artículo queda en estado PROCESANDO indefinidamente | Media | Medio | Endpoint `/internal/reprocess` + visibilidad del estado en el panel admin |
| Cold start inaceptable en Cloud Run | Media | Bajo | Configurar `--min-instances=1` si es necesario |
| Redis no disponible al subir imagen | Baja | Medio | Java debe manejar el error y dejar el artículo en `PENDIENTE`; el admin puede re-procesar |
| Imagen no descargable desde la URL | Media | Bajo | Python captura el error, publica evento `FALLIDO` con descripción del error |
| CLIP degradado con texto en español | Baja | Bajo | Mitigado por uso del modelo multilingüe |
| Inventario crece más allá de 5,000 artículos | Baja | Medio | Activar índice `ivfflat` en pgvector (ver sección 8.4) |

---

## 12. Glosario técnico

| Término | Definición |
|---|---|
| **Embedding** | Vector numérico de 512 dimensiones que representa el contenido visual de una imagen o semántico de un texto según el modelo CLIP |
| **Espacio de embeddings** | Espacio matemático de 512 dimensiones donde viven todos los vectores. Imágenes visualmente similares quedan geométricamente cercanas |
| **Similitud coseno** | Métrica que mide el ángulo entre dos vectores. 1.0 = idénticos, 0.0 = sin relación |
| **Normalización L2** | Escala un vector para que tenga longitud 1. Permite usar producto punto como equivalente a similitud coseno |
| **CLIP** | Contrastive Language-Image Pretraining. Modelo de OpenAI entrenado en 400M pares (imagen, texto) que proyecta imagen y texto al mismo espacio vectorial |
| **pgvector** | Extensión de PostgreSQL que agrega el tipo `vector(n)` y operadores de búsqueda por similitud (`<=>` para coseno) |
| **Operador `<=>`** | Operador de pgvector que computa distancia coseno entre dos vectores. `1 - (a <=> b)` da el score de similitud |
| **Redis Pub/Sub** | Mecanismo de mensajería de Redis donde los publicadores envían mensajes a canales y los suscriptores los reciben en tiempo real |
| **Lifespan (FastAPI)** | Patrón para cargar recursos costosos (modelo, conexiones) una sola vez al iniciar el servidor |
| **Drop-in replacement** | Reemplazo directo de un componente por otro con la misma interfaz. El modelo multilingüe es un drop-in del encoder de texto base |
| **ivfflat** | Índice aproximado de pgvector basado en clustering. Acelera búsquedas vectoriales a costa de precisión mínima; se activa a partir de ~5,000 artículos |
| **Sequential scan** | PostgreSQL recorre todas las filas para calcular similitud. Más rápido que el índice para volúmenes pequeños (<5,000 artículos) |
| **estado_ia** | Campo de `ImagenArticulo` que indica el estado del procesamiento de embedding: `PENDIENTE`, `PROCESANDO`, `COMPLETADO` o `FALLIDO` |
| **Query expansion** | Técnica que enriquece el prompt del usuario con contexto de dominio antes de encodear. En este sistema se aplica el `QUERY_TEMPLATE` para anclar la búsqueda semántica al dominio de decoración de eventos |
| **Bi-Encoder** | Arquitectura donde query y documentos se encodean por separado. CLIP es un Bi-Encoder. Muy eficiente pero sin contexto cruzado entre query y candidato |
| **Cross-Encoder** | Arquitectura donde el par (query, candidato) se evalúa conjuntamente. Mayor precisión que el Bi-Encoder pero no escala a búsqueda sobre todo el inventario. Se usa como etapa de reranking sobre los top-K candidatos del Bi-Encoder |
| **Reranking** | Segunda etapa del pipeline de recomendación: toma los top-20 candidatos del retrieval inicial y los reordena con un Cross-Encoder para obtener los top-5 definitivos con mayor precisión |

---

*Versión 2.0 — Documento de diseño final. Actualizar antes de cualquier cambio de arquitectura o stack.*
