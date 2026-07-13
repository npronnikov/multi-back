# Tasks: add-version-button

## Phase 1: Backend Dependencies

- [x] T001 — Добавить Jackson YAML зависимость в `pom.xml` (`multi-back/pom.xml`)
- [x] T002 — Настроить Maven resource filtering для `project.version` (`multi-back/pom.xml`)

## Phase 2: Backend Implementation

- [x] T003 — Создать `VersionResponse` DTO (`multi-back/src/main/java/com/example/demo/dto/VersionResponse.java`)
- [x] T004 — Создать `VersionService` с логикой получения версии и записи в YAML (`multi-back/src/main/java/com/example/demo/service/VersionService.java`)
- [x] T005 — Создать `VersionController` с endpoint `GET /api/version` (`multi-back/src/main/java/com/example/demo/controller/VersionController.java`)

## Phase 3: Frontend Implementation

- [x] T006 — Добавить кнопку "Version" в main page (`multi-front/app/page.tsx`)
- [x] T007 — Добавить состояние для loading и error в компонент (`multi-front/app/page.tsx`)
- [x] T008 — Реализовать обработчик клика с вызовом `/api/version` (`multi-front/app/page.tsx`)

## Phase 4: Testing

- [x] T009 — Написать unit тест для `VersionService` (`multi-back/src/test/java/com/example/demo/service/VersionServiceTest.java`)
- [x] T010 — Написать unit тест для `VersionController` (`multi-back/src/test/java/com/example/demo/controller/VersionControllerTest.java`)

## Verification

- [x] T011 — Запустить бэкенд тесты: `cd multi-back && ./mvnw test`
- [x] T012 — Проверить сборку бэкенда: `cd multi-back && ./mvnw package`
- [x] T013 — Проверить компиляцию бэкенда: `cd multi-back && ./mvnw compile`
- [x] T014 — Проверить сборку фронтенда: `cd multi-front && npm run build`
- [x] T015 — Проверить линтинг фронтенда (если доступен): `cd multi-front && npm run lint`
