CREATE TABLE IF NOT EXISTS type_of_interest (
    id VARCHAR(32) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS points_of_interest_type (
    point_of_interest_id INTEGER NOT NULL,
    type_of_interest_id VARCHAR(32) NOT NULL,
    
    PRIMARY KEY (point_of_interest_id, type_of_interest_id),
    
    CONSTRAINT fk_point_of_interest
        FOREIGN KEY (point_of_interest_id)
        REFERENCES points_of_interest(id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_type_of_interest
        FOREIGN KEY (type_of_interest_id)
        REFERENCES type_of_interest(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_points_of_interest_type_point_id ON points_of_interest_type(point_of_interest_id);
CREATE INDEX IF NOT EXISTS idx_points_of_interest_type_type_id ON points_of_interest_type(type_of_interest_id);

INSERT INTO type_of_interest (id)
VALUES
    ('nature'),
    ('architecture'),
    ('food'),
    ('history')