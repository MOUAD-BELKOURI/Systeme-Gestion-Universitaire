package com.universite.service.impl;

import com.universite.dto.SeanceDTO;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.Seance;
import com.universite.repository.*;
import com.universite.service.EmploiTempsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmploiTempsServiceImpl implements EmploiTempsService {

    private final SeanceRepository seanceRepository;
    private final ModuleRepository moduleRepository;
    private final EnseignantRepository enseignantRepository;
    private final SalleRepository salleRepository;

    @Override
    @Transactional
    public Seance createSeance(SeanceDTO seanceDTO) {
        // Validation des données
        if (seanceDTO.getDateHeure() == null) {
            throw new IllegalArgumentException("La date et heure sont obligatoires");
        }
        if (seanceDTO.getDuree() == null || seanceDTO.getDuree() <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }
        if (seanceDTO.getGroupe() == null || seanceDTO.getGroupe().trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe est obligatoire");
        }

        // Vérifier les conflits
        LocalDateTime startTime = seanceDTO.getDateHeure();
        LocalDateTime endTime = startTime.plusMinutes(seanceDTO.getDuree());

        // Vérifier conflit salle
        if (verifierConflitSalle(seanceDTO.getSalleId(), startTime, endTime)) {
            throw new IllegalArgumentException("La salle est déjà occupée à cet horaire");
        }

        // Vérifier conflit enseignant
        if (verifierConflitEnseignant(seanceDTO.getEnseignantId(), startTime, endTime)) {
            throw new IllegalArgumentException("L'enseignant est déjà occupé à cet horaire");
        }

        // Vérifier conflit groupe
        if (verifierConflitGroupe(seanceDTO.getGroupe(), startTime, endTime)) {
            throw new IllegalArgumentException("Le groupe est déjà occupé à cet horaire");
        }

        // Créer la séance
        Seance seance = new Seance();
        seance.setModule(moduleRepository.findById(seanceDTO.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", seanceDTO.getModuleId())));
        seance.setEnseignant(enseignantRepository.findById(seanceDTO.getEnseignantId())
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", seanceDTO.getEnseignantId())));
        seance.setGroupe(seanceDTO.getGroupe());
        seance.setSalle(salleRepository.findById(seanceDTO.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle", "id", seanceDTO.getSalleId())));
        seance.setDateHeure(seanceDTO.getDateHeure());
        seance.setDuree(seanceDTO.getDuree());
        seance.setTypeSeance(seanceDTO.getTypeSeance());

        return seanceRepository.save(seance);
    }

    @Override
    @Transactional
    public Seance updateSeance(Long id, SeanceDTO seanceDTO) {
        Seance seance = getSeanceById(id);

        // Validation des données
        if (seanceDTO.getDateHeure() == null) {
            throw new IllegalArgumentException("La date et heure sont obligatoires");
        }
        if (seanceDTO.getDuree() == null || seanceDTO.getDuree() <= 0) {
            throw new IllegalArgumentException("La durée doit être positive");
        }
        if (seanceDTO.getGroupe() == null || seanceDTO.getGroupe().trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe est obligatoire");
        }

        // Vérifier les conflits (en excluant la séance actuelle)
        LocalDateTime startTime = seanceDTO.getDateHeure();
        LocalDateTime endTime = startTime.plusMinutes(seanceDTO.getDuree());

        final Long seanceId = id;

        // Vérifier conflit salle
        List<Seance> conflitsSalle = seanceRepository.findConflitsSalle(
                seanceDTO.getSalleId(), startTime, endTime);
        conflitsSalle.removeIf(s -> s.getId().equals(seanceId));
        if (!conflitsSalle.isEmpty()) {
            throw new IllegalArgumentException("La salle est déjà occupée à cet horaire");
        }

        // Vérifier conflit enseignant
        List<Seance> conflitsEnseignant = seanceRepository.findConflitsEnseignant(
                seanceDTO.getEnseignantId(), startTime, endTime);
        conflitsEnseignant.removeIf(s -> s.getId().equals(seanceId));
        if (!conflitsEnseignant.isEmpty()) {
            throw new IllegalArgumentException("L'enseignant est déjà occupé à cet horaire");
        }

        // Vérifier conflit groupe
        List<Seance> conflitsGroupe = seanceRepository.findConflitsGroupe(
                seanceDTO.getGroupe(), startTime, endTime);
        conflitsGroupe.removeIf(s -> s.getId().equals(seanceId));
        if (!conflitsGroupe.isEmpty()) {
            throw new IllegalArgumentException("Le groupe est déjà occupé à cet horaire");
        }

        // Mettre à jour la séance
        seance.setModule(moduleRepository.findById(seanceDTO.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", seanceDTO.getModuleId())));
        seance.setEnseignant(enseignantRepository.findById(seanceDTO.getEnseignantId())
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant", "id", seanceDTO.getEnseignantId())));
        seance.setGroupe(seanceDTO.getGroupe());
        seance.setSalle(salleRepository.findById(seanceDTO.getSalleId())
                .orElseThrow(() -> new ResourceNotFoundException("Salle", "id", seanceDTO.getSalleId())));
        seance.setDateHeure(seanceDTO.getDateHeure());
        seance.setDuree(seanceDTO.getDuree());
        seance.setTypeSeance(seanceDTO.getTypeSeance());

        return seanceRepository.save(seance);
    }

    @Override
    @Transactional
    public void deleteSeance(Long id) {
        Seance seance = getSeanceById(id);
        seanceRepository.delete(seance);
    }

    @Override
    @Transactional(readOnly = true)
    public Seance getSeanceById(Long id) {
        return seanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Séance", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getAllSeances() {
        return seanceRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByEnseignant(Long enseignantId) {
        return seanceRepository.findByEnseignantId(enseignantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByGroupe(String groupe) {
        if (groupe == null || groupe.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe ne peut pas être vide");
        }
        return seanceRepository.findByGroupe(groupe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesBySalle(Long salleId) {
        return seanceRepository.findBySalleId(salleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByModule(Long moduleId) {
        return seanceRepository.findByModuleId(moduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifierConflitSalle(Long salleId, LocalDateTime startTime, LocalDateTime endTime) {
        if (salleId == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null");
        }

        List<Seance> conflits = seanceRepository.findConflitsSalle(salleId, startTime, endTime);
        return !conflits.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifierConflitEnseignant(Long enseignantId, LocalDateTime startTime, LocalDateTime endTime) {
        if (enseignantId == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null");
        }

        List<Seance> conflits = seanceRepository.findConflitsEnseignant(enseignantId, startTime, endTime);
        return !conflits.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifierConflitGroupe(String groupe, LocalDateTime startTime, LocalDateTime endTime) {
        if (groupe == null || groupe.trim().isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null ou vides");
        }

        List<Seance> conflits = seanceRepository.findConflitsGroupe(groupe, startTime, endTime);
        return !conflits.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesPourSemaine(LocalDateTime startOfWeek) {
        if (startOfWeek == null) {
            startOfWeek = LocalDateTime.now();
        }

        final LocalDateTime startWeek = startOfWeek;
        final LocalDateTime endOfWeek = startWeek.plusDays(7);

        List<Seance> toutesSeances = seanceRepository.findAllWithDetails();

        return toutesSeances.stream()
                .filter(s -> !s.getDateHeure().isBefore(startWeek) && s.getDateHeure().isBefore(endOfWeek))
                .sorted((s1, s2) -> {
                    int dayCompare = s1.getDateHeure().getDayOfWeek().compareTo(s2.getDateHeure().getDayOfWeek());
                    if (dayCompare != 0) {
                        return dayCompare;
                    }
                    return s1.getDateHeure().toLocalTime().compareTo(s2.getDateHeure().toLocalTime());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByGroupeAndFiliere(String groupe, Integer filiereId) {
        if (groupe == null || groupe.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe ne peut pas être vide");
        }
        if (filiereId == null) {
            throw new IllegalArgumentException("L'ID de la filière ne peut pas être null");
        }

        return seanceRepository.findByGroupeAndFiliere(groupe, filiereId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByEtudiant(Long etudiantId) {
        if (etudiantId == null) {
            throw new IllegalArgumentException("L'ID de l'étudiant ne peut pas être null");
        }

        return seanceRepository.findSeancesByEtudiantId(etudiantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesAujourdhui() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Seance> toutesSeances = seanceRepository.findAllWithDetails();

        return toutesSeances.stream()
                .filter(s -> !s.getDateHeure().isBefore(startOfDay) && s.getDateHeure().isBefore(endOfDay))
                .sorted((s1, s2) -> s1.getDateHeure().compareTo(s2.getDateHeure()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesProchaines() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime dansUneSemaine = maintenant.plusDays(7);

        List<Seance> toutesSeances = seanceRepository.findAllWithDetails();

        return toutesSeances.stream()
                .filter(s -> s.getDateHeure().isAfter(maintenant) && s.getDateHeure().isBefore(dansUneSemaine))
                .sorted((s1, s2) -> s1.getDateHeure().compareTo(s2.getDateHeure()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByGroupeFiliereAndDate(String groupe, Integer filiereId, LocalDateTime date) {
        if (groupe == null || groupe.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe ne peut pas être vide");
        }
        if (filiereId == null) {
            throw new IllegalArgumentException("L'ID de la filière ne peut pas être null");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date ne peut pas être null");
        }

        return seanceRepository.findByGroupeFiliereAndDate(groupe, filiereId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSeancesByGroupe(String groupe) {
        if (groupe == null || groupe.trim().isEmpty()) {
            return 0;
        }

        List<Seance> seances = seanceRepository.findByGroupe(groupe);
        return seances != null ? seances.size() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSeancesByEnseignant(Long enseignantId) {
        if (enseignantId == null) {
            return 0;
        }

        List<Seance> seances = seanceRepository.findByEnseignantId(enseignantId);
        return seances != null ? seances.size() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public long countSeancesByModule(Long moduleId) {
        if (moduleId == null) {
            return 0;
        }

        List<Seance> seances = seanceRepository.findByModuleId(moduleId);
        return seances != null ? seances.size() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> rechercherSeances(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            return getAllSeances();
        }

        final String rechercheLower = recherche.toLowerCase().trim();
        List<Seance> toutesSeances = seanceRepository.findAllWithDetails();

        return toutesSeances.stream()
                .filter(s ->
                        (s.getModule() != null && s.getModule().getNom() != null &&
                                s.getModule().getNom().toLowerCase().contains(rechercheLower)) ||
                                (s.getGroupe() != null && s.getGroupe().toLowerCase().contains(rechercheLower)) ||
                                (s.getTypeSeance() != null && s.getTypeSeance().toLowerCase().contains(rechercheLower)) ||
                                (s.getSalle() != null && s.getSalle().getNom() != null &&
                                        s.getSalle().getNom().toLowerCase().contains(rechercheLower)) ||
                                (s.getEnseignant() != null && s.getEnseignant().getNom() != null &&
                                        s.getEnseignant().getNom().toLowerCase().contains(rechercheLower)) ||
                                (s.getEnseignant() != null && s.getEnseignant().getPrenom() != null &&
                                        s.getEnseignant().getPrenom().toLowerCase().contains(rechercheLower))
                )
                .sorted((s1, s2) -> s1.getDateHeure().compareTo(s2.getDateHeure()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeSeanceAvecModuleEtGroupe(Long moduleId, String groupe, LocalDateTime dateHeure) {
        if (moduleId == null || groupe == null || groupe.trim().isEmpty() || dateHeure == null) {
            return false;
        }

        final LocalDateTime date = dateHeure.toLocalDate().atStartOfDay();
        List<Seance> seances = seanceRepository.findByModuleId(moduleId);

        return seances.stream()
                .anyMatch(s -> s.getGroupe().equals(groupe) &&
                        s.getDateHeure().toLocalDate().equals(date.toLocalDate()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesEntreDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être avant la date de fin");
        }

        final LocalDateTime start = startDate;
        final LocalDateTime end = endDate;

        List<Seance> toutesSeances = seanceRepository.findAllWithDetails();

        return toutesSeances.stream()
                .filter(s -> !s.getDateHeure().isBefore(start) && s.getDateHeure().isBefore(end))
                .sorted((s1, s2) -> s1.getDateHeure().compareTo(s2.getDateHeure()))
                .toList();
    }

    // NOUVELLES MÉTHODES AJOUTÉES POUR RÉSOUDRE LE PROBLÈME

    @Override
    @Transactional(readOnly = true)
    public List<Seance> getSeancesByGroupeWithDetails(String groupe) {
        if (groupe == null || groupe.trim().isEmpty()) {
            throw new IllegalArgumentException("Le groupe ne peut pas être vide");
        }
        return seanceRepository.findByGroupeWithDetails(groupe);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDebugInfo(String groupe) {
        Map<String, Object> debugInfo = new HashMap<>();

        // Récupérer toutes les séances
        List<Seance> toutesSeances = getAllSeances();
        debugInfo.put("totalSeances", toutesSeances.size());

        // Séances par groupe
        List<Seance> seancesGroupe = getSeancesByGroupe(groupe);
        debugInfo.put("seancesGroupe", seancesGroupe.size());

        // Séances par groupe avec détails
        List<Seance> seancesGroupeDetails = getSeancesByGroupeWithDetails(groupe);
        debugInfo.put("seancesGroupeDetails", seancesGroupeDetails.size());

        // Détails des séances du groupe
        List<Map<String, Object>> details = seancesGroupeDetails.stream()
                .map(s -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", s.getId());
                    detail.put("module", s.getModule() != null ? s.getModule().getNom() : "null");
                    detail.put("moduleId", s.getModule() != null ? s.getModule().getId() : "null");
                    detail.put("groupe", s.getGroupe());
                    detail.put("filiere", s.getModule() != null && s.getModule().getFiliere() != null ?
                            s.getModule().getFiliere().getNom() + " (ID: " + s.getModule().getFiliere().getId() + ")" : "null");
                    detail.put("date", s.getDateHeure());
                    detail.put("type", s.getTypeSeance());
                    detail.put("enseignant", s.getEnseignant() != null ?
                            s.getEnseignant().getNom() + " " + s.getEnseignant().getPrenom() : "null");
                    detail.put("salle", s.getSalle() != null ? s.getSalle().getNom() : "null");
                    return detail;
                })
                .collect(Collectors.toList());

        debugInfo.put("details", details);

        // Statistiques par filière
        Map<String, Long> statsFiliere = seancesGroupeDetails.stream()
                .filter(s -> s.getModule() != null && s.getModule().getFiliere() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getModule().getFiliere().getNom(),
                        Collectors.counting()
                ));
        debugInfo.put("statsParFiliere", statsFiliere);

        // Statistiques par type de séance
        Map<String, Long> statsType = seancesGroupeDetails.stream()
                .filter(s -> s.getTypeSeance() != null)
                .collect(Collectors.groupingBy(
                        Seance::getTypeSeance,
                        Collectors.counting()
                ));
        debugInfo.put("statsParType", statsType);

        // Séances pour aujourd'hui
        List<Seance> seancesAujourdhui = seancesGroupeDetails.stream()
                .filter(s -> s.getDateHeure().toLocalDate().equals(LocalDate.now()))
                .collect(Collectors.toList());
        debugInfo.put("seancesAujourdhui", seancesAujourdhui.size());

        // Séances à venir (prochaines 7 jours)
        List<Seance> seancesProchaines = seancesGroupeDetails.stream()
                .filter(s -> s.getDateHeure().isAfter(LocalDateTime.now()) &&
                        s.getDateHeure().isBefore(LocalDateTime.now().plusDays(7)))
                .collect(Collectors.toList());
        debugInfo.put("seancesProchaines", seancesProchaines.size());

        return debugInfo;
    }
}