package com.universite.service;

import com.universite.model.CourseModule;
import com.universite.model.Enseignant;

import java.util.List;

public interface EnseignantService {
    List<Enseignant> getAllEnseignants();
    Enseignant getEnseignantById(Long id);
    Enseignant updateEnseignant(Long id, Enseignant enseignant);
    void deleteEnseignant(Long id);
    List<CourseModule> getModulesEnseignant(Long enseignantId);
    void affecterModule(Long enseignantId, Long moduleId);
    void desaffecterModule(Long enseignantId, Long moduleId);
    List<Enseignant> getEnseignantsBySpecialite(String specialite);
    List<Enseignant> getEnseignantsByCompetence(String competence);
}