from fastapi import APIRouter
from openai import OpenAI
from app.schemas import models

router = APIRouter()

system_promt = (
    "Ты опытный гид-краевед с 20 летним опытом, умеющий рассказать про любую достопримечательность максимально интересно и увлеченно.\n"
    "Чтобы описать место ты принимаешь параметры в следующем формате:\n"
    "Название: [название точки];\n"
    "Описание: [описание места];\n"
    "Факты: [факт 1, факт 2, ...].\n"
    "где содержимое внутри [] являются получаемым от пользователя значением.\n"
    "Для ответа ты формулируешь полученные параметры от пользователя в интересный рассказ. Если используешь в рассказе год - пиши год словами.\n"
    "Кроме рассказа в ответе больше ничего не пиши, даже приветственное сообщение.\n"
    "В качестве примера ответа на запрос используй следующий текст:\n"
    "Плотина Городского пруда на реке Исеть - это гидротехническое сооружение на реке Исеть в Екатеринбурге. Построена в 1723 году с образованием пруда, снабжавшего водой Екатеринбургский завод. Среди местных жителей за плотиной и прилегающей территорией Исторического сквера закрепилось название «Плотинка». Является традиционным местом массовых народных гуляний и праздников.\n"
)

llm_system_message = {
    "role": "system",
    "content": system_promt
}

@router.post('/generate', response_model=models.LLMResponse)
async def create_desc(promt: models.LLMCreate):
    try:
        client = OpenAI(
            base_url="https://openrouter.ai/api/v1",
            api_key='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',
        )
        model='amazon/nova-2-lite-v1:free'
    except:
        return None

    completion = client.chat.completions.create(
        model=model,
        web_search_options={
            "search_context_size": "high"
        },
        messages=[
            llm_system_message,
            {
                "role": "user",
                "content": (
                    f"Название: [{promt.poi_name}];\n"
                    f"Описание: [{promt.poi_description}];\n"
                    f"Факты: [{promt.poi_facts}];\n"
                )
            }
        ]
    )
    result = models.LLMResponse(poi_generated_text=completion.choices[0].message.content.replace('\n\n', ''))
    return result 