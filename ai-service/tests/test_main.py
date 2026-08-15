import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from main import app
import json

client = TestClient(app)

@patch("tasks.client")
@patch("tasks.process_document_task.delay")
def test_ingest_document(mock_delay, mock_openai):
    # Mocking celery delay
    mock_task = MagicMock()
    mock_task.id = "fake-task-id"
    mock_delay.return_value = mock_task

    file_content = b"This is a test document text."
    files = {"file": ("test.txt", file_content, "text/plain")}
    response = client.post("/api/v1/ai/ingest", files=files)
    
    assert response.status_code == 200
    assert response.json() == {"task_id": "fake-task-id", "status": "Processing document"}
    mock_delay.assert_called_once_with("This is a test document text.")

@patch("tasks.client")
@patch("tasks.translate_message_task.delay")
def test_translate_message(mock_delay, mock_openai):
    mock_task = MagicMock()
    mock_task.id = "fake-translate-task-id"
    mock_delay.return_value = mock_task

    payload = {"message": "Hello world", "target_language": "Spanish"}
    response = client.post("/api/v1/ai/translate", json=payload)
    
    assert response.status_code == 200
    assert response.json() == {"task_id": "fake-translate-task-id", "status": "Translating message"}
    mock_delay.assert_called_once_with("Hello world", "Spanish")

@patch("tasks.client.chat.completions.create")
def test_celery_process_document_task_logic(mock_create):
    from tasks import process_document_task
    
    mock_response = MagicMock()
    mock_response.choices[0].message.content = '{"structuredProblem": "Need help", "techCategory": "SOFTWARE_WEB"}'
    mock_create.return_value = mock_response
    
    # Not calling .delay, but calling the function directly to test the synchronous logic
    result = process_document_task("Some text")
    
    assert "structuredProblem" in result
    assert result["structuredProblem"] == "Need help"

@patch("tasks.client.chat.completions.create")
def test_celery_translate_message_task_logic(mock_create):
    from tasks import translate_message_task
    
    mock_response = MagicMock()
    mock_response.choices[0].message.content = "Hola mundo"
    mock_create.return_value = mock_response
    
    result = translate_message_task("Hello world", "Spanish")
    
    assert result == {"translated_text": "Hola mundo"}
