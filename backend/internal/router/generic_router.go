package router

import (
	"aigpsservice/internal/config"
	"aigpsservice/internal/handler"
	"aigpsservice/internal/repository"
	"aigpsservice/internal/repository/postgres"
	"aigpsservice/internal/service"
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
// @host ip:8080
// @BasePath /
func SetupRouter(cfg *config.Config, db *sql.DB) http.Handler {
	mux := http.NewServeMux()

	poiRepo := postgres.NewPOIRepository(db)
	repos := &repository.Repositories{
		POI: poiRepo,
	}

	poiService := service.NewPOIService(repos.POI)
	poiHandler := handler.NewPOIHandler(poiService)

	// Swagger docs
	mux.Handle("/swagger/", httpSwagger.WrapHandler)

	// Health endpoint
	mux.HandleFunc("/health", poiHandler.HealthCheck)

	// POI endpoints
	mux.HandleFunc("/api/poi/nearby", poiHandler.FindNearestPOI)

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
