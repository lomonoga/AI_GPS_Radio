CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS points_of_interest (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    location GEOMETRY(Point, 4326) NOT NULL
);

CREATE TABLE IF NOT EXISTS poi_files (
    id SERIAL PRIMARY KEY,
    poi_id INTEGER NOT NULL REFERENCES points_of_interest(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    is_short BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для точек интереса
CREATE INDEX IF NOT EXISTS idx_poi_location ON points_of_interest USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_poi_created_at ON points_of_interest(created_at);

-- Индексы для аудио файлов
CREATE INDEX IF NOT EXISTS idx_poi_audio_poi_id ON poi_audio_files(poi_id);
CREATE INDEX IF NOT EXISTS idx_poi_audio_is_short ON poi_audio_files(is_short);
CREATE INDEX IF NOT EXISTS idx_poi_audio_s3_key ON poi_audio_files(s3_key);