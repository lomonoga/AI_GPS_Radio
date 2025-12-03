package handler

import (
	"aigpsservice/internal/domain"
	"aigpsservice/internal/service"
	"aigpsservice/pkg/logger"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"strconv"
	"time"
)

type POIHandler struct {
	poiService *service.POIService
}

func NewPOIHandler(poiService *service.POIService) *POIHandler {
	return &POIHandler{
		poiService: poiService,
	}
}

type Response struct {
	Data  any    `json:"data,omitempty"`
	Error string `json:"error,omitempty"`
}

func (h *POIHandler) writeJSON(w http.ResponseWriter, status int, data any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *POIHandler) writeError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(Response{Error: message})
}

// CreatePOI godoc
// @Tags POI
// @Summary Создание новой точки интереса
// @Description Создает новую точку интереса с изображением и аудиофайлами
// @Accept multipart/form-data
// @Param name formData string true "Название точки интереса"
// @Param description formData string true "Описание точки интереса"
// @Param latitude formData number true "Широта"
// @Param longitude formData number true "Долгота"
// @Param image formData file true "Изображение точки интереса"
// @Param short_audio formData file true "Короткое аудио"
// @Param full_audio formData []file true "Полные аудио файлы"
// @Success 201 {object} domain.PointOfInterest
// @Failure 400 {object} Response
// @Failure 500 {object} Response
// @Router /api/poi/create [post]
func (h *POIHandler) CreatePOI(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		h.writeError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	err := r.ParseMultipartForm(32 << 20)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Failed to parse form data: "+err.Error())
		return
	}

	name := r.FormValue("name")
	description := r.FormValue("description")
	latStr := r.FormValue("latitude")
	lngStr := r.FormValue("longitude")

	if name == "" || description == "" || latStr == "" || lngStr == "" {
		h.writeError(w, http.StatusBadRequest, "Missing required fields: name, description, latitude, longitude")
		return
	}

	latitude, err := strconv.ParseFloat(latStr, 64)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid latitude format")
		return
	}

	longitude, err := strconv.ParseFloat(lngStr, 64)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid longitude format")
		return
	}

	imageFile, imageHeader, err := r.FormFile("image")
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Image file is required: "+err.Error())
		return
	}
	defer imageFile.Close()

	poiRequest := &domain.PointOfInterest{
		Name:        name,
		Description: description,
		Latitude:    latitude,
		Longitude:   longitude,
		CreatedAt:   time.Now(),
	}

	var shortAudioFile multipart.File
	var shortAudioHeader *multipart.FileHeader
	shortAudioFile, shortAudioHeader, err = r.FormFile("short_audio")
	if err == nil && shortAudioHeader != nil {
		defer shortAudioFile.Close()

		shortAudio := &domain.File{
			FileName:     shortAudioHeader.Filename,
			FileSize:     shortAudioHeader.Size,
			MimeType:     shortAudioHeader.Header.Get("Content-Type"),
			IsShort:      true,
			SerialNumber: 1,
			CreatedAt:    time.Now(),
		}
		poiRequest.ShortAudioFile = shortAudio
	}

	fullAudioFiles := make([]multipart.File, 0)
	fullAudioFileData := make([]*domain.File, 0)

	if r.MultipartForm != nil && r.MultipartForm.File != nil {
		if files, ok := r.MultipartForm.File["full_audio"]; ok {
			for i, fileHeader := range files {
				file, err := fileHeader.Open()
				if err != nil {
					logger.Error.Printf("Failed to open full audio file %s: %v\n", fileHeader.Filename, err)
					continue
				}

				fullAudio := &domain.File{
					FileName:     fileHeader.Filename,
					FileSize:     fileHeader.Size,
					MimeType:     fileHeader.Header.Get("Content-Type"),
					IsShort:      false,
					SerialNumber: int64(i + 1),
					CreatedAt:    time.Now(),
				}

				fullAudioFiles = append(fullAudioFiles, file)
				fullAudioFileData = append(fullAudioFileData, fullAudio)
			}
		}
	}
	poiRequest.FullAudioFiles = fullAudioFileData

	imageFileData := &domain.File{
		FileName:     imageHeader.Filename,
		FileSize:     imageHeader.Size,
		MimeType:     imageHeader.Header.Get("Content-Type"),
		IsShort:      false,
		SerialNumber: 0,
		CreatedAt:    time.Now(),
	}

	createdPOI, err := h.poiService.CreatePOI(poiRequest, imageFileData, imageFile, shortAudioFile, fullAudioFiles)
	if err != nil {
		h.writeError(w, http.StatusInternalServerError, "Failed to create point of interest: "+err.Error())
		return
	}

	h.writeJSON(w, http.StatusCreated, Response{Data: createdPOI})
}

// FindNearestPOI godoc
// @Tags POI
// @Summary Поиск ближайшей точки интереса
// @Description Возвращает ближайшие точки интереса по координатам
// @Param latitude query number true "Широта" example(55.7558)
// @Param longitude query number true "Долгота" example(37.6173)
// @Param radius query number false "Радиус в метрах" example(100) default(150)
// @Success 200 {object} domain.PointOfInterest
// @Router /api/poi/nearby [get]
func (h *POIHandler) FindNearestPOI(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	query := r.URL.Query()

	latStr := query.Get("latitude")
	lngStr := query.Get("longitude")
	radiusStr := query.Get("radius")

	if radiusStr == "" {
		radiusStr = "150"
	}
	radius, err := strconv.Atoi(radiusStr)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "The radius must be a number")
		return
	}

	if latStr == "" || lngStr == "" {
		h.writeError(w, http.StatusBadRequest, "Parameters 'latitude' and 'longitude' are required")
		return
	}

	lat, err := strconv.ParseFloat(latStr, 64)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid latitude format")
		return
	}

	lng, err := strconv.ParseFloat(lngStr, 64)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid longitude format")
		return
	}

	poi, err := h.poiService.FindNearestPOI(lat, lng, radius)
	if err != nil {
		h.writeError(w, http.StatusNotFound, err.Error())
		return
	}

	h.writeJSON(w, http.StatusOK, Response{Data: poi})
}

// DeletePOI godoc
// @Tags POI
// @Summary Удаление точки интереса по id
// @Description Удаляет точку интереса со всеми связанными файлами
// @Param id query number true "Id точки интереса" example(195)
// @Success 200 {boolean} true "Успешное выполнение"
// @Router /api/poi/delete [delete]
func (h *POIHandler) DeletePOI(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete {
		h.writeError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	query := r.URL.Query()

	idPOIStr := query.Get("id")
	if idPOIStr == "" {
		h.writeError(w, http.StatusBadRequest, "Missing required field: id")
		return
	}
	idPOI, err := strconv.Atoi(idPOIStr)
	if err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid id format")
		return
	}

	resultDelete, err := h.poiService.DeletePOI(idPOI)
	if err != nil {
		h.writeError(w, http.StatusNotFound, err.Error())
		return
	}

	h.writeJSON(w, http.StatusOK, Response{Data: resultDelete})
}

// HealthCheck godoc
// @Tags Health
// @Summary Проверка здоровья сервиса
// @Description Проверяет доступность сервиса
// @Success 200 {object} Response "Успешный ответ"
// @Router /health [get]
func (h *POIHandler) HealthCheck(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	h.writeJSON(w, http.StatusOK, Response{Data: map[string]string{
		"status": "OK",
	}})
}
