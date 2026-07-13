# Backend Architecture Overview

**Repo alias:** back

**Назначение:** Spring Boot REST API сервис для управления todo-задачами с JPA персистентностью.

## Наблюдения по архитектуре:

1. **Стандартный Spring Boot stack:** Приложение использует Spring Boot 4.1.0 с Java 21, Spring Data JPA для работы с БД, Spring MVC для REST контроллеров и H2 in-memory database для хранения данных.

2. **Классическая трехслойная архитектура:**
   - **Entity layer:** `Todo.java` — JPA сущность с Lombok annotations
   - **Repository layer:** `TodoRepository` — интерфейс JpaRepository с отключенным REST export
   - **Controller layer:** `TodoController` — REST endpoints для CRUD операций

3. **REST API дизайн:** Контроллер реализует полный CRUD интерфейс: GET /api/todos (список), POST /api/todos (создание), PUT /api/todos/{id} (обновление), DELETE /api/todos/{id} (удаление) с proper HTTP статусами и обработкой ошибок.

4. **Dependency injection и clean code:** Контроллер использует конструкторную инъекцию зависимостей (final field + constructor), следуя современным Spring best practices, без использования @Autowired.

5. **Minimal configuration:** `WebConfig.java` вероятно содержит CORS или MVC конфигурацию для фронтенда, приложение использует стандартные Spring Boot defaults без кастомной конфигурации безопасности или middleware.
