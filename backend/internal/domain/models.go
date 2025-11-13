package domain

import "time"

type File struct {
	ID           int64     `json:"id"`
	S3Key        string    `json:"s3_key"`
	FileName     string    `json:"file_name"`
	FileSize     int64     `json:"file_size,omitempty"`
	MimeType     string    `json:"mime_type,omitempty"`
	SerialNumber int64     `json:"serial_number"`
	IsShort      bool      `json:"is_short"`
	CreatedAt    time.Time `json:"created_at"`
}

type PointOfInterest struct {
	ID             int64     `json:"id"`
	Name           string    `json:"name"`
	Description    string    `json:"description,omitempty"`
	Latitude       float64   `json:"latitude"`
	Longitude      float64   `json:"longitude"`
	FullAudioFiles []File    `json:"full_audio_files,omitempty"`
	ShortAudioFile *File     `json:"short_audio_file,omitempty"`
	CreatedAt      time.Time `json:"created_at"`
}

type POIRepository interface {
	FindNearestPOI(latitude, longitude float64) (*PointOfInterest, error)
}
