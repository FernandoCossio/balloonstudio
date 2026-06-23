import io
import torch
import torch.nn.functional as F
import numpy as np
from PIL import Image
from pathlib import Path
from transformers import CLIPModel, CLIPProcessor
from sentence_transformers import SentenceTransformer
from app.core.config import CLIP_IMAGE_MODEL, CLIP_TEXT_MODEL
from app.core.logging import get_logger

logger = get_logger("CLIPService")


class CLIPService:
    """
    Wrapper for CLIP models to generate image and text embeddings.
    """
    DIMENSION = 512

    def __init__(self, device: str = None):
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"Initializing CLIPService on device: {self.device}")

        # Image Encoder
        logger.info(f"Loading image encoder ({CLIP_IMAGE_MODEL})...")
        self._modelo_imagen = CLIPModel.from_pretrained(CLIP_IMAGE_MODEL).to(self.device)
        self._procesador_imagen = CLIPProcessor.from_pretrained(CLIP_IMAGE_MODEL)
        self._modelo_imagen.eval()

        # Text Encoder (multilingual for Spanish)
        logger.info(f"Loading text encoder ({CLIP_TEXT_MODEL})...")
        self._modelo_texto = SentenceTransformer(CLIP_TEXT_MODEL, device=self.device)

        logger.info(f"CLIPService models loaded successfully. Dimension: {self.DIMENSION}")

    def encode_image(self, source: str | Path | bytes) -> list[float]:
        """
        Generates embedding for an image and normalizes it.
        """
        try:
            image = self._load_image(source)
            inputs = self._procesador_imagen(images=image, return_tensors="pt").to(self.device)

            with torch.no_grad():
                outputs = self._modelo_imagen.get_image_features(**inputs)
                if hasattr(outputs, "pooler_output") and outputs.pooler_output is not None:
                    features = outputs.pooler_output
                elif isinstance(outputs, torch.Tensor):
                    features = outputs
                else:
                    features = outputs[0]

            if features.dim() > 2:
                features = features[:, 0, :]

            features = F.normalize(features, p=2, dim=-1)
            return features.view(-1).cpu().tolist()
        except Exception as e:
            logger.error(f"Error encoding image: {e}", exc_info=True)
            raise ValueError(f"Could not encode image: {e}")

    QUERY_TEMPLATE = (
        "decoración de eventos: {prompt}. "
        "Estilo visual, colores, materiales y ambiente."
    )

    def encode_text(self, text: str) -> list[float]:
        """
        Generates embedding for a Spanish prompt and normalizes it.
        """
        text = text.strip()
        if not text:
            raise ValueError("Text prompt cannot be empty.")

        text_expanded = self.QUERY_TEMPLATE.format(prompt=text)
        try:
            embedding = self._modelo_texto.encode(
                text_expanded,
                normalize_embeddings=True,
                convert_to_numpy=True
            )
            return embedding.tolist()
        except Exception as e:
            logger.error(f"Error encoding text: {e}", exc_info=True)
            raise ValueError(f"Could not encode text: {e}")

    def _load_image(self, source: str | Path | bytes) -> Image.Image:
        try:
            if isinstance(source, bytes):
                return Image.open(io.BytesIO(source)).convert("RGB")
            return Image.open(source).convert("RGB")
        except Exception as e:
            raise ValueError(f"Failed to load image: {e}")


_clip_service = None


def init_clip_service(device: str = None) -> None:
    global _clip_service
    if _clip_service is None:
        _clip_service = CLIPService(device=device)


def get_clip_service() -> CLIPService:
    if _clip_service is None:
        raise RuntimeError("CLIPService is not initialized")
    return _clip_service
