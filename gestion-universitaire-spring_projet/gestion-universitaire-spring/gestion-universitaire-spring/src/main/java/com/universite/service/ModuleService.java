package com.universite.service;

import com.universite.dto.ModuleDTO;
import com.universite.model.CourseModule;

import java.util.List;

public interface ModuleService {
    List<CourseModule> getAllModules();
    CourseModule getModuleById(Long id);
    CourseModule createModule(ModuleDTO moduleDTO);
    CourseModule updateModule(Long id, ModuleDTO moduleDTO);
    void deleteModule(Long id);

    List<CourseModule> getModulesByFiliere(Integer filiereId);
    List<CourseModule> getModulesByEnseignant(Long enseignantId);
    List<CourseModule> getModulesSansEnseignant();

    void affecterEnseignant(Long moduleId, Long enseignantId);
    void desaffecterEnseignant(Long moduleId);

    // Nouvelles méthodes
    List<CourseModule> getModulesByFiliereAndNiveau(Integer filiereId, String niveau);
    List<CourseModule> getModulesByFiliereAndSemestre(Integer filiereId, String semestre);
}