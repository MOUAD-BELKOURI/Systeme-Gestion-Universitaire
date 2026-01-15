package com.universite.service.impl;

import com.universite.dto.ModuleDTO;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.CourseModule;
import com.universite.model.Filiere;
import com.universite.repository.EnseignantRepository;
import com.universite.repository.FiliereRepository;
import com.universite.repository.ModuleRepository;
import com.universite.service.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final FiliereRepository filiereRepository;
    private final EnseignantRepository enseignantRepository;

    @Override
    public List<CourseModule> getAllModules() {
        return moduleRepository.findAll();
    }

    @Override
    public CourseModule getModuleById(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));
    }

    @Override
    @Transactional
    public CourseModule createModule(ModuleDTO moduleDTO) {
        Filiere filiere = filiereRepository.findById(moduleDTO.getFiliereId())
                .orElseThrow(() -> new ResourceNotFoundException("Filière", "id", moduleDTO.getFiliereId()));

        CourseModule module = new CourseModule();
        module.setNom(moduleDTO.getNom());
        module.setFiliere(filiere);
        module.setSemestre(moduleDTO.getSemestre());
        module.setVolumeHoraire(moduleDTO.getVolumeHoraire());
        module.setCompetencesRequises(moduleDTO.getCompetencesRequises());

        // Désactiver temporairement le niveau jusqu'à ce que le champ existe
        // module.setNiveau(moduleDTO.getNiveau());

        if (moduleDTO.getEnseignantId() != null) {
            module.setEnseignant(enseignantRepository.findById(moduleDTO.getEnseignantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", moduleDTO.getEnseignantId())));
        }

        return moduleRepository.save(module);
    }

    @Override
    @Transactional
    public CourseModule updateModule(Long id, ModuleDTO moduleDTO) {
        CourseModule module = getModuleById(id);
        Filiere filiere = filiereRepository.findById(moduleDTO.getFiliereId())
                .orElseThrow(() -> new ResourceNotFoundException("Filière", "id", moduleDTO.getFiliereId()));

        module.setNom(moduleDTO.getNom());
        module.setFiliere(filiere);
        module.setSemestre(moduleDTO.getSemestre());
        module.setVolumeHoraire(moduleDTO.getVolumeHoraire());
        module.setCompetencesRequises(moduleDTO.getCompetencesRequises());

        // Désactiver temporairement le niveau jusqu'à ce que le champ existe
        // module.setNiveau(moduleDTO.getNiveau());

        if (moduleDTO.getEnseignantId() != null) {
            module.setEnseignant(enseignantRepository.findById(moduleDTO.getEnseignantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", moduleDTO.getEnseignantId())));
        } else {
            module.setEnseignant(null);
        }

        return moduleRepository.save(module);
    }

    @Override
    public void deleteModule(Long id) {
        CourseModule module = getModuleById(id);
        moduleRepository.delete(module);
    }

    @Override
    public List<CourseModule> getModulesByFiliere(Integer filiereId) {
        return moduleRepository.findByFiliereId(filiereId);
    }

    @Override
    public List<CourseModule> getModulesByEnseignant(Long enseignantId) {
        return moduleRepository.findByEnseignantId(enseignantId);
    }

    @Override
    public List<CourseModule> getModulesSansEnseignant() {
        return moduleRepository.findModulesSansEnseignant();
    }

    @Override
    @Transactional
    public void affecterEnseignant(Long moduleId, Long enseignantId) {
        CourseModule module = getModuleById(moduleId);
        module.setEnseignant(enseignantRepository.findById(enseignantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", enseignantId)));
        moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void desaffecterEnseignant(Long moduleId) {
        CourseModule module = getModuleById(moduleId);
        module.setEnseignant(null);
        moduleRepository.save(module);
    }

    @Override
    public List<CourseModule> getModulesByFiliereAndSemestre(Integer filiereId, String semestre) {
        return moduleRepository.findByFiliereIdAndSemestre(filiereId, semestre);
    }

    @Override
    public List<CourseModule> getModulesByFiliereAndNiveau(Integer filiereId, String niveau) {
        try {
            // Essayer d'abord la méthode qui utilise le champ niveau
            return moduleRepository.findByFiliereIdAndNiveau(filiereId, niveau);
        } catch (Exception e) {
            // Si échec, utiliser la logique par semestre
            return getModulesByFiliereAndNiveauViaSemestre(filiereId, niveau);
        }
    }

    private List<CourseModule> getModulesByFiliereAndNiveauViaSemestre(Integer filiereId, String niveau) {
        // Déterminer les semestres compatibles avec le niveau
        List<String> semestresCompatibles = getSemestresCompatiblesPourNiveau(niveau);

        List<CourseModule> modules = new ArrayList<>();
        for (String semestre : semestresCompatibles) {
            List<CourseModule> modulesSemestre = moduleRepository.findByFiliereIdAndSemestre(filiereId, semestre);
            if (modulesSemestre != null) {
                modules.addAll(modulesSemestre);
            }
        }

        return modules;
    }

    private List<String> getSemestresCompatiblesPourNiveau(String niveau) {
        Map<String, List<String>> compatibilite = new HashMap<>();
        compatibilite.put("L1", List.of("S1", "S2"));
        compatibilite.put("L2", List.of("S3", "S4"));
        compatibilite.put("L3", List.of("S5", "S6"));
        compatibilite.put("M1", List.of("S7", "S8"));
        compatibilite.put("M2", List.of("S9", "S10"));

        return compatibilite.getOrDefault(niveau != null ? niveau.toUpperCase() : "L1", List.of("S1"));
    }
}