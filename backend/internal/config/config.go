package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"sync"
)

type Config struct {
	DBHost      string
	DBPort      string
	DBUser      string
	DBPassword  string
	DBName      string
	ServerPort  string
	S3Endpoint  string
	S3AccessKey string
	S3SecretKey string
	S3UseSSL    bool
	S3Bucket    string
}

var configInstance *Config
var once sync.Once

func Load() *Config {
	once.Do(func() {
		configInstance = &Config{
			DBHost:      getEnv("DB_HOST_AIGPSSERVICE", "localhost"),
			DBPort:      getEnv("DB_PORT_AIGPSSERVICE", "5432"),
			DBUser:      getEnv("DB_USER_AIGPSSERVICE", "postgres"),
			DBPassword:  getEnv("DB_PASSWORD_AIGPSSERVICE", "postgres"),
			DBName:      getEnv("DB_NAME_AIGPSSERVICE", "postgres"),
			ServerPort:  getEnv("SERVER_PORT_AIGPSSERVICE", "8080"),
			S3Endpoint:  getEnv("S3_ENDPOINT_AIGPSSERVICE", "localhost:9000"),
			S3AccessKey: getEnv("S3_ACCESS_KEY_AIGPSSERVICE", "minioadmin"),
			S3SecretKey: getEnv("S3_SECRET_KEY_AIGPSSERVICE", "minioadmin"),
			S3UseSSL:    getEnvBool("S3_USE_SSL_AIGPSSERVICE", false),
			S3Bucket:    getEnv("S3_BUCKET_AIGPSSERVICE", "default"),
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

func getEnvBool(key string, defaultValue bool) bool {
	if value, exists := os.LookupEnv(key); exists {
		if boolValue, err := strconv.ParseBool(value); err == nil {
			return boolValue
		}
		switch strings.ToLower(value) {
		case "true", "yes", "1", "on":
			return true
		case "false", "no", "0", "off":
			return false
		}
	}
	return defaultValue
}

func (c *Config) GetDBConnectionString() string {
	return fmt.Sprintf("host=%s port=%s dbname=%s user=%s password=%s sslmode=disable",
		c.DBHost,
		c.DBPort,
		c.DBName,
		c.DBUser,
		c.DBPassword,
	)
}
