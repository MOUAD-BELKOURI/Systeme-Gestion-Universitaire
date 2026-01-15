package com.universite.service.impl;

import com.universite.exception.ResourceNotFoundException;
import com.universite.model.CourseModule;
import com.universite.model.Etudiant;
import com.universite.model.Filiere;
import com.universite.repository.EtudiantRepository;
import com.universite.repository.ModuleRepository;
import com.universite.service.EtudiantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EtudiantServiceImpl implements EtudiantService {

    private final EtudiantRepository etudiantRepository;
    private final ModuleRepository moduleRepository;

    @Override
    public List<Etudiant> getAllEtudiants() {
        return etudiantRepository.findAll();
    }

    @Override
    public Etudiant getEtudiantById(Long id) {
        // Utilisez la méthode avec fetch pour charger les modules
        return etudiantRepository.findByIdWithModules(id)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", id));
    }

    @Override
    public Etudiant updateEtudiant(Long id, Etudiant etudiantDetails) {
        Etudiant etudiant = getEtudiantById(id);

        etudiant.setNom(etudiantDetails.getNom());
        etudiant.setPrenom(etudiantDetails.getPrenom());
        etudiant.setEmail(etudiantDetails.getEmail());
        etudiant.setFiliere(etudiantDetails.getFiliere());
        etudiant.setNiveau(etudiantDetails.getNiveau());
        etudiant.setGroupe(etudiantDetails.getGroupe());
        etudiant.setCompetences(etudiantDetails.getCompetences());

        return etudiantRepository.save(etudiant);
    }

    @Override
    public void deleteEtudiant(Long id) {
        Etudiant etudiant = getEtudiantById(id);
        etudiantRepository.delete(etudiant);
    }

    @Override
    public List<Etudiant> getEtudiantsByFiliere(Integer filiereId) {
        return etudiantRepository.findByFiliereId(filiereId);
    }

    @Override
    public List<Etudiant> getEtudiantsByGroupe(String groupe) {
        return etudiantRepository.findByGroupe(groupe);
    }

    @Override
    public List<Etudiant> getEtudiantsByFiliereAndGroupe(Integer filiereId, String groupe) {
        return etudiantRepository.findByFiliereIdAndGroupe(filiereId, groupe);
    }

    @Override
    public List<Etudiant> getEtudiantsByFiliereAndNiveau(Integer filiereId, String niveau) {
        return etudiantRepository.findByFiliereAndNiveau(filiereId, niveau);
    }

    @Override
    @Transactional
    public void affecterModule(Long etudiantId, Long moduleId) {
        Etudiant etudiant = getEtudiantById(etudiantId);
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        if (etudiant.getFiliere() == null) {
            throw new IllegalArgumentException("L'étudiant n'a pas de filière assignée");
        }

        if (module.getFiliere() == null || !module.getFiliere().getId().equals(etudiant.getFiliere().getId())) {
            throw new IllegalArgumentException("Ce module n'est pas de votre filière");
        }

        etudiant.getModules().add(module);
        etudiantRepository.save(etudiant);
    }

    @Override
    @Transactional
    public void desaffecterModule(Long etudiantId, Long moduleId) {
        Etudiant etudiant = getEtudiantById(etudiantId);
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        etudiant.getModules().remove(module);
        etudiantRepository.save(etudiant);
    }

    @Override
    public Set<CourseModule> getModulesEtudiant(Long etudiantId) {
        // Utilisez la nouvelle méthode du repository
        return etudiantRepository.findModulesByEtudiantId(etudiantId);
    }

    @Override
    public List<CourseModule> getModulesDisponiblesPourEtudiant(Long etudiantId) {
        Etudiant etudiant = getEtudiantById(etudiantId);
        Filiere filiere = etudiant.getFiliere();

        if (filiere == null) {
            return new ArrayList<>();
        }

        String niveau = etudiant.getNiveau();
        String semestre = getSemestreActuelPourNiveau(niveau);

        // Récupérer les modules du semestre actuel pour la filière
        List<CourseModule> modulesFiliere = moduleRepository.findByFiliereIdAndSemestre(filiere.getId(), semestre);

        // Exclure les modules déjà inscrits
        Set<CourseModule> modulesInscrits = etudiant.getModules();
        return modulesFiliere.stream()
                .filter(module -> !modulesInscrits.contains(module))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseModule> getModulesParFiliereEtNiveau(Long etudiantId) {
        Etudiant etudiant = getEtudiantById(etudiantId);
        Filiere filiere = etudiant.getFiliere();

        if (filiere == null) {
            return new ArrayList<>();
        }

        String niveau = etudiant.getNiveau();
        String semestre = getSemestreActuelPourNiveau(niveau);

        return moduleRepository.findByFiliereIdAndSemestre(filiere.getId(), semestre);
    }

    @Override
    public String getSemestreActuelPourNiveau(String niveau) {
        Map<String, String> niveauToSemestre = new HashMap<>();
        niveauToSemestre.put("L1", "S1");
        niveauToSemestre.put("L2", "S3");
        niveauToSemestre.put("L3", "S5");
        niveauToSemestre.put("M1", "S7");
        niveauToSemestre.put("M2", "S9");

        return niveauToSemestre.getOrDefault(niveau.toUpperCase(), "S1");
    }

    // Cette méthode est maintenant privée car utilisée seulement en interne
    private boolean estModuleCompatible(String semestreModule, String niveauEtudiant) {
        Map<String, List<String>> compatibilite = new HashMap<>();
        compatibilite.put("L1", Arrays.asList("S1", "S2"));
        compatibilite.put("L2", Arrays.asList("S3", "S4"));
        compatibilite.put("L3", Arrays.asList("S5", "S6"));
        compatibilite.put("M1", Arrays.asList("S7", "S8"));
        compatibilite.put("M2", Arrays.asList("S9", "S10"));

        List<String> semestresCompatibles = compatibilite.get(niveauEtudiant.toUpperCase());
        return semestresCompatibles != null && semestresCompatibles.contains(semestreModule.toUpperCase());
    }
}