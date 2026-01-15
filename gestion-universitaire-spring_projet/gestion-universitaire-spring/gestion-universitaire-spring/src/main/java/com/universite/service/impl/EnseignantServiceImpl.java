package com.universite.service.impl;

import com.universite.exception.ResourceNotFoundException;
import com.universite.model.CourseModule;
import com.universite.model.Enseignant;
import com.universite.repository.EnseignantRepository;
import com.universite.repository.ModuleRepository;
import com.universite.service.EnseignantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnseignantServiceImpl implements EnseignantService {

    private final EnseignantRepository enseignantRepository;
    private final ModuleRepository moduleRepository;

    @Override
    public List<Enseignant> getAllEnseignants() {
        return enseignantRepository.findAll();
    }

    @Override
    public Enseignant getEnseignantById(Long id) {
        return enseignantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", id));
    }

    @Override
    public Enseignant updateEnseignant(Long id, Enseignant enseignantDetails) {
        Enseignant enseignant = getEnseignantById(id);

        enseignant.setNom(enseignantDetails.getNom());
        enseignant.setPrenom(enseignantDetails.getPrenom());
        enseignant.setEmail(enseignantDetails.getEmail());
        enseignant.setSpecialite(enseignantDetails.getSpecialite());
        enseignant.setGrade(enseignantDetails.getGrade());
        enseignant.setChargeHoraire(enseignantDetails.getChargeHoraire());
        enseignant.setCompetences(enseignantDetails.getCompetences());

        return enseignantRepository.save(enseignant);
    }

    @Override
    public void deleteEnseignant(Long id) {
        Enseignant enseignant = getEnseignantById(id);
        enseignantRepository.delete(enseignant);
    }

    @Override
    public List<CourseModule> getModulesEnseignant(Long enseignantId) {
        Enseignant enseignant = getEnseignantById(enseignantId);
        return List.copyOf(enseignant.getModules());
    }

    @Override
    @Transactional
    public void affecterModule(Long enseignantId, Long moduleId) {
        Enseignant enseignant = getEnseignantById(enseignantId);
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        module.setEnseignant(enseignant);
        moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void desaffecterModule(Long enseignantId, Long moduleId) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        if (module.getEnseignant() != null && module.getEnseignant().getId().equals(enseignantId)) {
            module.setEnseignant(null);
            moduleRepository.save(module);
        }
    }

    @Override
    public List<Enseignant> getEnseignantsBySpecialite(String specialite) {
        return enseignantRepository.findBySpecialiteContaining(specialite);
    }

    @Override
    public List<Enseignant> getEnseignantsByCompetence(String competence) {
        return enseignantRepository.findByCompetence(competence);
    }
}