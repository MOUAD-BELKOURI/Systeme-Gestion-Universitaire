package com.universite.service;

import com.universite.model.*;
import com.universite.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EtudiantService etudiantService;
    private final ModuleService moduleService;

    // Inscrire un étudiant à un module
    @Transactional
    public Inscription inscrireEtudiant(Long etudiantId, Long moduleId) {
        Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);
        CourseModule module = moduleService.getModuleById(moduleId);

        // Vérifier si l'inscription existe déjà
        if (inscriptionRepository.existsByEtudiantIdAndModuleId(etudiantId, moduleId)) {
            throw new IllegalArgumentException("L'étudiant est déjà inscrit à ce module");
        }

        // Créer l'inscription
        Inscription inscription = new Inscription(
                etudiant,
                module,
                etudiant.getFiliere(),
                getAnneeScolaireActuelle(),
                module.getSemestre(),
                etudiant.getNiveau()
        );

        // Ajouter le module à l'étudiant
        etudiant.getModules().add(module);

        return inscriptionRepository.save(inscription);
    }

    // Désinscrire un étudiant d'un module
    @Transactional
    public void desinscrireEtudiant(Long etudiantId, Long moduleId) {
        Inscription inscription = inscriptionRepository
                .findByEtudiantIdAndModuleId(etudiantId, moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Inscription non trouvée"));

        Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);
        CourseModule module = moduleService.getModuleById(moduleId);

        // Supprimer le module de l'étudiant
        etudiant.getModules().remove(module);

        inscriptionRepository.delete(inscription);
    }

    // Désinscrire par ID d'inscription
    @Transactional
    public void desinscrireEtudiantByInscriptionId(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscription non trouvée"));

        Etudiant etudiant = inscription.getEtudiant();
        CourseModule module = inscription.getModule();

        if (etudiant != null && module != null) {
            etudiant.getModules().remove(module);
        }

        inscriptionRepository.delete(inscription);
    }

    // Récupérer toutes les inscriptions d'un étudiant
    public List<Inscription> getInscriptionsByEtudiant(Long etudiantId) {
        return inscriptionRepository.findByEtudiantId(etudiantId);
    }

    // Récupérer tous les étudiants d'un module
    public List<Inscription> getInscriptionsByModule(Long moduleId) {
        return inscriptionRepository.findByModuleId(moduleId);
    }

    // Récupérer les étudiants par filière et niveau
    public List<Etudiant> getEtudiantsByFiliereAndNiveau(Integer filiereId, String niveau) {
        List<Inscription> inscriptions = inscriptionRepository
                .findByFiliereIdAndNiveau(filiereId, niveau);

        return inscriptions.stream()
                .map(Inscription::getEtudiant)
                .distinct()
                .collect(Collectors.toList());
    }

    // Récupérer les modules d'un étudiant via les inscriptions
    public Set<CourseModule> getModulesByEtudiant(Long etudiantId) {
        List<Inscription> inscriptions = getInscriptionsByEtudiant(etudiantId);

        return inscriptions.stream()
                .map(Inscription::getModule)
                .collect(Collectors.toSet());
    }

    // Récupérer les inscriptions par semestre
    public List<Inscription> getInscriptionsBySemestre(String semestre) {
        return inscriptionRepository.findBySemestre(semestre);
    }

    // Mettre à jour le statut d'une inscription
    @Transactional
    public Inscription updateStatut(Long inscriptionId, String statut) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscription non trouvée"));

        inscription.setStatut(statut);
        return inscriptionRepository.save(inscription);
    }

    // Obtenir l'année scolaire actuelle
    private String getAnneeScolaireActuelle() {
        int currentYear = LocalDateTime.now().getYear();
        int nextYear = currentYear + 1;
        return currentYear + "-" + nextYear;
    }

    // Statistiques
    public long countInscriptionsByFiliere(Integer filiereId) {
        return inscriptionRepository.countByFiliereId(filiereId);
    }

    public long countInscriptionsByModule(Long moduleId) {
        return inscriptionRepository.countByModuleId(moduleId);
    }

    /**
     * Récupérer toutes les inscriptions (pour l'admin)
     */
    public List<Inscription> getAllInscriptions() {
        return inscriptionRepository.findAll();
    }

    /**
     * Vérifier si une inscription existe entre un étudiant et un module
     */
    public boolean existsByEtudiantAndModule(Long etudiantId, Long moduleId) {
        return inscriptionRepository.existsByEtudiantIdAndModuleId(etudiantId, moduleId);
    }

    /**
     * Sauvegarder une inscription (pour les opérations CRUD)
     */
    @Transactional
    public Inscription save(Inscription inscription) {
        // Vérifier si l'inscription existe déjà
        if (inscriptionRepository.existsByEtudiantIdAndModuleId(
                inscription.getEtudiant().getId(),
                inscription.getModule().getId())) {
            throw new IllegalArgumentException("L'étudiant est déjà inscrit à ce module");
        }
        return inscriptionRepository.save(inscription);
    }

    /**
     * Supprimer une inscription par son ID
     */
    @Transactional
    public void deleteInscription(Long id) {
        if (!inscriptionRepository.existsById(id)) {
            throw new IllegalArgumentException("Inscription non trouvée avec l'ID: " + id);
        }
        inscriptionRepository.deleteById(id);
    }

    /**
     * Mettre à jour le statut d'une inscription
     */
    @Transactional
    public Inscription updateInscriptionStatut(Long id, String statut) {
        Inscription inscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscription non trouvée"));
        inscription.setStatut(statut);
        return inscriptionRepository.save(inscription);
    }

    /**
     * Récupérer les inscriptions par filière, niveau et semestre (pour les filtres)
     */
    public List<Inscription> getInscriptionsByFiliereNiveauSemestre(
            Integer filiereId, String niveau, String semestre) {

        List<Inscription> inscriptions = inscriptionRepository.findAll();

        return inscriptions.stream()
                .filter(i -> filiereId == null ||
                        (i.getFiliere() != null && i.getFiliere().getId().equals(filiereId)))
                .filter(i -> niveau == null || niveau.isEmpty() ||
                        (i.getNiveau() != null && i.getNiveau().equalsIgnoreCase(niveau)))
                .filter(i -> semestre == null || semestre.isEmpty() ||
                        (i.getSemestre() != null && i.getSemestre().equalsIgnoreCase(semestre)))
                .collect(Collectors.toList());
    }

    /**
     * Créer une inscription avec tous les paramètres (pour l'admin)
     */
    @Transactional
    public Inscription createInscription(Long etudiantId, Long moduleId, String semestre,
                                         String anneeScolaire, String statut) {
        Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);
        CourseModule module = moduleService.getModuleById(moduleId);

        // Vérifier si déjà inscrit
        if (inscriptionRepository.existsByEtudiantIdAndModuleId(etudiantId, moduleId)) {
            throw new IllegalArgumentException("L'étudiant est déjà inscrit à ce module");
        }

        Inscription inscription = new Inscription(
                etudiant,
                module,
                etudiant.getFiliere(),
                anneeScolaire != null ? anneeScolaire : getAnneeScolaireActuelle(),
                semestre,
                etudiant.getNiveau()
        );

        if (statut != null) {
            inscription.setStatut(statut);
        }

        // Ajouter le module à l'étudiant
        etudiant.getModules().add(module);

        return inscriptionRepository.save(inscription);
    }

    /**
     * Obtenir les statistiques des inscriptions
     */
    public Map<String, Object> getInscriptionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalInscriptions = inscriptionRepository.count();
        long activeInscriptions = inscriptionRepository.findAll().stream()
                .filter(i -> "ACTIVE".equals(i.getStatut()))
                .count();
        long completedInscriptions = inscriptionRepository.findAll().stream()
                .filter(i -> "COMPLETED".equals(i.getStatut()))
                .count();

        stats.put("total", totalInscriptions);
        stats.put("active", activeInscriptions);
        stats.put("completed", completedInscriptions);
        stats.put("suspended", totalInscriptions - activeInscriptions - completedInscriptions);

        return stats;
    }

    /**
     * Rechercher des inscriptions avec critères multiples
     */
    public List<Inscription> searchInscriptions(String keyword, Integer filiereId,
                                                String niveau, String semestre, String statut) {
        List<Inscription> allInscriptions = inscriptionRepository.findAll();

        return allInscriptions.stream()
                .filter(i -> keyword == null || keyword.isEmpty() ||
                        (i.getEtudiant() != null &&
                                (i.getEtudiant().getNom().toLowerCase().contains(keyword.toLowerCase()) ||
                                        i.getEtudiant().getPrenom().toLowerCase().contains(keyword.toLowerCase()))))
                .filter(i -> filiereId == null ||
                        (i.getFiliere() != null && i.getFiliere().getId().equals(filiereId)))
                .filter(i -> niveau == null || niveau.isEmpty() ||
                        (i.getNiveau() != null && i.getNiveau().equalsIgnoreCase(niveau)))
                .filter(i -> semestre == null || semestre.isEmpty() ||
                        (i.getSemestre() != null && i.getSemestre().equalsIgnoreCase(semestre)))
                .filter(i -> statut == null || statut.isEmpty() ||
                        (i.getStatut() != null && i.getStatut().equalsIgnoreCase(statut)))
                .collect(Collectors.toList());
    }

    /**
     * Vérifier si un étudiant a des inscriptions
     */
    public boolean hasInscriptions(Long etudiantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByEtudiantId(etudiantId);
        return !inscriptions.isEmpty();
    }

    /**
     * Vérifier si un module a des inscriptions
     */
    public boolean hasModuleInscriptions(Long moduleId) {
        List<Inscription> inscriptions = inscriptionRepository.findByModuleId(moduleId);
        return !inscriptions.isEmpty();
    }

    /**
     * Rechercher les inscriptions par année scolaire
     */
    public List<Inscription> getInscriptionsByAnneeScolaire(String anneeScolaire) {
        return inscriptionRepository.findAll().stream()
                .filter(i -> anneeScolaire.equals(i.getAnneeScolaire()))
                .collect(Collectors.toList());
    }

    /**
     * Obtenir les inscriptions d'un étudiant pour un semestre donné
     */
    public List<Inscription> getInscriptionsByEtudiantAndSemestre(Long etudiantId, String semestre) {
        return inscriptionRepository.findByEtudiantId(etudiantId).stream()
                .filter(i -> semestre.equals(i.getSemestre()))
                .collect(Collectors.toList());
    }

    /**
     * Obtenir le nombre d'inscriptions actives par filière
     */
    public Map<String, Long> getActiveInscriptionsByFiliere() {
        return inscriptionRepository.findAll().stream()
                .filter(i -> "ACTIVE".equals(i.getStatut()) && i.getFiliere() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getFiliere().getNom(),
                        Collectors.counting()
                ));
    }

    /**
     * Récupérer les inscriptions avec pagination
     */
    public Map<String, Object> getInscriptionsWithPagination(int page, int size,
                                                             String keyword, Integer filiereId,
                                                             String niveau, String semestre, String statut) {
        List<Inscription> filteredInscriptions = searchInscriptions(keyword, filiereId, niveau, semestre, statut);

        int totalItems = filteredInscriptions.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        int start = Math.min(page * size, totalItems);
        int end = Math.min(start + size, totalItems);

        List<Inscription> paginatedList = filteredInscriptions.subList(start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("inscriptions", paginatedList);
        result.put("currentPage", page);
        result.put("totalPages", totalPages);
        result.put("totalItems", totalItems);

        return result;
    }
}