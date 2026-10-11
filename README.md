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

## Заявки (JWT)

После входа:

- `GET /api/tickets/columns` — статусы канбана
- `GET /api/tickets` — список (оператор: только назначенные на него; админ: все)
- `POST /api/tickets`, `GET/PATCH /api/tickets/{id}`, `POST /api/tickets/{id}/assign` (назначение — только admin)
- `GET /api/tickets/filter` — период, тег, серийник; у admin ещё `manager` (нужен хотя бы один параметр)
- `GET /api/users` — клиенты MAX для ручного создания заявки
- `GET /api/staff` — краткий список сотрудников (только admin, для фильтра по исполнителю)
- `GET /api/tickets/issue-types?lang=ru|zh` — справочник неисправностей (ru/zh)
- `PATCH /api/tickets/{id}` — все поля карточки и Excel, `external_export_url` — только ссылка
- `POST /api/tickets/{id}/read` — отметить диалог прочитанным
- `GET /api/messages/tickets` — id заявок с перепиской
- `GET /api/messages/ticket/{id}/preview`, `GET /api/messages/ticket/{id}/history?before_id=`, `POST /api/messages/send` (текст и/или медиа)
- `POST /api/media/upload` (multipart), `GET /api/media/{storageKey}` — вложения чата

Миграция `V2` добавляет двух демо-клиентов MAX и заявки (видны администратору; оператору — после назначения).

## Тесты

```bash
.\gradlew.bat :server:test
```
