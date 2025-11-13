from openai import OpenAI

llm_system_message = {
    "role": "system",
    "content": open('promt.txt', 'r', encoding='utf-8').read()
}

class LLMGenerator():
    def __init__(self, desc_text, place_tags, model='deepseek/deepseek-chat-v3.1:free'):
        self.client = OpenAI(
            base_url="https://openrouter.ai/api/v1",
            api_key=open(r'..\api_key.txt').readline(),
        )

        self.desc_text = desc_text
        self.model = model
        self.place_tags = place_tags

    def create_desc(self):
        completion = self.client.chat.completions.create(
            model=self.model,
            web_search_options={
                "search_context_size": "high"
            },
            messages=[
                llm_system_message,
                {
                    "role": "user",
                    "content": f"[{self.desc_text}; {', '.join(self.place_tags)}]"
                }
            ]
        )
        content = completion.choices[0].message.content
        return content.replace('\n\n')