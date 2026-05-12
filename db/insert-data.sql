-- =========================
-- USERS
-- =========================
INSERT INTO public.users (auth_provider,avatar_url,created_at,daily_ai_usage,email,full_name,last_ai_usage_date,password_hash,"role",status,updated_at) VALUES
	 ('LOCAL',NULL,'2026-05-08 20:37:46.696644+07',0,'hlklonga5@gmail.com','Hoang Le Kim Long','2026-05-08','$2a$10$PaVVwQMkVCUo9V5mOLZDEO7HYkVDCX3pFhK2DQkwV8u6wnqzelIsq','MEMBER','ACTIVE','2026-05-08 20:37:46.696644+07'),
	 ('LOCAL',NULL,'2026-05-10 01:04:39.436125+07',0,'dung@gmail.com','Nguyen Tien Dung','2026-05-09','$2a$10$Vv7nBM2hqxuVi9NXvpKO5O99q6gbDKTMs.x3uMz98JaDxyATCpkJ2','MEMBER','ACTIVE','2026-05-10 01:04:39.436125+07');

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO categories (name) VALUES
('Action'),
('Adventure'),
('Fantasy'),
('Romance'),
('Drama'),
('Comedy'),
('Horror'),
('Mystery'),
('Sci-Fi'),
('Slice of Life');

-- =========================
-- COMICS
-- =========================
INSERT INTO public.comics (author,average_rating,cover_image_url,created_at,description,format,original_language,status,title,total_ratings,updated_at) VALUES
	 ('Yuki Tanaka',4.2,'https://i.pinimg.com/1200x/32/ec/79/32ec798f31e28fd7d6c3c2775daf2114.jpg','2026-05-10 23:53:38.360188+07','A romantic story between two university students.','Manga','Japanese','ONGOING','Love in Tokyo',89,'2026-05-10 23:53:38.360188+07'),
	 ('Kim Jiho',4.1,'https://i.pinimg.com/1200x/aa/4b/cd/aa4bcd56244410d7a5e92bf80d5d5000.jpg','2026-05-10 23:53:38.360188+07','Daily life comedy with ridiculous situations.','Webtoon','Korean','ONGOING','Laugh Factory',60,'2026-05-10 23:53:38.360188+07'),
	 ('John Carter',4.8,'https://i.pinimg.com/736x/f0/e5/e0/f0e5e0d4484e2259ce84827b1bf23ea5.jpg','2026-05-10 23:53:38.360188+07','Elite soldiers protect the galaxy from alien invasions.','Comic','English','COMPLETED','Galaxy Warriors',250,'2026-05-10 23:53:38.360188+07'),
	 ('Akira Sato',4.5,'https://i.pinimg.com/736x/b2/03/fb/b203fb9eb0e37bf4dbb9167f7abe7204.jpg','2026-05-10 23:53:38.360188+07','A young warrior fights against dark creatures threatening humanity.','Manga','Japanese','ONGOING','Shadow Hunter',120,'2026-05-10 23:53:38.360188+07'),
	 ('Hana Lee',4.6,'https://i.pinimg.com/736x/f7/56/11/f75611b7bbd4e7494b663bc2615895eb.jpg','2026-05-10 23:53:38.360188+07','Students learn forbidden magic in a secret academy.','Manhwa','Korean','ONGOING','Mystic Academy',170,'2026-05-10 23:53:38.360188+07');


-- =========================
-- COMIC CATEGORIES
-- =========================
INSERT INTO comic_categories (comic_id, category_id) VALUES
(1, 1),
(1, 3),

(2, 4),
(2, 5),

(3, 1),
(3, 9),

(4, 3),
(4, 2),

(5, 6),
(5, 10);

-- =========================
-- CHAPTERS
-- =========================
INSERT INTO chapters (
    comic_id,
    chapter_number,
    title,
    created_at
)
VALUES
(1, 1, 'The Beginning', NOW()),
(1, 2, 'Dark Forest', NOW()),
(1, 3, 'First Battle', NOW()),

(2, 1, 'First Meeting', NOW()),
(2, 2, 'Confession', NOW()),

(3, 1, 'Galaxy Under Attack', NOW()),
(3, 2, 'The Last Hope', NOW()),

(4, 1, 'Welcome to the Academy', NOW()),
(4, 2, 'Forbidden Spell', NOW()),

(5, 1, 'Office Chaos', NOW());

-- =========================
-- PAGES
-- =========================
INSERT INTO pages (
    chapter_id,
    page_number,
    image_url,
    cleaned_image_url,
    created_at
)
VALUES

-- Chapter 1
(1, 1,
'https://example.com/pages/shadow-hunter/ch1/1.jpg',
'https://example.com/pages-clean/shadow-hunter/ch1/1.jpg',
NOW()),

(1, 2,
'https://example.com/pages/shadow-hunter/ch1/2.jpg',
'https://example.com/pages-clean/shadow-hunter/ch1/2.jpg',
NOW()),

(1, 3,
'https://example.com/pages/shadow-hunter/ch1/3.jpg',
'https://example.com/pages-clean/shadow-hunter/ch1/3.jpg',
NOW()),

-- Chapter 2
(2, 1,
'https://example.com/pages/shadow-hunter/ch2/1.jpg',
'https://example.com/pages-clean/shadow-hunter/ch2/1.jpg',
NOW()),

(2, 2,
'https://example.com/pages/shadow-hunter/ch2/2.jpg',
'https://example.com/pages-clean/shadow-hunter/ch2/2.jpg',
NOW()),

-- Love in Tokyo
(4, 1,
'https://example.com/pages/love-tokyo/ch1/1.jpg',
'https://example.com/pages-clean/love-tokyo/ch1/1.jpg',
NOW()),

(4, 2,
'https://example.com/pages/love-tokyo/ch1/2.jpg',
'https://example.com/pages-clean/love-tokyo/ch1/2.jpg',
NOW()),

-- Galaxy Warriors
(6, 1,
'https://example.com/pages/galaxy/ch1/1.jpg',
'https://example.com/pages-clean/galaxy/ch1/1.jpg',
NOW()),

(6, 2,
'https://example.com/pages/galaxy/ch1/2.jpg',
'https://example.com/pages-clean/galaxy/ch1/2.jpg',
NOW()),

-- Mystic Academy
(8, 1,
'https://example.com/pages/mystic/ch1/1.jpg',
'https://example.com/pages-clean/mystic/ch1/1.jpg',
NOW()),

(8, 2,
'https://example.com/pages/mystic/ch1/2.jpg',
'https://example.com/pages-clean/mystic/ch1/2.jpg',
NOW());
-- =========================
-- RATINGS
-- user_id = 1,2
-- =========================
INSERT INTO ratings (
    user_id,
    comic_id,
    score,
    created_at
)
VALUES
(1, 1, 5, NOW()),
(2, 1, 4, NOW()),

(1, 2, 4, NOW()),
(2, 2, 5, NOW()),

(1, 3, 5, NOW()),
(2, 3, 5, NOW()),

(1, 4, 4, NOW()),
(2, 4, 5, NOW()),

(1, 5, 4, NOW());

-- =========================
-- COMMENTS
-- =========================
INSERT INTO comments (
    user_id,
    chapter_id,
    parent_id,
    content,
    created_at,
    updated_at
)
VALUES
(1, 1, NULL, 'This chapter is amazing!', NOW(), NOW()),
(2, 1, 1, 'Yeah, I love the art style.', NOW(), NOW()),

(1, 4, NULL, 'Very cute romance story.', NOW(), NOW()),

(2, 6, NULL, 'The sci-fi atmosphere is awesome.', NOW(), NOW()),

(1, 8, NULL, 'Magic academy stories never disappoint.', NOW(), NOW());

-- =========================
-- READING HISTORIES
-- =========================
INSERT INTO reading_histories (
    user_id,
    comic_id,
    chapter_id,
    last_page_read,
    updated_at
)
VALUES
(1, 1, 2, 2, NOW()),
(2, 1, 3, 1, NOW()),

(1, 2, 1, 2, NOW()),

(2, 3, 1, 1, NOW()),

(1, 4, 2, 2, NOW());

-- =========================
-- USER LIBRARIES
-- =========================
INSERT INTO user_libraries (
    user_id,
    comic_id,
    list_type,
    created_at
)
VALUES
(1, 1, 'FAVORITE', NOW()),
(1, 2, 'READING', NOW()),
(2, 3, 'READ_LATER', NOW()),
(1, 4, 'FAVORITE', NOW()),
(2, 5, 'READING', NOW());
