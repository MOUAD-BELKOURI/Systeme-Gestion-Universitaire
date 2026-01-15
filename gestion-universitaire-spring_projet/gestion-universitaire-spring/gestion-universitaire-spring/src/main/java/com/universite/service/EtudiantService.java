package com.universite.service;

import com.universite.model.CourseModule;
import com.universite.model.Etudiant;

import java.util.List;
import java.util.Set;

public interface EtudiantService {
    List<Etudiant> getAllEtudiants();
    Etudiant getEtudiantById(Long id);
    Etudiant updateEtudiant(Long id, Etudiant etudiantDetails);
    void deleteEtudiant(Long id);

    List<Etudiant> getEtudiantsByFiliere(Integer filiereId);
    List<Etudiant> getEtudiantsByGroupe(String groupe);
    List<Etudiant> getEtudiantsByFiliereAndGroupe(Integer filiereId, String groupe);
    List<Etudiant> getEtudiantsByFiliereAndNiveau(Integer filiereId, String niveau);

    void affecterModule(Long etudiantId, Long moduleId);
    void desaffecterModule(Long etudiantId, Long moduleId);
    Set<CourseModule> getModulesEtudiant(Long etudiantId);

    // Nouvelles méthodes
    List<CourseModule> getModulesDisponiblesPourEtudiant(Long etudiantId);
    List<CourseModule> getModulesParFiliereEtNiveau(Long etudiantId);
    String getSemestreActuelPourNiveau(String niveau);
}