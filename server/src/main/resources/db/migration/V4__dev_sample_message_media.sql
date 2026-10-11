UPDATE messages
SET media_type = 'photo',
    media_url = '/api/media/demo_client_printer.png',
    media_file_name = 'printer.png',
    media_caption = 'Первый слой срывается'
WHERE message_type = 'user'
  AND message_text LIKE 'Здравствуйте%'
  AND media_url IS NULL;
