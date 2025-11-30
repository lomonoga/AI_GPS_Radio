package handler

import (
	"aigpsservice/internal/config"
	"aigpsservice/pkg/logger"
	"context"
	"fmt"
	"io"
	"net/http"
	"path/filepath"
	"slices"
	"strings"
	"time"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
)

type S3Proxy struct {
	client *minio.Client
	bucket string
}

func NewS3Proxy(cfg *config.Config) (*S3Proxy, error) {
	endpoint := cfg.S3Endpoint
	accessKey := cfg.S3AccessKey
	secretKey := cfg.S3SecretKey
	useSSL := cfg.S3UseSSL
	bucket := cfg.S3Bucket

	client, err := minio.New(endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(accessKey, secretKey, ""),
		Secure: useSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create MinIO client: %w", err)
	}

	ctx := context.Background()
	exists, err := client.BucketExists(ctx, bucket)
	if err != nil {
		return nil, fmt.Errorf("failed to check bucket existence: %w", err)
	}

	if !exists {
		err = client.MakeBucket(ctx, bucket, minio.MakeBucketOptions{})
		if err != nil {
			return nil, fmt.Errorf("failed to create bucket: %w", err)
		}
		logger.Info.Printf("Bucket '%s' created successfully", bucket)
	}

	logger.Info.Printf("S3Proxy initialized: endpoint=%s, bucket=%s", endpoint, bucket)
	return &S3Proxy{
		client: client,
		bucket: bucket,
	}, nil
}

// GetFile godoc
// @Tags S3
// @Summary Получить файл из S3
// @Description Скачивает файл из S3 хранилища по указанному пути
// @Param path path string true "Путь к файлу в S3" example("images/photo.jpg")
// @Success 200 {file} byte "Файл"
// @Header 200 {string} Content-Type "MIME-тип файла"
// @Header 200 {string} Content-Length "Размер файла в байтах"
// @Router /s3/files/{path} [get]
func (p *S3Proxy) ProxyGet(w http.ResponseWriter, r *http.Request) {
	objectPath := strings.TrimPrefix(r.URL.Path, "s3/files/")
	if objectPath == "" {
		http.Error(w, "File path is required", http.StatusBadRequest)
		return
	}

	logger.Info.Printf("Getting file: %s", objectPath)

	ctx := context.Background()
	object, err := p.client.GetObject(ctx, p.bucket, objectPath, minio.GetObjectOptions{})
	if err != nil {
		logger.Error.Printf("Error getting object: %v", err)
		http.Error(w, fmt.Sprintf("Failed to get object: %v", err), http.StatusInternalServerError)
		return
	}
	defer object.Close()

	objInfo, err := object.Stat()
	if err != nil {
		logger.Error.Printf("Error stating object: %v", err)
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", getContentType(objectPath))
	w.Header().Set("Content-Length", fmt.Sprintf("%d", objInfo.Size))
	w.Header().Set("Last-Modified", objInfo.LastModified.Format(time.RFC1123))
	w.Header().Set("ETag", objInfo.ETag)

	if isStaticFile(objectPath) {
		w.Header().Set("Cache-Control", "public, max-age=3600")
	}

	_, err = io.Copy(w, object)
	if err != nil {
		logger.Error.Printf("Error streaming object: %v", err)
		http.Error(w, "Failed to stream file", http.StatusInternalServerError)
		return
	}

	logger.Info.Printf("File served successfully: %s (%d bytes)", objectPath, objInfo.Size)
}

// ListFiles godoc
// @Tags S3
// @Summary Получить список файлов
// @Description Возвращает список файлов в S3 бакете с возможностью фильтрации по префиксу
// @Param prefix query string false "Префикс для фильтрации файлов" example("images/")
// @Success 200 {object} domain.S3ListResponse
// @Router /s3/list [get]
func (p *S3Proxy) ListObjects(w http.ResponseWriter, r *http.Request) {
	prefix := r.URL.Query().Get("prefix")

	ctx := context.Background()
	objectsCh := p.client.ListObjects(ctx, p.bucket, minio.ListObjectsOptions{
		Prefix:    prefix,
		Recursive: true,
	})

	type FileInfo struct {
		Name         string    `json:"name"`
		Size         int64     `json:"size"`
		LastModified time.Time `json:"last_modified"`
		ContentType  string    `json:"content_type"`
	}

	var files []FileInfo
	for object := range objectsCh {
		if object.Err != nil {
			logger.Error.Printf("Error listing objects: %v", object.Err)
			http.Error(w, "Failed to list files", http.StatusInternalServerError)
			return
		}
		files = append(files, FileInfo{
			Name:         object.Key,
			Size:         object.Size,
			LastModified: object.LastModified,
			ContentType:  getContentType(object.Key),
		})
	}

	w.Header().Set("Content-Type", "application/json")
	fmt.Fprintf(w, `{"files": %v}`, files)
	logger.Info.Printf("Listed %d files with prefix: %s", len(files), prefix)
}

// HealthCheck godoc
// @Tags S3
// @Summary Проверка соединения с S3 хранилищем
// @Description Проверяет соединение с S3 хранилищем
// @Router /s3/health [get]
func (p *S3Proxy) HealthCheck(w http.ResponseWriter, r *http.Request) {
	ctx := context.Background()
	_, err := p.client.ListBuckets(ctx)
	if err != nil {
		http.Error(w, `{"status": "error", "message": "Cannot connect to MinIO"}`, http.StatusServiceUnavailable)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	fmt.Fprint(w, `{"status": "healthy", "message": "Connected to MinIO"}`)
}

func getContentType(filename string) string {
	ext := strings.ToLower(filepath.Ext(filename))
	switch ext {
	case ".mp3":
		return "audio/mpeg"
	case ".wav":
		return "audio/wav"
	case ".ogg":
		return "audio/ogg"
	case ".m4a":
		return "audio/mp4"
	case ".aac":
		return "audio/aac"
	case ".flac":
		return "audio/flac"
	case ".webm":
		return "audio/webm"
	case ".mp4":
		return "video/mp4"
	case ".avi":
		return "video/x-msvideo"
	case ".mov":
		return "video/quicktime"
	case ".wmv":
		return "video/x-ms-wmv"
	case ".flv":
		return "video/webm"
	case ".mkv":
		return "video/x-matroska"
	case ".mpeg", ".mpg":
		return "video/mpeg"
	case ".3gp":
		return "video/3gpp"
	case ".ts":
		return "video/mp2t"
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	case ".gif":
		return "image/gif"
	case ".pdf":
		return "application/pdf"
	case ".txt":
		return "text/plain"
	case ".html":
		return "text/html"
	case ".css":
		return "text/css"
	case ".js":
		return "application/javascript"
	case ".json":
		return "application/json"
	default:
		return "application/octet-stream"
	}
}

func isStaticFile(filename string) bool {
	ext := strings.ToLower(filepath.Ext(filename))
	staticExtensions := []string{".jpg", ".jpeg", ".png", ".gif", ".css", ".js", ".pdf"}
	return slices.Contains(staticExtensions, ext)
}
