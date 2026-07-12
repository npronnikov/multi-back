# API Version Endpoint Specification Delta

## Purpose
Добавляет новый REST endpoint для получения информации о версии backend приложения.

## Requirements

### ADDED Requirements

#### Requirement: Version endpoint возвращает версию backend
Backend должен предоставлять endpoint для получения текущей версии приложения.

##### Scenario: Успешный запрос версии
- **WHEN** клиент выполняет `GET /api/version`
- **THEN** сервер возвращает HTTP статус 200 OK
- **AND** response body содержит JSON объект:
  ```json
  {
    "version": "string",
    "timestamp": "string (ISO-8601)"
  }
  ```
- **AND** поле `version` содержит версию backend из pom.xml (например "0.0.1-SNAPSHOT")
- **AND** поле `timestamp` содержит текущее время сервера в формате ISO-8601

##### Scenario: Обработка ошибок
- **WHEN** сервер недоступен или возникает внутренняя ошибка
- **THEN** сервер возвращает HTTP статус 5xx
- **AND** response body содержит информацию об ошибке (согласно стандартной обработке ошибок Spring Boot)
