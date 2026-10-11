-- Демо-сообщения для вкладки «Диалоги» (локальная разработка).
INSERT INTO messages (ticket_id, message_text, message_type, timestamp)
SELECT t.id, 'Здравствуйте, принтер перестал печатать первый слой', 'user', NOW() - INTERVAL '2 hours'
FROM tickets t
         JOIN users u ON t.user_id = u.id
WHERE u.max_id = 900001
  AND t.reason = 'Не печатает первый слой'
  AND NOT EXISTS (SELECT 1 FROM messages m WHERE m.ticket_id = t.id);

INSERT INTO messages (ticket_id, message_text, message_type, timestamp, delivery_status)
SELECT t.id, 'Добрый день! Пришлите, пожалуйста, фото стола и сопла.', 'backend', NOW() - INTERVAL '90 minutes', 'sent'
FROM tickets t
         JOIN users u ON t.user_id = u.id
WHERE u.max_id = 900001
  AND t.reason = 'Не печатает первый слой'
  AND (SELECT COUNT(*) FROM messages m WHERE m.ticket_id = t.id) = 1;

INSERT INTO messages (ticket_id, message_text, message_type, timestamp)
SELECT t.id, 'Когда приедет запчасть по моему заказу?', 'user', NOW() - INTERVAL '30 minutes'
FROM tickets t
         JOIN users u ON t.user_id = u.id
WHERE u.max_id = 900002
  AND t.reason = 'Заказал запчасть по SKU'
  AND NOT EXISTS (SELECT 1 FROM messages m WHERE m.ticket_id = t.id);
