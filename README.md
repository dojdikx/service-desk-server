# service-desk-server

Ktor-сервер для мобильной CRM Service desk (сотрудники поддержки, заявки, MAX).

## Требования

- JDK 17+
- Docker (для PostgreSQL)

## База данных

```bash
docker compose up -d
```

Параметры по умолчанию:

| Параметр | Значение |
| --- | --- |
| Хост | `localhost` |
| Порт | `5433` |
| База | `servicedesk` |
| Пользователь | `servicedesk_user` |
| Пароль | `servicedesk_password` |

Если порт `5433` занят (например, другим проектом), остановите контейнер или измените проброс в `docker-compose.yml`.

## Запуск API

```bash
.\gradlew.bat :server:run
```

При старте Flyway накатывает миграции из `server/src/main/resources/db/migration` (отключить: `FLYWAY_MIGRATE=false`).

Таблицы: `users`, `support_staff`, `tickets`, `messages`, `ticket_read_state`, `analytics`. Канал MAX (`max_id`), без Telegram и без таблицы принтеров.

Проверка:

```bash
curl http://127.0.0.1:8080/
```

Ожидается JSON: `{"status":"ok","service":"service-desk-server"}`.

Проверка схемы в Postgres:

```bash
docker exec -it service-desk-db psql -U servicedesk_user -d servicedesk -c "\dt"
```

## Тесты

```bash
.\gradlew.bat :server:test
```
