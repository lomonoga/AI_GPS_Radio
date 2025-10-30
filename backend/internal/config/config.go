package config

import (
	"os"
	"sync"
)

type Config struct {
	DBHost     string
	DBPort     string
	DBUser     string
	DBPassword string
	DBName     string
	ServerPort string
}

var configInstance *Config
var once sync.Once

func Load() *Config {
	once.Do(func() {
		configInstance = &Config{
			DBHost:     getEnv("DB_HOST_AIGPSSERVICE", "localhost"),
			DBPort:     getEnv("DB_PORT_AIGPSSERVICE", "5432"),
			DBUser:     getEnv("DB_USER_AIGPSSERVICE", "postgres"),
			DBPassword: getEnv("DB_PASSWORD_AIGPSSERVICE", "postgres"),
			DBName:     getEnv("DB_NAME_AIGPSSERVICE", "aigpsservice"),
			ServerPort: getEnv("SERVER_PORT_AIGPSSERVICE", "8765"),
		}
	})
	return configInstance
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}
