package router

import (
	"aigpsservice/internal/config"
	"aigpsservice/internal/handler"
	"aigpsservice/internal/repository"
	"aigpsservice/internal/service"
	"aigpsservice/pkg/logger"
	"database/sql"
	"log"
	"net/http"
	"net/http/httptest"
	"time"

	_ "aigpsservice/docs"

	httpSwagger "github.com/swaggo/http-swagger"
)

// @title AIGPS Service API
// @version 1.0
// @description API для сервиса геолокации и точек интереса
// @host 45.150.8.131:8080
// @BasePath /
func SetupRouter(cfg *config.Config, db *sql.DB) http.Handler {
	mux := http.NewServeMux()

	poiRepo := repository.NewPOIRepository(db)
	fileStorage, err := service.NewS3FileStorage(*cfg)
	if err != nil {
		logger.Error.Fatalln("Error init s3 file Storage")
	}

	poiService := service.NewPOIService(poiRepo, fileStorage)
	poiHandler := handler.NewPOIHandler(poiService)
	s3Proxy, err := handler.NewS3Proxy(cfg)
	if err != nil {
		logger.Error.Fatalf("Failed to create S3 proxy: %v", err)
	}

	// Swagger docs
	mux.Handle("/swagger/", httpSwagger.WrapHandler)

	// Health endpoint
	mux.HandleFunc("/health", poiHandler.HealthCheck)

	// POI endpoints
	mux.HandleFunc("/api/poi/nearby", poiHandler.FindNearestPOI)
	mux.HandleFunc("/api/poi/create", poiHandler.CreatePOI)

	// S3 proxy
	mux.HandleFunc("/s3/files/", s3Proxy.ProxyGet)
	mux.HandleFunc("/s3/list", s3Proxy.ListObjects)
	mux.HandleFunc("/s3/health", s3Proxy.HealthCheck)

	handler := applyMiddleware(mux)

	return handler
}

func applyMiddleware(handler http.Handler) http.Handler {
	handler = loggingMiddleware(handler)
	return handler
}

func loggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		recorder := httptest.NewRecorder()
		next.ServeHTTP(recorder, r)

		for key, values := range recorder.Header() {
			for _, value := range values {
				w.Header().Add(key, value)
			}
		}
		w.WriteHeader(recorder.Code)
		w.Write(recorder.Body.Bytes())

		duration := time.Since(start)
		log.Printf(
			"[%s] %s %s - %d %v",
			r.Method,
			r.URL.Path,
			r.RemoteAddr,
			recorder.Code,
			duration,
		)
	})
}
