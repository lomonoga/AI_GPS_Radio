package main

import (
	"aigpsservice/internal/config"
	"aigpsservice/internal/router"
	"aigpsservice/internal/service"
	"aigpsservice/pkg/database"
	"aigpsservice/pkg/logger"
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	logger.Init()

	cfg := config.Load()

	db, err := database.NewPostgresDB(cfg)
	if err != nil {
		logger.Error.Fatal("Failed to connect to database:", err)
	}
	defer db.Close()

	handler := router.SetupRouter(cfg, db)
	server := &http.Server{
		Addr:    ":" + cfg.ServerPort,
		Handler: handler,
	}

	err = service.RunMigrations(cfg, db)
	if err != nil {
		logger.Error.Fatal("Could not run migrations: %w", err)
	}

	go func() {
		logger.Info.Printf("Server starting on port %s", cfg.ServerPort)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error.Fatalf("Server failed: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info.Println("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		logger.Error.Fatalf("Server forced to shutdown: %v", err)
	}

	logger.Info.Println("Server exited")
}
