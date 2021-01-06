from fastapi import FastAPI, HTTPException
from weco_datascience import http
from weco_datascience.batching import BatchExecutionQueue
from weco_datascience.image import get_image_from_url
from weco_datascience.logging import get_logger

from .palette_encoder import PaletteEncoder

logger = get_logger(__name__)

# Initialise encoder
logger.info("Initialising PaletteEncoder model")
palette_encoder = PaletteEncoder(
    palette_size=5, bin_sizes=[2, 4, 6, 8], sat_min=(10 / 256), val_min=(10 / 256)
)

# initialise API
logger.info("Starting API")
app = FastAPI(title="Palette extractor", description="extracts palettes")
logger.info("API started, awaiting requests")

batch_inferrer_queue = BatchExecutionQueue(palette_encoder, batch_size=4, timeout=0.5)


@app.get("/palette/")
async def main(query_url: str):
    try:
        image = await get_image_from_url(query_url, size=100)
    except ValueError as e:
        error_string = str(e)
        logger.error(error_string)
        raise HTTPException(status_code=404, detail=error_string)

    palette = await batch_inferrer_queue.execute(image)
    logger.info(f"extracted palette from url: {query_url}")

    return {"palette": palette}


@app.get("/healthcheck")
def healthcheck():
    return {"status": "healthy"}


@app.on_event("startup")
def on_startup():
    http.start_persistent_client_session()
    batch_inferrer_queue.start_worker()


@app.on_event("shutdown")
async def on_shutdown():
    await http.close_persistent_client_session()
    batch_inferrer_queue.stop_worker()
