# Design: Edit Sent Messages

## Overview

Добавляем возможность редактирования текста todo-сообщений через modal dialog. Backend уже готов — существующий PUT `/api/todos/{id}` поддерживает обновление поля `text`. Основная работа — реализация UI на frontend с optimistic update и error handling.

## Decisions

### Decision: UI Pattern — Modal Dialog
**Выбор:** Modal dialog для редактирования текста
**Обоснование:**
- Простой и понятный UX без сложностей focus management
- Не изменяет layout списка при редактировании
- Легко добавить валидацию и error handling
- Хорошая доступность (keyboard navigation, screen readers)
- Быстрая реализация — низкий риск

**Альтернативы:**
- Inline editing: более быстрый UX, но сложнее в реализации (focus management, layout changes)
- Content editable: самый быстрый UX, но высокий риск багов (cursor position, special keys)
- Replace with input: явный state switching, но больше визуального noise

**Future improvements:** Можно добавить inline editing как progressive enhancement после подтверждения паттерна

---

### Decision: Оптимистичное обновление UI
**Выбор:** Optimistic UI update с rollback при ошибке
**Обоснование:**
- Следует существующему паттерну в коде (toggle, delete уже используют optimistic update)
- Быстрый UX — пользователь видит изменения мгновенно
- Проверенный паттерн с обработкой ошибок

**Паттерн:**
```
1. User clicks "Save" → UI обновляется сразу с новым текстом
2. PUT request к API
3. On success → UI остается обновленным
4. On error → UI откатывается + show error toast
```

---

### Decision: Валидация на клиенте
**Выбор:** Валидация на стороне клиента с ограничением maxlength
**Обоснование:**
- Предотвращает пустые тексты и слишком длинные сообщения
- Немедленный feedback пользователю без круглого путешествия к серверу
- Backend уже имеет базовую валидацию (но не показано в коде)

**Ограничения:**
- `text` обязательно (required)
- `text` максимум 500 символов (настраивается в константе)

---

### Decision: Без изменений в схеме БД
**Выбор:** Не изменять схему БД для этого изменения
**Обоснование:**
- Существующая схема достаточна для хранения обновлённого текста
- PUT endpoint уже работает
- Опциональные метаданные (`updatedAt`, `edited`, `version`) можно добавить позже как отдельное изменение

**Future:** Можно добавить `todo-metadata-enhancements` для audit trail

---

## Architecture

### Component Structure (Frontend)

```
app/page.tsx
├── TodoList
│   ├── TodoItem
│   │   ├── TodoText (с кнопкой Edit)
│   │   ├── ToggleButton
│   │   └── DeleteButton
│   └── EditModal (новый компонент)
└── TodoForm (создание новых)
```

### Data Flow

```
User clicks "Edit"
    ↓
Open EditModal с current text
    ↓
User edits text → validation
    ↓
User clicks "Save"
    ↓
Optimistic update: setTodo(id, newText)
    ↓
PUT /api/todos/{id} { text: newText, completed }
    ↓
Success → UI stays updated
Error → rollback + error toast
```

### UI Element Identification

**Уникальные идентификаторы (UUID):**
Все создаваемые UI элементы получают уникальные UUID для отслеживания, тестирования и отладки:

**EditModal компонент:**
- Modal container: `data-id="edit-modal-550e8400-e29b-41d4-a716-446655440000"`
- Input field: `data-id="edit-input-6ba7b810-9dad-11d1-80b4-00c04fd430c8"`
- Save button: `data-id="edit-save-9bf7e3cf-4dd4-4371-b3c4-68e88d18f7d9"`
- Cancel button: `data-id="edit-cancel-a0eebc99-9c3b-4b38-9b9a-3a9b9c9d9e9f"`

**TodoItem компонент (обновление):**
- Edit button: `data-id="edit-button-b1c4d2e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e"`
- Todo text: `data-id="todo-text-c2d5e3f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f"`
- Toggle button: `data-id="toggle-button-d3e6f4a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a"`
- Delete button: `data-id="delete-button-e4f7a5b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b"`

**Значения UUID (для реализации):**

Конкретные UUID для использования в коде:

```typescript
// EditModal UUIDs
const EDIT_MODAL_CONTAINER = "550e8400-e29b-41d4-a716-446655440000";
const EDIT_MODAL_INPUT = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
const EDIT_MODAL_SAVE = "9bf7e3cf-4dd4-4371-b3c4-68e88d18f7d9";
const EDIT_MODAL_CANCEL = "a0eebc99-9c3b-4b38-9b9a-3a9b9c9d9e9f";

// TodoItem UUIDs
const TODO_EDIT_BUTTON = "b1c4d2e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e";
const TODO_TEXT = "c2d5e3f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f";
const TODO_TOGGLE_BUTTON = "d3e6f4a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a";
const TODO_DELETE_BUTTON = "e4f7a5b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b";
```

**Использование UUID в реализации:**

При реализации компоненты должны использовать эти конкретные UUID как константы:

```typescript
// EditModal компонент
<div data-id="edit-modal-550e8400-e29b-41d4-a716-446655440000">
  <input data-id="edit-input-6ba7b810-9dad-11d1-80b4-00c04fd430c8" />
  <button data-id="edit-save-9bf7e3cf-4dd4-4371-b3c4-68e88d18f7d9">Save</button>
  <button data-id="edit-cancel-a0eebc99-9c3b-4b38-9b9a-3a9b9c9d9e9f">Cancel</button>
</div>

// TodoItem компонент
<div>
  <span data-id="todo-text-c2d5e3f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f">Todo text</span>
  <button data-id="edit-button-b1c4d2e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e">Edit</button>
  <button data-id="toggle-button-d3e6f4a7-b8c9-4d0e-1f2a-3b4c5d6e7f8a">Toggle</button>
  <button data-id="delete-button-e4f7a5b8-c9d0-4e1f-2a3b-4c5d6e7f8a9b">Delete</button>
</div>
```

**Использование UUID:**
- E2E тестирование: надёжная селекция элементов без привязки к CSS классам
  ```typescript
  // Пример E2E теста
  await page.click('[data-id="edit-button-b1c4d2e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e"]');
  await page.fill('[data-id="edit-input-6ba7b810-9dad-11d1-80b4-00c04fd430c8"]', 'New text');
  await page.click('[data-id="edit-save-9bf7e3cf-4dd4-4371-b3c4-68e88d18f7d9"]');
  ```
- Debugging: идентификация элементов в React DevTools через атрибуты
- Analytics: отслеживание пользовательских действий по конкретным элементам
- Error logging: точное указание проблемного элемента в логах

**Важное примечание:**
UUID предопределены на стадии спецификации и должны использоваться как константы в коде. Не генерировать новые UUID при каждом рендере — это нарушит воспроизводимость тестов и отладки.

### API Contract

**PUT `/api/todos/{id}`** (уже существует):
```json
// Request
{
  "text": "новый текст",
  "completed": false
}

// Response (200 OK)
{
  "id": 1,
  "text": "новый текст",
  "completed": false
}

// Error (4xx/5xx)
{
  "error": "message"
}
```

---

## Data Model

### Entity: Todo (без изменений)

Существующая entity не требует изменений:

```java
@Entity
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String text;        // ← можно обновлять
    private boolean completed;  // ← не меняется
}
```

**Future enhancements** (не в этом change):
- `updatedAt` timestamp
- `edited` boolean flag
- `version` для optimistic locking

---

## Implementation Notes

### Frontend (Next.js React)

**Новый компонент EditModal:**
- Props: `isOpen`, `onClose`, `initialText`, `onSave`
- State: `text`, `error`
- Validation: `text.trim().length > 0 && text.length <= 500`
- Keyboard: Escape для закрытия, Enter для сохранения (опционально)

**Обновление TodoItem:**
- Добавить кнопку "Edit" рядом с "Delete"
- `handleEditClick`: открытие EditModal
- `handleSave`: optimistic update + PUT request

**API call:**
```typescript
async function updateTodo(id: number, text: string, completed: boolean) {
  const response = await fetch(`/api/todos/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, completed })
  });
  
  if (!response.ok) throw new Error('Failed to update');
  return response.json();
}
```

**Error handling:**
- Toast notification при ошибке: "Failed to save changes"
- Rollback к исходному тексту в state
- Логирование ошибки в console

### Backend (Spring Boot)

**Изменения не требуются** — PUT endpoint уже готов:

```java
@PutMapping("/api/todos/{id}")
public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todo) {
    // уже существует и работает
}
```

**Опциональные улучшения** (не в этом change):
- Добавить `@Size(max=500)` валидацию в entity
- Добавить `@NotBlank` для text поля
- Обновить `updatedAt` timestamp

---

## Risks

### Technical Risks

1. **Optimistic update rollback timing**
   - **Risk:** Race condition если несколько пользователей редактируют один todo
   - **Mitigation:** В текущей версии нет multi-user, риск минимален
   - **Future:** Добавить versioning для optimistic locking

2. **Modal dialog accessibility**
   - **Risk:** Плохой keyboard navigation или screen reader support
   - **Mitigation:** Использовать semantic HTML, ARIA attributes, focus trap

3. **Validation mismatch**
   - **Risk:** Клиентская валидация расходится с серверной
   - **Mitigation:** Обрабатывать 4xx ошибки от сервера и показывать validation errors

### UX Risks

1. **Modal fatigue**
   - **Risk:** Пользователям может надоесть modal dialog при частых редактированиях
   - **Mitigation:** Monitor user feedback, consider inline editing как improvement

2. **Slow API response**
   - **Risk:** Optimistic update создаёт иллюзию скорости, но медленный API может быть заметен
   - **Mitigation:** Показывать loading indicator (опционально)

---

## Testing Strategy

### Manual Testing Scenarios

1. **Happy path:**
   - Открыть EditModal → изменить текст → Save → проверить что текст обновился

2. **Validation:**
   - Попытка сохранить пустой текст → проверить ошибку
   - Попытка сохранить > 500 символов → проверить ошибку

3. **Error handling:**
   - Отключить сеть → Save → проверить rollback и error message
   - Симулировать 500 ошибку → проверить rollback

4. **Accessibility:**
   - Tab navigation через modal
   - Escape для закрытия
   - Enter для сохранения

### Automation Tests (Future)

- Unit tests для EditModal component
- Integration tests для API call
- E2E tests для полного flow

---

## Open Questions

### 1. Inline vs Modal — Future Enhancement
**Вопрос:** Стоит ли добавить inline editing как альтернативу modal?
**Статус:** Решено — начинаем с modal, inline можно добавить позже как progressive enhancement

### 2. История редактирований
**Вопрос:** Нужно ли сохранять историю изменений текста?
**Статус:** Решено — не в рамках этого change, можно добавить как `todo-edit-history` capability отдельно

### 3. Metadata enhancements
**Вопрос:** Нужно ли добавить `updatedAt`, `edited`, `version`?
**Статус:** Решено — не в рамках этого change, можно добавить как `todo-metadata-enhancements` отдельно