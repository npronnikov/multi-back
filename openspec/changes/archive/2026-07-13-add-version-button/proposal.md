# Proposal: add-version-button

## Why
Пользователю необходимо иметь возможность просматривать текущую версию приложения через пользовательский интерфейс. В настоящее время информация о версии доступна только в `pom.xml` и не представлена в UI. Кроме того, требуется хранить информацию о версии в YAML-конфигурации для использования другими системами.

## What Changes
Будет добавлен REST API endpoint для получения информации о версии приложения, кнопка в UI для отображения версии, и механизм сохранения версии в YAML-конфигурацию.

**Изменения:**
- Backend: новый `VersionController` с endpoint `GET /api/version`
- Frontend: кнопка "Version" в UI с отображением версии
- Backend: запись версии в `version.yml` при вызове endpoint

## Capabilities
- `version-api`: REST API для получения версии приложения
- `version-ui`: UI-компонент для отображения версии
- `version-yaml`: сохранение версии в YAML-конфигурацию

## Impact
**Затронутые компоненты:**
- Backend (multi-back): новый controller, YAML-конфигурация
- Frontend (multi-front): новый UI-компонент

**Риски:**
- Maven resource filtering может быть не настроен для версии
- Потребуется добавить зависимость Jackson YAML для работы с YAML

**Зависимости:**
- Spring Boot (уже есть)
- Jackson Dataformat YAML (требуется добавить)

## Success Criteria
1. Endpoint `GET /api/version` возвращает JSON с версией
2. Кнопка в UI вызывает endpoint и отображает версию
3. Версия сохраняется в `version.yml` при вызове endpoint
4. Проект собирается без ошибок: `./gradlew build` для backend, `npm run build` для frontend
5. Бэкенд-тесты проходят: `./gradlew test`
