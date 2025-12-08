import io
import tempfile
from silero import silero_tts
from fastapi import APIRouter
from fastapi.responses import FileResponse
from app.schemas import models

router = APIRouter()

@router.post('/generate', response_class=FileResponse)
async def create_file(tts_info: models.TTSGenerate):
    try:
        model, _ = silero_tts('ru', speaker='v5_ru')
    except:
        return None
    
    buffer = io.BytesIO()
    model.save_wav(**tts_info.model_dump(), audio_path=buffer)
    
    buffer.seek(0)
    audio_bytes = buffer.read()
    buffer.close()

    # Создаем временный файл
    temp_file = tempfile.NamedTemporaryFile(
        suffix=".mp3",
        delete=False
    )
    
    # Записываем данные
    temp_file.write(audio_bytes)
    temp_file.close()

    return FileResponse(
        path=temp_file.name,
        filename="generated_audio.mp3",
        media_type="audio/wav"
    )

    # return FileResponse(audio_bytes, media_type='audio/wav')