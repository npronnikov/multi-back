# Architecture Overview: main (multi-front)

**Repo alias:** main

**Назначение:**
Фронтенд-репозиторий Next.js-приложения с пользовательским интерфейсом Todo List. Предоставляет современный React-клиент с REST-интеграцией к бэкенду.

## Архитектурные наблюдения

1. **Next.js App Router с React Server Components** — проект использует Next.js 16.2.10 с App Router (`app/` директория),.page.tsx - клиентский компонент ("use client") с локальным state-управлением.

2. **TypeScript + Tailwind CSS v4** — строгая типизация интерфейсов Todo, API-контрактов; стилизация через Tailwind CSS v4 с PostCSS и dark mode поддержкой.

3. **REST API интеграция с оптимистичными обновлениями** — компонент напрямую общается с бэкендом (`/api/todos`), реализует optimistic UI updates с rollback при ошибках.

4. **Environment-based конфигурация** — API endpoint через `NEXT_PUBLIC_API_URL` с fallback на `localhost:8081`, что позволяет гибкую настройку для разных окружений.

5. **Клиентская обработка ошибок** — пользователю показываются понятные сообщения при сбоях сети или недоступности бэкенда, с сохранением предыдущего состояния.
