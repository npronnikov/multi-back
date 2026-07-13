# Архитектура Backend API

## Repo alias
`back`

## Технологический стек
- **Framework**: Spring Boot 4.1.0
- **Language**: Java 21
- **Data Access**: Spring Data JPA + Spring Data REST
- **Database**: H2 (file-based)
- **Build**: Maven
- **Utilities**: Lombok

## Архитектурные наблюдения

### 1. Классическая многослойная архитектура
Приложение следует стандартной Spring Boot архитектуре с разделением на слои:
- **Entity Layer**: `Todo.java` - JPA сущность с аннотациями Lombok (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor)
- **Repository Layer**: `TodoRepository.java` - интерфейс JpaRepository с явным отключением Spring Data REST экспорта
- **Controller Layer**: `TodoController.java` - REST контроллер с HTTP методами (GET, POST, PUT, DELETE)
- **Configuration Layer**: `WebConfig.java` - CORS конфигурация

### 2. Dependency Injection через конструктор
Все зависимости инжектируются через конструкторы (field injection отсутствует). `TodoController` получает `TodoRepository` через конструктор - это современный подход в Spring, facilitating тестирование и обнаружение проблем при старте приложения.

### 3. H2 Database с file-based персистентностью
Конфигурация использует файловую H2 database (`jdbc:h2:file:./data/tododb`) вместо in-memory. Это означает, что данные сохраняются между перезапусками приложения. H2 Console доступна на `/h2-console` для разработки и дебага.

### 4. Spring Data REST отключён для Repository
Хотя включена зависимость `spring-boot-starter-data-rest`, репозиторий `TodoRepository` использует аннотацию `@RepositoryRestResource(exported = false)`, что отключает автоматическую генерацию REST endpoints. Вместо этого используется кастомный `@RestController` для полного контроля над API.

### 5. CORS конфигурация для фронтенда
`WebConfig` настраивает CORS для всех `/api/**` endpoints, разрешая запросы с `http://localhost:8082` (где запущен Next.js frontend). Это позволяет фронтенду напрямую общаться с backend без proxy сервера.

## Ключевые компоненты

### API Endpoints (TodoController)
- `GET /api/todos` - получить все задачи
- `POST /api/todos` - создать новую задачу (возвращает 201 CREATED)
- `PUT /api/todos/{id}` - обновить задачу по ID
- `DELETE /api/todos/{id}` - удалить задачу (возвращает 204 NO CONTENT)

### Database Schema (Todo Entity)
- `id` - Long (auto-increment, primary key)
- `text` - String (содержимое задачи)
- `completed` - boolean (статус выполнения)

## Конфигурация
- **Server Port**: 8081
- **Database**: H2 file at `./data/tododb`
- **CORS Origin**: `http://localhost:8082`
- **H2 Console**: `/h2-console`

## Ключевые файлы
- `src/main/java/com/example/demo/DemoApplication.java` - точка входа
- `src/main/java/com/example/demo/Todo.java` - JPA сущность
- `src/main/java/com/example/demo/TodoRepository.java` - repository
- `src/main/java/com/example/demo/TodoController.java` - REST контроллер
- `src/main/java/com/example/demo/WebConfig.java` - CORS конфигурация
- `src/main/resources/application.properties` - конфигурация приложения
