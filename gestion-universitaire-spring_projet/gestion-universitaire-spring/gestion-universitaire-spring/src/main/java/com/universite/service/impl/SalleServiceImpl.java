package com.universite.service.impl;

import com.universite.exception.ResourceNotFoundException;
import com.universite.model.Salle;
import com.universite.repository.SalleRepository;
import com.universite.repository.SeanceRepository;
import com.universite.service.SalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalleServiceImpl implements SalleService {

    private final SalleRepository salleRepository;
    private final SeanceRepository seanceRepository;

    @Override
    public List<Salle> getAllSalles() {
        return salleRepository.findAll();
    }

    @Override
    public Salle getSalleById(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle", "id", id));
    }

    @Override
    @Transactional
    public Salle createSalle(Salle salle) {
        if (existeSalleAvecNom(salle.getNom())) {
            throw new IllegalArgumentException("Une salle avec ce nom existe déjà");
        }
        return salleRepository.save(salle);
    }

    @Override
    @Transactional
    public Salle updateSalle(Long id, Salle salleDetails) {
        Salle salle = getSalleById(id);

        // Vérifier si le nouveau nom est déjà utilisé
        if (!salle.getNom().equals(salleDetails.getNom()) &&
                existeSalleAvecNom(salleDetails.getNom())) {
            throw new IllegalArgumentException("Une salle avec ce nom existe déjà");
        }

        salle.setNom(salleDetails.getNom());
        salle.setCapacite(salleDetails.getCapacite());
        salle.setTypeSalle(salleDetails.getTypeSalle());
        salle.setDisponible(salleDetails.getDisponible());

        return salleRepository.save(salle);
    }

    @Override
    @Transactional
    public void deleteSalle(Long id) {
        Salle salle = getSalleById(id);

        // Vérifier si la salle est utilisée
        if (getNombreSeances(id) > 0) {
            throw new IllegalStateException("Impossible de supprimer une salle utilisée dans des séances");
        }

        salleRepository.delete(salle);
    }

    @Override
    public List<Salle> getSallesDisponibles() {
        return salleRepository.findByDisponibleTrue();
    }

    @Override
    public List<Salle> getSallesDisponibles(LocalDateTime startTime, LocalDateTime endTime) {
        return salleRepository.findSallesDisponibles(startTime, endTime);
    }

    @Override
    public boolean isSalleDisponible(Long salleId, LocalDateTime startTime, LocalDateTime endTime) {
        Salle salle = getSalleById(salleId);
        if (!salle.getDisponible()) {
            return false;
        }

        // Vérifier les conflits avec les séances existantes
        List<com.universite.model.Seance> conflits = seanceRepository.findConflitsSalle(
                salleId, startTime, endTime);
        return conflits.isEmpty();
    }

    @Override
    public Integer getNombreSeances(Long salleId) {
        return seanceRepository.findBySalleId(salleId).size();
    }

    @Override
    public boolean existeSalleAvecNom(String nom) {
        return salleRepository.findAll().stream()
                .anyMatch(s -> s.getNom().equalsIgnoreCase(nom));
    }

    @Override
    public List<Salle> searchSalles(String keyword) {
        return salleRepository.findAll().stream()
                .filter(s -> s.getNom().toLowerCase().contains(keyword.toLowerCase()) ||
                        (s.getTypeSalle() != null &&
                                s.getTypeSalle().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }
}