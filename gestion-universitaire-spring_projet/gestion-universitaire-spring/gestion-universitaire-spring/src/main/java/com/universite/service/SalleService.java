package com.universite.service;

import com.universite.model.Salle;

import java.time.LocalDateTime;
import java.util.List;

public interface SalleService {
    List<Salle> getAllSalles();
    Salle getSalleById(Long id);
    Salle createSalle(Salle salle);
    Salle updateSalle(Long id, Salle salle);
    void deleteSalle(Long id);
    List<Salle> getSallesDisponibles();
    List<Salle> getSallesDisponibles(LocalDateTime startTime, LocalDateTime endTime);
    boolean isSalleDisponible(Long salleId, LocalDateTime startTime, LocalDateTime endTime);
    Integer getNombreSeances(Long salleId);
    boolean existeSalleAvecNom(String nom);
    List<Salle> searchSalles(String keyword);
}