package com.universite.service;

import com.universite.dto.FiliereDTO;
import com.universite.model.Filiere;
import com.universite.repository.FiliereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service  // ← CETTE ANNOTATION EST ESSENTIELLE
@RequiredArgsConstructor
public class FiliereService {

    private final FiliereRepository filiereRepository;

    public List<Filiere> getAllFilieres() {
        return filiereRepository.findAll();
    }

    public Filiere getFiliereById(Integer id) {
        return filiereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Filière non trouvée avec l'ID: " + id));
    }

    public Filiere createFiliere(FiliereDTO filiereDTO) {
        if (filiereRepository.existsByNom(filiereDTO.getNom())) {
            throw new RuntimeException("Une filière avec ce nom existe déjà");
        }

        Filiere filiere = new Filiere();
        filiere.setNom(filiereDTO.getNom());
        filiere.setDescription(filiereDTO.getDescription());

        return filiereRepository.save(filiere);
    }

    public Filiere updateFiliere(Integer id, FiliereDTO filiereDTO) {
        Filiere filiere = getFiliereById(id);

        // Vérifier si le nom existe déjà pour une autre filière
        if (!filiere.getNom().equals(filiereDTO.getNom())
                && filiereRepository.existsByNom(filiereDTO.getNom())) {
            throw new RuntimeException("Une filière avec ce nom existe déjà");
        }

        filiere.setNom(filiereDTO.getNom());
        filiere.setDescription(filiereDTO.getDescription());

        return filiereRepository.save(filiere);
    }

    public void deleteFiliere(Integer id) {
        Filiere filiere = getFiliereById(id);
        filiereRepository.delete(filiere);
    }

    public boolean existsByNom(String nom) {
        return filiereRepository.existsByNom(nom);
    }
}