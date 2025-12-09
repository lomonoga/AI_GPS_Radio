from pydantic import BaseModel
from typing import Literal

class LLMCreate(BaseModel):
    poi_name: str
    poi_description: str
    poi_facts: list[str]

class LLMResponse(BaseModel):
    poi_generated_text: str

class TTSGenerate(BaseModel):
    text: str
    speaker: Literal['aidar', 'baya'] = 'aidar'
    sample_rate: int = 24_000