package com.universite.service;

import com.universite.dto.SeanceDTO;
import com.universite.model.Seance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EmploiTempsService {

    // Méthodes existantes...
    Seance createSeance(SeanceDTO seanceDTO);
    Seance updateSeance(Long id, SeanceDTO seanceDTO);
    void deleteSeance(Long id);
    Seance getSeanceById(Long id);
    List<Seance> getAllSeances();
    List<Seance> getSeancesByEnseignant(Long enseignantId);
    List<Seance> getSeancesByGroupe(String groupe);
    List<Seance> getSeancesBySalle(Long salleId);
    List<Seance> getSeancesByModule(Long moduleId);
    boolean verifierConflitSalle(Long salleId, LocalDateTime startTime, LocalDateTime endTime);
    boolean verifierConflitEnseignant(Long enseignantId, LocalDateTime startTime, LocalDateTime endTime);
    boolean verifierConflitGroupe(String groupe, LocalDateTime startTime, LocalDateTime endTime);
    List<Seance> getSeancesPourSemaine(LocalDateTime startOfWeek);
    List<Seance> getSeancesByGroupeAndFiliere(String groupe, Integer filiereId);
    List<Seance> getSeancesByEtudiant(Long etudiantId);
    List<Seance> getSeancesAujourdhui();
    List<Seance> getSeancesProchaines();
    List<Seance> getSeancesByGroupeFiliereAndDate(String groupe, Integer filiereId, LocalDateTime date);
    long countSeancesByGroupe(String groupe);
    long countSeancesByEnseignant(Long enseignantId);
    long countSeancesByModule(Long moduleId);
    List<Seance> rechercherSeances(String recherche);
    boolean existeSeanceAvecModuleEtGroupe(Long moduleId, String groupe, LocalDateTime dateHeure);
    List<Seance> getSeancesEntreDates(LocalDateTime startDate, LocalDateTime endDate);

    // NOUVELLES MÉTHODES AJOUTÉES
    List<Seance> getSeancesByGroupeWithDetails(String groupe);
    Map<String, Object> getDebugInfo(String groupe);
}