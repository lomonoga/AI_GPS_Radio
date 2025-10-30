package postgres

import (
	"aigpsservice/internal/domain"
	"database/sql"
	"fmt"
)

type poiRepository struct {
	db *sql.DB
}

func NewPOIRepository(db *sql.DB) domain.POIRepository {
	return &poiRepository{db: db}
}

func (r *poiRepository) FindNearestPOI(latitude, longitude float64) (*domain.PointOfInterest, error) {
	query := `
        WITH nearest_poi AS (
            SELECT 
				p.id,
                p.name,
                p.description, 
                p.created_at,
                ST_X(p.location) as longitude,
                ST_Y(p.location) as latitude,
                ST_Distance(
                    p.location::geography, 
                    ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography
                ) as distance_meters
            FROM points_of_interest p
            ORDER BY p.location <-> ST_SetSRID(ST_MakePoint($1, $2), 4326)
            LIMIT 1
        )
        SELECT 
            np.id, np.name, np.description, np.latitude, np.longitude, np.created_at,
            f.id, f.s3_key, f.file_name, f.file_size, f.mime_type, f.serial_number, f.is_short, f.created_at
        FROM nearest_poi np
        LEFT JOIN poi_files f ON np.id = f.poi_id
        ORDER BY f.is_short DESC, f.serial_number ASC
    `

	rows, err := r.db.Query(query, longitude, latitude)
	if err != nil {
		return nil, fmt.Errorf("database query error: %w", err)
	}
	defer rows.Close()

	var poi *domain.PointOfInterest

	for rows.Next() {
		var fileID sql.NullInt64
		var file domain.File

		if poi == nil {
			poi = &domain.PointOfInterest{
				FullAudioFiles: []domain.File{},
			}

			err := rows.Scan(
				&poi.ID,
				&poi.Name,
				&poi.Description,
				&poi.Latitude,
				&poi.Longitude,
				&poi.CreatedAt,
				&fileID,
				&file.S3Key,
				&file.FileName,
				&file.FileSize,
				&file.MimeType,
				&file.SerialNumber,
				&file.IsShort,
				&file.CreatedAt,
			)
			if err != nil {
				return nil, fmt.Errorf("scan error: %w", err)
			}
		} else {
			err := rows.Scan(
				&poi.ID,
				&poi.Name,
				&poi.Description,
				&poi.Latitude,
				&poi.Longitude,
				&poi.CreatedAt,
				&fileID,
				&file.S3Key,
				&file.FileName,
				&file.FileSize,
				&file.MimeType,
				&file.SerialNumber,
				&file.IsShort,
				&file.CreatedAt,
			)
			if err != nil {
				return nil, fmt.Errorf("scan error: %w", err)
			}
		}

		if fileID.Valid {
			file.ID = fileID.Int64

			if file.IsShort {
				poi.ShortAudioFile = file
			} else {
				poi.FullAudioFiles = append(poi.FullAudioFiles, file)
			}
		}
	}

	if err = rows.Err(); err != nil {
		return nil, fmt.Errorf("rows error: %w", err)
	}

	if poi == nil {
		return nil, fmt.Errorf("no points of interest found")
	}

	return poi, nil
}
