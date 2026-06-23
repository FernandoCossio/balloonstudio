
## 1. Configuración y ejecución local

### 1.1 Requisitos previos
- Python 3.10 o superior.
- Docker (para el contenedor de Redis).

### 1.2 Levantar Redis Stack
El servicio requiere Redis para cachear los embeddings de las vacantes. Puedes levantarlo usando Docker:

```bash
docker run -d   -p 6380:6379   -p 8002:8001   --name redis-stack-ia   -e REDIS_ARGS="--requirepass tu-clave-segura"   redis/redis-stack
```

### 1.3 Configuración de entorno
1. Crea un archivo `.env` en la raíz del proyecto basado en el ejemplo:
   ```bash
   cp .env.example .env
   ```
2. Asegúrate de que las variables en `.env` coincidan con tu configuración de Redis (por defecto apunta a `localhost:6380`).

### 1.4 Instalación y ejecución
1. Crear un entorno virtual:
   ```bash
   python -m venv venv
   ```
2. Activar el entorno virtual:
   - **Windows**: `.\venv\Scripts\activate`
   - **Linux/macOS**: `source venv/bin/activate`
3. Instalar dependencias:
   ```bash
   pip install -r requirements.txt
   ```
4. Iniciar el servidor de desarrollo:
   ```bash
   uvicorn app.main:app --reload --port 8000
   ```

---

## 2. Probar la API

Con el contenedor en ejecución, puedes acceder a:

- Documentación interactiva (Swagger UI):

  ```text
  http://localhost:8000/docs
  ```

- Documentación alternativa (ReDoc):

  ```text
  http://localhost:8000/redoc
  ```