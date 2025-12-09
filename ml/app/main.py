from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from app.api.endpoints import llm
from app.api.endpoints import tts
 
app = FastAPI()

app.include_router(llm.router, prefix="/api/v1/llm", tags=["LLM"])
app.include_router(tts.router, prefix="/api/v1/tts", tags=["TTS"])
 
@app.get("/")
async def root():
    return {"message": "ML Application API"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}