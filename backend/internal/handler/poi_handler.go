package handler

import (
	"aigpsservice/internal/service"
	"encoding/json"
	"net/http"
	"strconv"
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

func (h *POIHandler) FindNearestPOI(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	query := r.URL.Query()

	latStr := query.Get("latitude")
	lngStr := query.Get("longitude")

	if latStr == "" || lngStr == "" {
		h.writeError(w, http.StatusBadRequest, "Parameters 'lat' and 'lng' are required")
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

	poi, err := h.poiService.FindNearestPOI(lat, lng)
	if err != nil {
		h.writeError(w, http.StatusNotFound, err.Error())
		return
	}

	h.writeJSON(w, http.StatusOK, Response{Data: poi})
}

func (h *POIHandler) HealthCheck(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	h.writeJSON(w, http.StatusOK, Response{Data: map[string]string{
		"status": "OK",
	}})
}
