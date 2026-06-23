import psycopg2
from psycopg2.extras import RealDictCursor
from pgvector.psycopg2 import register_vector
from app.core.config import DATABASE_URL
from app.core.logging import get_logger

logger = get_logger("DBService")


class DBService:
    """
    Handles database operations for storing visual embeddings and executing similarity queries.
    """

    def __init__(self, dsn: str = DATABASE_URL):
        self.dsn = dsn
        self._conn = None

    def get_connection(self):
        """
        Returns a database connection. Automatically reconnects if closed.
        """
        if self._conn is None or self._conn.closed != 0:
            try:
                logger.info("Establishing new connection to PostgreSQL database...")
                self._conn = psycopg2.connect(self.dsn)
                # Ensure the vector extension is created in the database
                with self._conn.cursor() as cur:
                    cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
                self._conn.commit()
                # Register pgvector type on the connection
                register_vector(self._conn)
                logger.info("Connected to database successfully. pgvector registered.")
            except Exception as e:
                logger.error(f"Failed to connect to database: {e}", exc_info=True)
                raise RuntimeError(f"Database connection error: {e}")
        return self._conn

    def close(self):
        if self._conn is not None and self._conn.closed == 0:
            self._conn.close()
            logger.info("Database connection closed.")

    def search_similar_articles(self, query_vector: list[float], limit: int = 5, categoria_id: int = None) -> list[dict]:
        """
        Searches for similar articles in the database using the pgvector cosine distance operator.
        """
        conn = self.get_connection()
        query = """
        SELECT
            ai.id,
            ai.nombre,
            ai.tipo_articulo,
            ai.stock_total,
            1 - (ai.embedding_visual::vector <=> %s::vector) AS score
        FROM articulo_inventario ai
        WHERE ai.embedding_visual IS NOT NULL
          AND ai.is_deleted = false
          AND (
              %s IS NULL OR EXISTS (
                  SELECT 1 FROM categoria_inventario ci
                  WHERE ci.inventario_id = ai.id AND ci.categoria_id = %s
              )
          )
        ORDER BY ai.embedding_visual::vector <=> %s::vector
        LIMIT %s;
        """
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(query, (query_vector, categoria_id, categoria_id, query_vector, limit))
                results = cur.fetchall()
                # Return standard list of dicts
                return [dict(row) for row in results]
        except Exception as e:
            logger.error(f"Error querying similar articles: {e}", exc_info=True)
            conn.rollback()
            raise RuntimeError(f"Database query error: {e}")

    def update_article_embedding(self, articulo_id: int, embedding: list[float]) -> bool:
        """
        Updates the visual embedding vector of a specific article.
        """
        conn = self.get_connection()
        query = """
        UPDATE articulo_inventario
        SET embedding_visual = %s
        WHERE id = %s AND is_deleted = false;
        """
        try:
            # Format the list of floats as a pgvector-compatible string '[v1, v2, ...]'
            # This ensures compatibility whether the column is TEXT or native vector type.
            embedding_str = "[" + ",".join(map(str, embedding)) + "]"
            with conn.cursor() as cur:
                cur.execute(query, (embedding_str, articulo_id))
                updated = cur.rowcount > 0
                conn.commit()
                if updated:
                    logger.info(f"Successfully updated embedding for article ID {articulo_id}")
                else:
                    logger.warning(f"No active article found with ID {articulo_id} to update embedding")
                return updated
        except Exception as e:
            logger.error(f"Error updating article embedding: {e}", exc_info=True)
            conn.rollback()
            raise RuntimeError(f"Database update error: {e}")

    def update_image_state(self, imagen_id: int, estado: str) -> bool:
        """
        Updates the estado_ia of a specific image in imagen_articulo.
        """
        conn = self.get_connection()
        query = """
        UPDATE imagen_articulo
        SET estado_ia = %s
        WHERE id = %s AND is_deleted = false;
        """
        try:
            with conn.cursor() as cur:
                cur.execute(query, (estado, imagen_id))
                updated = cur.rowcount > 0
                conn.commit()
                if updated:
                    logger.info(f"Successfully updated image ID {imagen_id} state to {estado}")
                else:
                    logger.warning(f"No active image found with ID {imagen_id} to update state")
                return updated
        except Exception as e:
            logger.error(f"Error updating image state: {e}", exc_info=True)
            conn.rollback()
            raise RuntimeError(f"Database update image state error: {e}")


    def get_health_stats(self) -> tuple[int, int]:
        """
        Returns count of (articles_with_embedding, pending_images).
        Returns (0, 0) if query fails (e.g. migration not run yet).
        """
        try:
            conn = self.get_connection()
            with conn.cursor() as cur:
                cur.execute("SELECT COUNT(*) FROM articulo_inventario WHERE embedding_visual IS NOT NULL AND is_deleted = false;")
                con_emb = cur.fetchone()[0]
                
                # Check if state column/table exists first by executing and catching specific errors
                cur.execute("SELECT COUNT(*) FROM imagen_articulo WHERE estado_ia = 'PENDIENTE';")
                pendientes = cur.fetchone()[0]
                return con_emb, pendientes
        except Exception as e:
            logger.warning(f"Database health query failed (possibly pending migrations): {e}")
            try:
                self.get_connection().rollback()
            except Exception:
                pass
            return 0, 0



_db_service = None


def init_db_service() -> None:
    global _db_service
    if _db_service is None:
        _db_service = DBService()
        # Verify connection on startup
        _db_service.get_connection()


def get_db_service() -> DBService:
    if _db_service is None:
        raise RuntimeError("DBService is not initialized")
    return _db_service
