package service

import (
	"aigpsservice/internal/config"
	"aigpsservice/pkg/logger"
	"database/sql"

	"github.com/golang-migrate/migrate/v4"
	"github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	_ "github.com/lib/pq"
)

func RunMigrations(config *config.Config, db *sql.DB) error {

	if err := db.Ping(); err != nil {
		logger.Error.Fatalf("Failed to ping database: %v", err)
	}

	driver, err := postgres.WithInstance(db, &postgres.Config{})
	if err != nil {
		logger.Error.Fatal("Could not create migration driver: %w", err)
	}

	m, err := migrate.NewWithDatabaseInstance(
		"file://migrations",
		config.DBName,
		driver)
	if err != nil {
		logger.Error.Fatal("Could not create migration instance: %w", err)
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		logger.Error.Fatal("Could not run migrations: %w", err)
	}

	logger.Info.Println("Migrations applied successfully")
	return nil
}
