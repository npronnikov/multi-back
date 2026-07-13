# Version API Specification Delta

## Purpose
Добавляет новый REST API endpoint для получения информации о версии приложения. Это delta-spec определяет поведение endpoint `/api/version`.

## Requirements

### ADDED Requirements

#### Requirement: Get Application Version
Система должна предоставлять REST endpoint для получения текущей версии приложения.

##### Scenario: Successful version request
- **WHEN** клиент отправляет `GET /api/version`
- **THEN** система возвращает HTTP 200 OK
- **AND** ответ содержит JSON объект с полем `version`
- **AND** значение `version` соответствует версии из `pom.xml`

##### Scenario: Version format
- **WHEN** клиент получает ответ от `/api/version`
- **THEN** поле `version` содержит строку в формате семантического версионирования
- **AND** формат соответствует `{major}.{minor}.{patch}-{qualifier}` (например, `0.0.1-SNAPSHOT`)

##### Scenario: Concurrent requests
- **WHEN** множество клиентов одновременно запрашивают `/api/version`
- **THEN** все запросы успешно обрабатываются
- **AND** все клиенты получают одинаковые значения версии
