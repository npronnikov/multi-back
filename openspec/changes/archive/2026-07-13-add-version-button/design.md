# Design: add-version-button

## Overview
Реализация REST endpoint для получения версии, UI кнопки для отображения и сохранения версии в YAML-конфигурацию. Версия берётся из Maven properties, YAML создаётся/обновляется при каждом запросе, UI компонент интегрируется в существующий Next.js интерфейс.

## Decisions

### Decision: Maven Resource Filtering для версии
**Выбор:** Использовать Maven resource filtering для чтения версии из `pom.xml` через `@Value("${project.version}")`

**Обоснование:**
- Стандартный механизм Spring Boot для чтения версии из Maven
- Не требует дополнительной конфигурации
- Автоматически обновляется при изменении версии в pom.xml

**Альтернативы:**
- Чтение `pom.xml` напрямую (сложно и ненадёжно)
- Хардкод версии (не обновляется автоматически)

### Decision: Jackson YAML для работы с YAML
**Выбор:** Использовать Jackson Dataformat YAML для чтения/записи YAML-файлов

**Обоснование:**
- Spring Boot уже использует Jackson для JSON
- Единая экосистема для JSON и YAML
- Стабильная и поддерживаемая библиотека

**Альтернативы:**
- SnakeYAML (требует отдельной зависимости)
- Ручной парсинг (ненадёжно и сложно)

### Decision: YAML-файл в src/main/resources
**Выбор:** Создавать `version.yml` в `src/main/resources/`

**Обоснование:**
- Стандартное расположение для ресурсов в Spring Boot
- Файл будет включён в CLASSPATH
- Может быть прочитан другими частями приложения

**Альтернативы:**
- Внешняя директория конфигурации (сложнее для разработки)
- База данных (over-engineering для текущей задачи)

### Decision: Без синхронизации для записи YAML
**Выбор:** Не использовать механизмы синхронизации при записи YAML, полагаться на атомарную запись

**Обоснование:**
- Простота реализации
- Если файл повреждён, он будет перезаписан при следующем запросе
- YAML-файл не критичен для работы приложения

**Альтернативы:**
- Блокировка файла (сложно для REST-архитектуры)
- База данных с транзакциями (over-engineering)

### Decision: UI кнопка рядом с существующими
**Выбор:** Добавить кнопку "Version" рядом с кнопкой "Add" в main page

**Обоснование:**
- Следует существующим паттернам UI
- Логично размещать рядом с другими action buttons
- Не требует перепроектирования layout

**Альтернативы:**
- Отдельная страница (избыточно для простого действия)
- Modal окно (сложно для MVP)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (Next.js)                   │
│                                                              │
│  ┌─────────────┐         ┌──────────────────┐              │
│  │ Version Btn │────────>│ fetch /api/version│              │
│  └─────────────┘         └──────────────────┘              │
│                                      │                      │
│                                      v (display result)     │
│                               ┌─────────────┐              │
│                               │  Show alert  │              │
│                               └─────────────┘              │
└─────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP GET
                                      v
┌─────────────────────────────────────────────────────────────┐
│                         Backend (Spring Boot)                │
│                                                              │
│  ┌─────────────────┐    ┌───────────────────────────────┐ │
│  │ VersionController│──>│ VersionService                 │ │
│  │ /api/version     │    │                               │ │
│  └─────────────────┘    │ 1. Get version from Maven     │ │
│                          │ 2. Write to version.yml       │ │
│                          │ 3. Return JSON                │ │
│                          └───────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ version.yml (src/main/resources/)                    │  │
│  │ version: 0.0.1-SNAPSHOT                               │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ pom.xml                                               │  │
│  │ <version>0.0.1-SNAPSHOT</version>                     │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Data Model

### API Response
```json
{
  "version": "0.0.1-SNAPSHOT"
}
```

### YAML File
```yaml
version: 0.0.1-SNAPSHOT
```

## Implementation Notes

### Backend (multi-back)

**VersionController:**
- REST controller с аннотацией `@RestController`
- Метод `getVersion()` с маппингом `GET /api/version`
- Возвращает `VersionResponse` DTO

**VersionService:**
- Читает версию через `@Value("${project.version}")`
- Создаёт/обновляет `version.yml` с помощью Jackson YAML
- Обрабатывает ошибки файловой системы без выброса исключений

**Maven Configuration:**
- Добавить resource filtering в `pom.xml` (если ещё не настроен)
- `${project.version}` будет доступен как property

**Dependency:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

### Frontend (multi-front)

**Component:**
- Добавить кнопку `<button>` рядом с существующей кнопкой "Add"
- Использовать `useEffect` или event handler для `onClick`
- Fetch API для вызова `/api/version`

**Error Handling:**
- Try-catch вокруг fetch
- Отображение ошибки пользователю (alert или inline)

**Loading State:**
- Состояние `isLoading` для индикации загрузки
- Отключение кнопки во время запроса

## Risks

### Технические риски
1. **Maven resource filtering не настроен**
   - **Митигация:** Проверить и настроить filtering в `pom.xml`
   - **Резерв:** Хардкод версии как fallback

2. **YAML-файл не создаётся из-за прав доступа**
   - **Митигация:** Логирование ошибки, API продолжает работать
   - **Резерв:** Игнорировать ошибки записи YAML

3. **Concurrent writes повреждают YAML**
   - **Митигация:** Атомарная запись (write to temp file + rename)
   - **Резерв:** Файл будет перезаписан при следующем запросе

### Зависимости
- Jackson Dataformat YAML (новая зависимость)
- Maven Resources Plugin (обычно уже есть)

### Совместимость
- Backend: Spring Boot 4.1.0 (поддерживается)
- Frontend: Next.js 16.2.10, React 19 (поддерживается)
