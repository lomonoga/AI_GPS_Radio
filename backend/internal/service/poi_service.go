package service

import (
	"aigpsservice/internal/domain"
	"aigpsservice/internal/repository"
	"context"
	"fmt"
	"mime/multipart"
	"time"
)

type POIService struct {
	repo         *repository.POIRepository
	fileStorage  FileStorage
	maxImageSize int64
	maxAudioSize int64
}

func NewPOIService(repo *repository.POIRepository, fileStorage FileStorage) *POIService {
	return &POIService{
		repo:         repo,
		fileStorage:  fileStorage,
		maxImageSize: 10 << 20,
		maxAudioSize: 50 << 20,
	}
}

func (s *POIService) CreatePOI(
	poi *domain.PointOfInterest,
	imageFileData *domain.File,
	imageFile multipart.File,
	shortAudioFile multipart.File,
	fullAudioFiles []multipart.File,
) (*domain.PointOfInterest, error) {

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := s.validatePOI(poi); err != nil {
		return nil, fmt.Errorf("validation failed: %w", err)
	}

	imageS3Key, err := s.uploadImage(ctx, imageFile, imageFileData)
	if err != nil {
		return nil, fmt.Errorf("failed to upload image: %w", err)
	}
	imageFileData.S3Key = imageS3Key
	poi.ImageFile = imageFileData
	defer imageFile.Close()

	var shortAudioS3Key string
	if shortAudioFile != nil && poi.ShortAudioFile != nil {
		shortAudioS3Key, err = s.uploadAudio(ctx, shortAudioFile, poi.ShortAudioFile)
		if err != nil {
			s.cleanupFile(imageS3Key)
			return nil, fmt.Errorf("failed to upload short audio: %w", err)
		}
		poi.ShortAudioFile.S3Key = shortAudioS3Key
		defer shortAudioFile.Close()
	}

	uploadedFullAudioKeys := make([]string, 0)
	for i, fullAudioFile := range fullAudioFiles {
		if i < len(poi.FullAudioFiles) {
			fullAudioData := poi.FullAudioFiles[i]
			fullAudioS3Key, err := s.uploadAudio(ctx, fullAudioFile, fullAudioData)
			if err != nil {
				s.cleanupFile(imageS3Key)
				if shortAudioS3Key != "" {
					s.cleanupFile(shortAudioS3Key)
				}
				for _, key := range uploadedFullAudioKeys {
					s.cleanupFile(key)
				}
				return nil, fmt.Errorf("failed to upload full audio %s: %w", fullAudioData.FileName, err)
			}
			fullAudioData.S3Key = fullAudioS3Key
			uploadedFullAudioKeys = append(uploadedFullAudioKeys, fullAudioS3Key)
			defer fullAudioFile.Close()
		}
	}

	createdPOI, err := s.repo.CreatePOI(ctx, poi)
	if err != nil {
		s.cleanupFile(imageS3Key)
		if shortAudioS3Key != "" {
			s.cleanupFile(shortAudioS3Key)
		}
		for _, key := range uploadedFullAudioKeys {
			s.cleanupFile(key)
		}
		return nil, fmt.Errorf("failed to save POI to database: %w", err)
	}

	return createdPOI, nil
}

func (s *POIService) validatePOI(poi *domain.PointOfInterest) error {
	if poi.Name == "" {
		return fmt.Errorf("name is required")
	}
	if poi.Description == "" {
		return fmt.Errorf("description is required")
	}
	if poi.Latitude < -90 || poi.Latitude > 90 {
		return fmt.Errorf("invalid latitude: %f", poi.Latitude)
	}
	if poi.Longitude < -180 || poi.Longitude > 180 {
		return fmt.Errorf("invalid longitude: %f", poi.Longitude)
	}
	return nil
}

func (s *POIService) uploadImage(_ context.Context, file multipart.File, fileData *domain.File) (string, error) {
	if fileData.FileSize > s.maxImageSize {
		return "", fmt.Errorf("image size %d exceeds maximum allowed %d", fileData.FileSize, s.maxImageSize)
	}

	if !s.isValidImageType(fileData.MimeType) {
		return "", fmt.Errorf("unsupported image type: %s", fileData.MimeType)
	}

	s3Key, err := s.fileStorage.UploadFile(file, fileData)
	if err != nil {
		return "", fmt.Errorf("failed to upload image to storage: %w", err)
	}

	return s3Key, nil
}

func (s *POIService) uploadAudio(_ context.Context, file multipart.File, fileData *domain.File) (string, error) {
	if fileData.FileSize > s.maxAudioSize {
		return "", fmt.Errorf("audio size %d exceeds maximum allowed %d", fileData.FileSize, s.maxAudioSize)
	}

	if !s.isValidAudioType(fileData.MimeType) {
		return "", fmt.Errorf("unsupported audio type: %s", fileData.MimeType)
	}

	s3Key, err := s.fileStorage.UploadFile(file, fileData)
	if err != nil {
		return "", fmt.Errorf("failed to upload audio to storage: %w", err)
	}

	return s3Key, nil
}

func (s *POIService) isValidImageType(mimeType string) bool {
	supportedTypes := map[string]bool{
		"image/jpeg": true,
		"image/jpg":  true,
		"image/png":  true,
		"image/webp": true,
		"image/gif":  true,
	}
	return supportedTypes[mimeType]
}

func (s *POIService) isValidAudioType(mimeType string) bool {
	supportedTypes := map[string]bool{
		"audio/mpeg":  true,
		"audio/mp3":   true,
		"audio/wav":   true,
		"audio/ogg":   true,
		"audio/aac":   true,
		"audio/x-m4a": true,
	}
	return supportedTypes[mimeType]
}

func (s *POIService) cleanupFile(s3Key string) error {
	if s3Key == "" {
		return nil
	}
	return s.fileStorage.DeleteFile(s3Key)
}

func (s *POIService) FindNearestPOI(latitude, longitude float64, radius int, interests []string) (*domain.PointOfInterest, error) {
	if latitude < -90 || latitude > 90 {
		return nil, fmt.Errorf("invalid latitude: must be between -90 and 90")
	}
	if longitude < -180 || longitude > 180 {
		return nil, fmt.Errorf("invalid longitude: must be between -180 and 180")
	}

	return s.repo.FindNearestPOI(latitude, longitude, radius, interests)
}

func (s *POIService) DeletePOI(idPOI int) (bool, error) {
	poi, err := s.repo.GetPOIById(idPOI)
	if err != nil {
		return false, fmt.Errorf("POI not found: %v", err)
	}

	// Delete files from s3
	s3FileKeys := make([]string, 0, len(poi.FullAudioFiles)+2)
	for _, file := range poi.FullAudioFiles {
		if file != nil {
			s3FileKeys = append(s3FileKeys, file.S3Key)
		}
	}
	if poi.ImageFile != nil {
		s3FileKeys = append(s3FileKeys, poi.ImageFile.S3Key)
	}
	if poi.ShortAudioFile != nil {
		s3FileKeys = append(s3FileKeys, poi.ShortAudioFile.S3Key)
	}

	err = s.fileStorage.DeleteFiles(s3FileKeys)
	if err != nil {
		return false, fmt.Errorf("falid to delete files from s3: %v", err)
	}

	// Delete poi and files from db
	idFiles := make([]int64, 0, len(poi.FullAudioFiles)+2)
	for _, file := range poi.FullAudioFiles {
		if file != nil {
			idFiles = append(idFiles, file.ID)
		}
	}
	if poi.ImageFile != nil {
		idFiles = append(idFiles, poi.ImageFile.ID)
	}
	if poi.ShortAudioFile != nil {
		idFiles = append(idFiles, poi.ShortAudioFile.ID)
	}

	return s.repo.DeletePOI(idPOI, idFiles)
}
