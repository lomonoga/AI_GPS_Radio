package repository

import (
	"aigpsservice/internal/domain"
	"context"
	"database/sql"
	"fmt"
)

type POIRepository struct {
	db *sql.DB
}

func NewPOIRepository(db *sql.DB) *POIRepository {
	return &POIRepository{db: db}
}

func (r *POIRepository) FindNearestPOI(latitude, longitude float64, radius int) (*domain.PointOfInterest, error) {
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
			WHERE ST_DWithin(
				p.location::geography,
				ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography,
				$3
			)
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

	rows, err := r.db.Query(query, longitude, latitude, radius)
	if err != nil {
		return nil, fmt.Errorf("database query error: %w", err)
	}
	defer rows.Close()

	var poi *domain.PointOfInterest

	for rows.Next() {
		var fileID sql.NullInt64
		var s3Key sql.NullString
		var fileName sql.NullString
		var fileSize sql.NullInt64
		var mimeType sql.NullString
		var serialNumber sql.NullInt64
		var isShort sql.NullBool
		var fileCreatedAt sql.NullTime

		if poi == nil {
			poi = &domain.PointOfInterest{
				FullAudioFiles: []*domain.File{},
			}

			err := rows.Scan(
				&poi.ID,
				&poi.Name,
				&poi.Description,
				&poi.Latitude,
				&poi.Longitude,
				&poi.CreatedAt,
				&fileID,
				&s3Key,
				&fileName,
				&fileSize,
				&mimeType,
				&serialNumber,
				&isShort,
				&fileCreatedAt,
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
				&s3Key,
				&fileName,
				&fileSize,
				&mimeType,
				&serialNumber,
				&isShort,
				&fileCreatedAt,
			)
			if err != nil {
				return nil, fmt.Errorf("scan error: %w", err)
			}
		}

		if fileID.Valid {
			file := domain.File{
				ID:           fileID.Int64,
				S3Key:        s3Key.String,
				FileName:     fileName.String,
				FileSize:     fileSize.Int64,
				MimeType:     mimeType.String,
				SerialNumber: serialNumber.Int64,
				IsShort:      isShort.Bool,
				CreatedAt:    fileCreatedAt.Time,
			}

			if file.SerialNumber == 0 { // If it is image
				poi.ImageFile = &file
			} else if file.IsShort { // If it is short audio
				poi.ShortAudioFile = &file
			} else { // If it is full audio
				poi.FullAudioFiles = append(poi.FullAudioFiles, &file)
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

func (r *POIRepository) CreatePOI(ctx context.Context, poi *domain.PointOfInterest) (*domain.PointOfInterest, error) {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	var poiID int64
	poiQuery := `
		INSERT INTO points_of_interest (name, description, location, created_at)
		VALUES ($1, $2, ST_SetSRID(ST_MakePoint($3, $4), 4326), $5)
		RETURNING id
	`

	err = tx.QueryRowContext(ctx, poiQuery,
		poi.Name,
		poi.Description,
		poi.Longitude,
		poi.Latitude,
		poi.CreatedAt,
	).Scan(&poiID)
	if err != nil {
		return nil, fmt.Errorf("failed to insert POI: %w", err)
	}

	var imageID int64
	imageQuery := `
		INSERT INTO poi_files (poi_id, s3_key, file_name, file_size, mime_type, serial_number, is_short, created_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id
	`

	err = tx.QueryRowContext(ctx, imageQuery,
		poiID,
		poi.ImageFile.S3Key,
		poi.ImageFile.FileName,
		poi.ImageFile.FileSize,
		poi.ImageFile.MimeType,
		poi.ImageFile.SerialNumber,
		poi.ImageFile.IsShort,
		poi.ImageFile.CreatedAt,
	).Scan(&imageID)
	if err != nil {
		return nil, fmt.Errorf("failed to insert image file: %w", err)
	}
	poi.ImageFile.ID = imageID
	poi.ImageFile = poi.ImageFile

	if poi.ShortAudioFile != nil {
		var shortAudioID int64
		err = tx.QueryRowContext(ctx, imageQuery,
			poiID,
			poi.ShortAudioFile.S3Key,
			poi.ShortAudioFile.FileName,
			poi.ShortAudioFile.FileSize,
			poi.ShortAudioFile.MimeType,
			poi.ShortAudioFile.SerialNumber,
			poi.ShortAudioFile.IsShort,
			poi.ShortAudioFile.CreatedAt,
		).Scan(&shortAudioID)
		if err != nil {
			return nil, fmt.Errorf("failed to insert short audio file: %w", err)
		}
		poi.ShortAudioFile.ID = shortAudioID
	}

	for _, fullAudio := range poi.FullAudioFiles {
		var fullAudioID int64
		err = tx.QueryRowContext(ctx, imageQuery,
			poiID,
			fullAudio.S3Key,
			fullAudio.FileName,
			fullAudio.FileSize,
			fullAudio.MimeType,
			fullAudio.SerialNumber,
			fullAudio.IsShort,
			fullAudio.CreatedAt,
		).Scan(&fullAudioID)
		if err != nil {
			return nil, fmt.Errorf("failed to insert full audio file: %w", err)
		}
		fullAudio.ID = fullAudioID
	}

	if err = tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	poi.ID = poiID

	return poi, nil
}
