package main

import (
	"aigpsservice/internal/config"
	"aigpsservice/internal/handler"
	"aigpsservice/internal/repository"
	"aigpsservice/internal/repository/postgres"
	"aigpsservice/internal/service"
	"aigpsservice/pkg/database"
	"aigpsservice/pkg/logger"
	"log"
	"net/http"
)

func main() {
	logger.Init()

	cfg := config.Load()

	db, err := database.NewPostgresDB(cfg)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}
	defer db.Close()

	poiRepo := postgres.NewPOIRepository(db)
	repos := &repository.Repositories{
		POI: poiRepo,
	}

	poiService := service.NewPOIService(repos.POI)

	poiHandler := handler.NewPOIHandler(poiService)

	http.HandleFunc("/api/poi/nearby", poiHandler.FindNearestPOI)
	http.HandleFunc("/health", poiHandler.HealthCheck)

	logger.Info.Printf("Server starting on port %s", cfg.ServerPort)
	if err := http.ListenAndServe(":"+cfg.ServerPort, nil); err != nil {
		logger.Error.Fatal("Failed to start server:", err)
	}
}
