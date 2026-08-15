from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from pydantic import BaseModel
import pdfplumber
import io
from tasks import process_document_task, translate_message_task
from celery.result import AsyncResult

app = FastAPI(title="Connecting-Dots AI Service")

class TranslateRequest(BaseModel):
    message: str
    target_language: str

@app.post("/api/v1/ai/ingest")
async def ingest_document(file: UploadFile = File(...)):
    text = ""
    if file.filename.endswith(".pdf"):
        content = await file.read()
        try:
            with pdfplumber.open(io.BytesIO(content)) as pdf:
                for page in pdf.pages:
                    page_text = page.extract_text()
                    if page_text:
                        text += page_text + "\n"
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Failed to parse PDF: {str(e)}")
    else:
        content = await file.read()
        text = content.decode("utf-8", errors="ignore")
    
    if not text.strip():
        raise HTTPException(status_code=400, detail="Document contains no readable text")
    
    task = process_document_task.delay(text)
    return {"task_id": task.id, "status": "Processing document"}

@app.post("/api/v1/ai/translate")
async def translate_message(request: TranslateRequest):
    task = translate_message_task.delay(request.message, request.target_language)
    return {"task_id": task.id, "status": "Translating message"}

@app.get("/api/v1/ai/status/{task_id}")
async def get_task_status(task_id: str):
    result = AsyncResult(task_id)
    if result.ready():
        return {"status": "COMPLETED", "result": result.result}
    return {"status": "PENDING"}
