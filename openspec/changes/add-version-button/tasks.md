# Tasks: Add Version Button

## Phase 1: Backend Implementation

- [ ] T001 — Создать DTO record `VersionDTO` в `backend/src/main/java/com/example/demo/dto/VersionDTO.java` с полями `version` (String) и `timestamp` (String)
- [ ] T002 — Создать REST controller `VersionController` в `backend/src/main/java/com/example/demo/controller/VersionController.java`
- [ ] T003 — Добавить метод `getVersion()` с аннотацией `@GetMapping("/api/version")` в `VersionController.java`
- [ ] T004 — Инжектировать версию через `@Value("${project.version}")` в `VersionController.java`
- [ ] T005 — Добавить логику генерации timestamp через `Instant.now().toString()` в методе `getVersion()`
- [ ] T006 — Вернуть `VersionDTO` с версией и timestamp через `ResponseEntity.ok()` в методе `getVersion()`

## Phase 2: Frontend Implementation

- [ ] T007 — Создать компонент `VersionButton` в `frontend/app/components/VersionButton.tsx` с кнопкой "Version"
- [ ] T008 — Добавить state для версии: `version`, `timestamp`, `isLoading`, `error` в `VersionButton.tsx`
- [ ] T009 — Добавить функцию `fetchVersion()` с запросом к `http://localhost:8081/api/version` в `VersionButton.tsx`
- [ ] T010 — Добавить обработку onClick: показать кэшированную версию или вызвать `fetchVersion()` в `VersionButton.tsx`
- [ ] T011 — Добавить отображение версии через `window.alert()` с форматом "Backend version: {version}\nTimestamp: {timestamp}" в `VersionButton.tsx`
- [ ] T012 — Добавить обработку ошибок и отображение "Failed to load version information." при ошибке в `VersionButton.tsx`
- [ ] T013 — Добавить disabled состояние кнопки во время `isLoading` в `VersionButton.tsx`
- [ ] T014 — Интегрировать `VersionButton` в header приложения в `frontend/app/page.tsx`

## Phase 3: Verification

- [ ] T015 — Запустить тесты backend: `cd backend && ./gradlew test`
- [ ] T016 — Проверить сборку backend: `cd backend && ./gradlew build`
- [ ] T017 — Проверить компиляцию backend: `cd backend && ./gradlew compileJava`
- [ ] T018 — Проверить сборку frontend: `cd frontend && npm run build`
- [ ] T019 — Запустить линтер frontend: `cd frontend && npm run lint`
- [ ] T020 — Проверить типы TypeScript: `cd frontend && npx tsc --noEmit`
