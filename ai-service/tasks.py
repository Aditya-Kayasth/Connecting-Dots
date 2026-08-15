import json
from openai import OpenAI
from celery_app import celery_app
from config import settings

# Initialize xAI client
client = OpenAI(
    api_key=settings.xai_api_key,
    base_url="https://api.x.ai/v1",
)

SYSTEM_PROMPT = """You are a Business Analyst assistant. Analyze the following NGO problem description and return ONLY a valid JSON object with exactly two keys: "structuredProblem" (a clear technical summary of the problem) and "techCategory" (must be exactly one of: SOFTWARE_WEB, DATA_SCIENCE_ML, IOT_HARDWARE, PROCESS_AUTOMATION). Do not include any explanation or markdown."""

@celery_app.task
def process_document_task(text: str):
    response = client.chat.completions.create(
        model="grok-beta",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": text},
        ],
        temperature=0.0
    )
    content = response.choices[0].message.content
    try:
        # Sometimes LLMs wrap in markdown json block
        if content.startswith("```json"):
            content = content[7:-3].strip()
        elif content.startswith("```"):
            content = content[3:-3].strip()
        parsed = json.loads(content)
        return parsed
    except Exception as e:
        return {"error": f"Failed to parse xAI response: {str(e)}", "raw": content}

@celery_app.task
def translate_message_task(message: str, target_language: str):
    response = client.chat.completions.create(
        model="grok-beta",
        messages=[
            {"role": "system", "content": f"You are a translator. Translate the following text to {target_language}. Return ONLY the translated text, no conversational filler."},
            {"role": "user", "content": message},
        ],
        temperature=0.0
    )
    return {"translated_text": response.choices[0].message.content.strip()}
