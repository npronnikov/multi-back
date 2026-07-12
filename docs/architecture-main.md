# Архитектура Main Repository

## Repo alias
main

## Назначение репозитория
Backend-сервер на Spring Boot, предоставляющий REST API для управления задачами (Todo). Работает на порту 8081 и использует встроенную H2 базу данных для персистентности.

## Архитектурные наблюдения

1. **Spring Boot 4.1.0 с Java 21**
   - Использует актуальную версию Spring Boot и современную Java 21
   - Конфигурация через `application.properties` с настройкой CORS для фронтенда

2. **Классическая трёхслойная архитектура**
   - `TodoController` (@RestController) — REST endpoints на `/api/todos`
   - `TodoRepository` (JpaRepository) — доступ к данным через Spring Data JPA
   - `Todo` (@Entity) — доменная модель с Lombok для сокращения кода

3. **H2 Database в файловом режиме**
   - База данных хранится в файловой системе (`./data/tododb`)
   - Включена H2 Console для административного доступа по `/h2-console`
   - Hibernate автоматически создаёт/обновляет схему (`ddl-auto=update`)

4. **CORS-конфигурация для фронтенда**
   - Разрешены запросы с `http://localhost:8082` (где запущен Next.js фронтенд)
   - Обеспечивает корректную работу между backend и frontend

5. **Spring Data REST отключён для репозитория**
   - `@RepositoryRestResource(exported = false)` предотвращает авто-генерацию REST endpoints
   - Все CRUD-операции явно определены в TodoController
