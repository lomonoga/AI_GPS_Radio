package service

import (
	"aigpsservice/internal/config"
	"aigpsservice/internal/domain"
	"bytes"
	"fmt"
	"io"
	"path/filepath"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/google/uuid"
)

type S3FileStorage struct {
	s3Client   *s3.S3
	bucketName string
	presignTTL time.Duration
}

type FileStorage interface {
	UploadFile(file io.Reader, fileData *domain.File) (string, error)
	DeleteFile(s3Key string) error
	DeleteFiles(s3Keys []string) error
}

func NewS3FileStorage(conf config.Config) (*S3FileStorage, error) {
	awsConfig := &aws.Config{
		Region:           aws.String("us-east-1"),
		Credentials:      credentials.NewStaticCredentials(conf.S3AccessKey, conf.S3SecretKey, ""),
		S3ForcePathStyle: aws.Bool(true),
	}

	if conf.S3Endpoint != "" {
		awsConfig.Endpoint = aws.String(conf.S3Endpoint)
	}

	if !conf.S3UseSSL {
		awsConfig.DisableSSL = aws.Bool(true)
	}

	sess, err := session.NewSession(awsConfig)
	if err != nil {
		return nil, fmt.Errorf("failed to create S3 session: %w", err)
	}

	s3Client := s3.New(sess)

	return &S3FileStorage{
		s3Client:   s3Client,
		bucketName: conf.S3Bucket,
		presignTTL: 24 * time.Hour,
	}, nil
}

func (s *S3FileStorage) UploadFile(file io.Reader, fileData *domain.File) (string, error) {
	fileExt := filepath.Ext(fileData.FileName)
	s3Key := s.generateS3Key(fileData, fileExt)

	fileBytes, err := io.ReadAll(file)
	if err != nil {
		return "", fmt.Errorf("failed to read file: %w", err)
	}

	input := &s3.PutObjectInput{
		Bucket:        aws.String(s.bucketName),
		Key:           aws.String(s3Key),
		Body:          bytes.NewReader(fileBytes),
		ContentLength: aws.Int64(int64(len(fileBytes))),
		ContentType:   aws.String(fileData.MimeType),
		Metadata: map[string]*string{
			"original-filename": aws.String(fileData.FileName),
			"upload-timestamp":  aws.String(time.Now().Format(time.RFC3339)),
		},
	}

	_, err = s.s3Client.PutObject(input)
	if err != nil {
		return "", fmt.Errorf("failed to upload file to S3: %w", err)
	}

	return s3Key, nil
}

func (s *S3FileStorage) DeleteFile(s3Key string) error {
	if s3Key == "" {
		return nil
	}

	input := &s3.DeleteObjectInput{
		Bucket: aws.String(s.bucketName),
		Key:    aws.String(s3Key),
	}

	_, err := s.s3Client.DeleteObject(input)
	if err != nil {
		return fmt.Errorf("failed to delete file from S3: %w", err)
	}

	return nil
}

func (s *S3FileStorage) DeleteFiles(s3Keys []string) error {
	if len(s3Keys) == 0 {
		return nil
	}

	validKeys := make([]string, 0, len(s3Keys))
	for _, key := range s3Keys {
		if key != "" {
			validKeys = append(validKeys, key)
		}
	}

	if len(validKeys) == 0 {
		return nil
	}

	objects := make([]*s3.ObjectIdentifier, len(validKeys))
	for i, key := range validKeys {
		objects[i] = &s3.ObjectIdentifier{
			Key: aws.String(key),
		}
	}

	input := &s3.DeleteObjectsInput{
		Bucket: aws.String(s.bucketName),
		Delete: &s3.Delete{
			Objects: objects,
			Quiet:   aws.Bool(false),
		},
	}

	result, err := s.s3Client.DeleteObjects(input)
	if err != nil {
		return fmt.Errorf("failed to delete files from S3: %w", err)
	}

	if len(result.Errors) > 0 {
		errorMessages := make([]string, 0, len(result.Errors))
		for _, e := range result.Errors {
			errorMsg := ""
			if e.Key != nil {
				errorMsg += fmt.Sprintf("Key: %s, ", *e.Key)
			}
			if e.Code != nil {
				errorMsg += fmt.Sprintf("Code: %s, ", *e.Code)
			}
			if e.Message != nil {
				errorMsg += fmt.Sprintf("Message: %s", *e.Message)
			}
			errorMessages = append(errorMessages, errorMsg)
		}

		return fmt.Errorf("some files failed to delete: %v", strings.Join(errorMessages, "; "))
	}

	return nil
}

func (s *S3FileStorage) generateS3Key(fileData *domain.File, fileExt string) string {
	fileID := uuid.New().String()

	var folder string
	switch {
	case fileData.MimeType == "image/jpeg" || fileData.MimeType == "image/png" || fileData.MimeType == "image/webp":
		folder = "images"
	case fileData.MimeType == "audio/mpeg" || fileData.MimeType == "audio/wav" || fileData.MimeType == "audio/ogg":
		folder = "audio"
	default:
		folder = "files"
	}

	now := time.Now()
	return fmt.Sprintf("%s/%d/%02d/%02d/%s%s",
		folder,
		now.Year(), now.Month(), now.Day(),
		fileID, fileExt)
}

func (s *S3FileStorage) HealthCheck() error {
	_, err := s.s3Client.HeadBucket(&s3.HeadBucketInput{
		Bucket: aws.String(s.bucketName),
	})
	return err
}
