package service

import (
	"aigpsservice/internal/domain"
	"fmt"
)

type POIService struct {
	poiRepo domain.POIRepository
}

func NewPOIService(poiRepo domain.POIRepository) *POIService {
	return &POIService{
		poiRepo: poiRepo,
	}
}

func (s *POIService) FindNearestPOI(latitude, longitude float64) (*domain.PointOfInterest, error) {
	if latitude < -90 || latitude > 90 {
		return nil, fmt.Errorf("invalid latitude: must be between -90 and 90")
	}
	if longitude < -180 || longitude > 180 {
		return nil, fmt.Errorf("invalid longitude: must be between -180 and 180")
	}

	return s.poiRepo.FindNearestPOI(latitude, longitude)
}
