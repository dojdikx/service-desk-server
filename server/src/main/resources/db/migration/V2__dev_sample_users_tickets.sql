-- Локальные демо-данные для экрана тикетов (можно удалить в проде).
INSERT INTO users (max_id, name, phone, city, status, serial_number, printer_model)
VALUES
    (900001, 'Иван Петров', '+79001112233', 'Москва', 'registered', '31B1234567890123', 'h2c'),
    (900002, 'Мария Сидорова', '+79004445566', 'Казань', 'registered', '0301234567890123', 'a1_mini')
ON CONFLICT (max_id) DO NOTHING;

INSERT INTO tickets (user_id, max_user_id, status, reason, full_name, assigned_to)
SELECT u.id, u.max_id, 'Первичный контакт', 'Не печатает первый слой', u.name, NULL
FROM users u
WHERE u.max_id = 900001
  AND NOT EXISTS (SELECT 1 FROM tickets t WHERE t.user_id = u.id AND t.reason = 'Не печатает первый слой');

INSERT INTO tickets (user_id, max_user_id, status, reason, full_name, assigned_to)
SELECT u.id, u.max_id, 'Ожидают запчасти', 'Заказал запчасть по SKU', u.name, NULL
FROM users u
WHERE u.max_id = 900002
  AND NOT EXISTS (SELECT 1 FROM tickets t WHERE t.user_id = u.id AND t.reason = 'Заказал запчасть по SKU');
