CREATE INDEX IF NOT EXISTS idx_poi_location ON points_of_interest USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_poi_created_at ON points_of_interest(created_at);

CREATE INDEX IF NOT EXISTS idx_poi_files_poi_id ON poi_files(poi_id);
CREATE INDEX IF NOT EXISTS idx_poi_files_is_short ON poi_files(is_short);
CREATE INDEX IF NOT EXISTS idx_poi_files_s3_key ON poi_files(s3_key);