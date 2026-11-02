-- EventHub - Baslangic kullanicilarini ekler.
-- Tum sifreler: Admin123!  (bcrypt ile hashlenmis, asagidaki hash'e karsilik gelir)
-- email alaninda UNIQUE constraint oldugu icin ayni kullanici tekrar calistirmak hata vermez.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    id, email, first_name, last_name, password, role,
    delete_flag, institution, institution_email_verified,
    created_at, updated_at
) VALUES
    (
        gen_random_uuid(),
        'admin@eventhub.local',
        'Admin',
        'User',
        '$2b$12$vlLtlDTHNf/fkkq0AzcE9emiDF/Khiph43sdfivZnFV2kHTLrYWOm',
        'ADMIN',
        false,
        NULL,
        false,
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'organizer@eventhub.local',
        'Organizer',
        'Demo',
        '$2b$12$vlLtlDTHNf/fkkq0AzcE9emiDF/Khiph43sdfivZnFV2kHTLrYWOm',
        'USER',
        false,
        NULL,
        false,
        now(),
        now()
    ),
    (
        gen_random_uuid(),
        'user@eventhub.local',
        'Test',
        'User',
        '$2b$12$vlLtlDTHNf/fkkq0AzcE9emiDF/Khiph43sdfivZnFV2kHTLrYWOm',
        'USER',
        false,
        NULL,
        false,
        now(),
        now()
    )
ON CONFLICT (email) DO NOTHING;

SELECT email, role, created_at FROM users ORDER BY created_at;
