package com.universite.controller;

import com.universite.model.*;
import com.universite.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.universite.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/etudiant")
@RequiredArgsConstructor
public class EtudiantController {

    private final EtudiantService etudiantService;
    private final EmploiTempsService emploiTempsService;
    private final EvaluationService evaluationService;
    private final ModuleService moduleService;
    private final InscriptionService inscriptionService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user,
                            @RequestParam(value = "date", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            Model model) {

        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        // Récupérer l'étudiant complet
        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

        // Vérifier si le profil est complet
        boolean profilComplet = etudiant.getFiliere() != null &&
                etudiant.getGroupe() != null &&
                etudiant.getNiveau() != null;

        List<String> erreursProfil = new ArrayList<>();
        if (!profilComplet) {
            if (etudiant.getFiliere() == null) erreursProfil.add("Filière non spécifiée");
            if (etudiant.getGroupe() == null) erreursProfil.add("Groupe non spécifié");
            if (etudiant.getNiveau() == null) erreursProfil.add("Niveau non spécifié");
        }

        model.addAttribute("user", etudiant);
        model.addAttribute("profilComplet", profilComplet);
        model.addAttribute("erreursProfil", erreursProfil);

        // 1. RÉCUPÉRER LES MODULES DE L'ÉTUDIANT
        Set<CourseModule> modules = new HashSet<>();
        if (profilComplet) {
            try {
                modules = inscriptionService.getModulesByEtudiant(etudiant.getId());

                if (modules.isEmpty()) {
                    modules = etudiantService.getModulesEtudiant(etudiant.getId());
                }
            } catch (Exception e) {
                modules = etudiantService.getModulesEtudiant(etudiant.getId());
            }
        }

        // Grouper les modules par semestre
        Map<String, List<CourseModule>> modulesParSemestre = modules.stream()
                .filter(m -> m.getSemestre() != null)
                .collect(Collectors.groupingBy(
                        CourseModule::getSemestre,
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("modules", modules);
        model.addAttribute("modulesParSemestre", modulesParSemestre);

        // 2. RÉCUPÉRER LES MODULES DISPONIBLES (selon filière et niveau)
        List<CourseModule> modulesDisponibles = new ArrayList<>();
        if (profilComplet && etudiant.getFiliere() != null && etudiant.getNiveau() != null) {
            try {
                modulesDisponibles = moduleService.getModulesByFiliereAndNiveau(
                        etudiant.getFiliere().getId(),
                        etudiant.getNiveau()
                );

                // Filtrer pour exclure les modules déjà inscrits
                final Set<Long> modulesInscritsIds = modules.stream()
                        .map(CourseModule::getId)
                        .collect(Collectors.toSet());

                modulesDisponibles = modulesDisponibles.stream()
                        .filter(m -> !modulesInscritsIds.contains(m.getId()))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                modulesDisponibles = new ArrayList<>();
            }
        }
        model.addAttribute("modulesDisponibles", modulesDisponibles);

        // 3. RÉCUPÉRER LES SÉANCES FILTRÉES
        List<Seance> seances = new ArrayList<>();
        if (profilComplet) {
            seances = getSeancesFiltrees(etudiant);
        }

        // Filtrer par date sélectionnée
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        List<Seance> seancesFiltreesParDate = new ArrayList<>();

        if (!seances.isEmpty()) {
            final LocalDateTime startOfDay = selectedDate.atStartOfDay();
            final LocalDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay();

            seancesFiltreesParDate = seances.stream()
                    .filter(s -> !s.getDateHeure().isBefore(startOfDay) && s.getDateHeure().isBefore(endOfDay))
                    .sorted(Comparator.comparing(Seance::getDateHeure))
                    .collect(Collectors.toList());
        }

        model.addAttribute("seances", seancesFiltreesParDate);
        model.addAttribute("selectedDate", selectedDate);

        // 4. RÉCUPÉRER LES ÉVALUATIONS
        List<Evaluation> evaluations = new ArrayList<>();
        if (profilComplet) {
            evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());
        }
        model.addAttribute("evaluations", evaluations);

        // 5. CALCULER LA MOYENNE
        Double moyenne = null;
        if (profilComplet) {
            moyenne = evaluationService.getMoyenneEtudiant(etudiant.getId());
        }
        model.addAttribute("moyenne", moyenne != null ? String.format("%.2f", moyenne) : "N/A");

        // 6. STATISTIQUES DIVERSES
        long seancesAujourdhui = seancesFiltreesParDate.size();
        model.addAttribute("seancesAujourdhui", seancesAujourdhui);

        Optional<CourseModule> moduleRecent = modules.stream()
                .max(Comparator.comparing(CourseModule::getCreatedAt));
        model.addAttribute("moduleRecent", moduleRecent.orElse(null));

        int totalHeures = modules.stream()
                .mapToInt(m -> m.getVolumeHoraire() != null ? m.getVolumeHoraire() : 0)
                .sum();
        model.addAttribute("totalHeures", totalHeures);

        String semestreActuel = getSemestreActuelPourNiveau(etudiant.getNiveau());
        model.addAttribute("semestreActuel", semestreActuel);

        // Statistiques des notes
        long evaluationsValidees = evaluations.stream()
                .filter(e -> e.getNoteFinale() != null && e.getNoteFinale() >= 10)
                .count();
        model.addAttribute("evaluationsValidees", evaluationsValidees);
        model.addAttribute("totalEvaluations", evaluations.size());

        return "etudiant/dashboard";
    }

    /**
     * Méthode pour filtrer les séances
     */
    private List<Seance> getSeancesFiltrees(Etudiant etudiant) {
        List<Seance> seancesFiltrees = new ArrayList<>();

        // Vérifications préalables
        if (etudiant.getGroupe() == null) {
            return seancesFiltrees;
        }

        // Récupérer les séances du groupe
        List<Seance> seancesDuGroupe = emploiTempsService.getSeancesByGroupeWithDetails(etudiant.getGroupe());

        if (seancesDuGroupe.isEmpty()) {
            return seancesFiltrees;
        }

        // Récupérer les modules de l'étudiant
        Set<CourseModule> modulesEtudiant = inscriptionService.getModulesByEtudiant(etudiant.getId());
        if (modulesEtudiant.isEmpty()) {
            modulesEtudiant = etudiantService.getModulesEtudiant(etudiant.getId());
        }

        // Filtrer selon différents critères
        if (!modulesEtudiant.isEmpty()) {
            // Filtrer par modules de l'étudiant
            Set<Long> moduleIds = modulesEtudiant.stream()
                    .map(CourseModule::getId)
                    .collect(Collectors.toSet());

            seancesFiltrees = seancesDuGroupe.stream()
                    .filter(s -> s.getModule() != null && moduleIds.contains(s.getModule().getId()))
                    .collect(Collectors.toList());
        } else {
            // Si l'étudiant n'a pas de modules, filtrer par filière
            if (etudiant.getFiliere() != null) {
                seancesFiltrees = seancesDuGroupe.stream()
                        .filter(s -> s.getModule() != null &&
                                s.getModule().getFiliere() != null &&
                                s.getModule().getFiliere().getId().equals(etudiant.getFiliere().getId()))
                        .collect(Collectors.toList());
            } else {
                seancesFiltrees = seancesDuGroupe;
            }
        }

        // Trier par date
        seancesFiltrees.sort(Comparator.comparing(Seance::getDateHeure));

        return seancesFiltrees;
    }

    private String getSemestreActuelPourNiveau(String niveau) {
        if (niveau == null) return "Non spécifié";

        switch (niveau.toUpperCase()) {
            case "L1": return "S1";
            case "L2": return "S3";
            case "L3": return "S5";
            case "M1": return "S7";
            case "M2": return "S9";
            default: return niveau;
        }
    }

    @GetMapping("/modules")
    public String listModules(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

        // Vérifier le profil
        if (etudiant.getFiliere() == null || etudiant.getNiveau() == null) {
            model.addAttribute("error", "Veuillez compléter votre profil (filière et niveau) pour voir vos modules.");
            model.addAttribute("user", etudiant);
            return "etudiant/modules/list";
        }

        // Récupérer les modules via inscriptions
        Set<CourseModule> modules = inscriptionService.getModulesByEtudiant(etudiant.getId());
        if (modules.isEmpty()) {
            modules = etudiantService.getModulesEtudiant(etudiant.getId());
        }

        // Récupérer les modules disponibles
        List<CourseModule> modulesDisponibles = new ArrayList<>();
        try {
            modulesDisponibles = moduleService.getModulesByFiliereAndNiveau(
                    etudiant.getFiliere().getId(),
                    etudiant.getNiveau()
            );
        } catch (Exception e) {
            modulesDisponibles = moduleService.getModulesByFiliere(etudiant.getFiliere().getId());
        }

        // Filtrer pour exclure les modules déjà inscrits
        final Set<Long> modulesInscritsIds = modules.stream()
                .map(CourseModule::getId)
                .collect(Collectors.toSet());

        List<CourseModule> modulesDisponiblesFiltres = modulesDisponibles.stream()
                .filter(m -> !modulesInscritsIds.contains(m.getId()))
                .collect(Collectors.toList());

        // Grouper par semestre
        Map<String, List<CourseModule>> modulesParSemestre = modules.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSemestre() != null ? m.getSemestre() : "Non spécifié",
                        TreeMap::new,
                        Collectors.toList()
                ));

        // Grouper les modules disponibles par semestre
        Map<String, List<CourseModule>> modulesDisponiblesParSemestre = modulesDisponiblesFiltres.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSemestre() != null ? m.getSemestre() : "Non spécifié",
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("modules", modules);
        model.addAttribute("modulesParSemestre", modulesParSemestre);
        model.addAttribute("modulesDisponibles", modulesDisponiblesFiltres);
        model.addAttribute("modulesDisponiblesParSemestre", modulesDisponiblesParSemestre);
        model.addAttribute("user", etudiant);

        String semestreActuel = getSemestreActuelPourNiveau(etudiant.getNiveau());
        model.addAttribute("semestreActuel", semestreActuel);

        return "etudiant/modules/list";
    }

    @PostMapping("/modules/inscrire")
    public String inscrireModule(@AuthenticationPrincipal User user,
                                 @RequestParam("moduleId") Long moduleId,
                                 RedirectAttributes redirectAttributes) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        try {
            Etudiant etudiant = (Etudiant) user;
            Inscription inscription = inscriptionService.inscrireEtudiant(etudiant.getId(), moduleId);
            redirectAttributes.addFlashAttribute("success", "Module inscrit avec succès !");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'inscription : " + e.getMessage());
        }

        return "redirect:/etudiant/modules";
    }

    @PostMapping("/modules/desinscrire")
    public String desinscrireModule(@AuthenticationPrincipal User user,
                                    @RequestParam("moduleId") Long moduleId,
                                    RedirectAttributes redirectAttributes) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        try {
            inscriptionService.desinscrireEtudiant(((Etudiant) user).getId(), moduleId);
            redirectAttributes.addFlashAttribute("success", "Module désinscrit avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la désinscription : " + e.getMessage());
        }

        return "redirect:/etudiant/modules";
    }

    @GetMapping("/profil")
    public String profil(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

        // Récupérer les modules via inscriptions
        Set<CourseModule> modules = inscriptionService.getModulesByEtudiant(etudiant.getId());
        if (modules.isEmpty()) {
            modules = etudiantService.getModulesEtudiant(etudiant.getId());
        }

        // Récupérer les évaluations
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());

        // Calculer les statistiques
        int totalHeures = modules.stream()
                .mapToInt(m -> m.getVolumeHoraire() != null ? m.getVolumeHoraire() : 0)
                .sum();

        Double moyenne = evaluationService.getMoyenneEtudiant(etudiant.getId());
        long evaluationsValidees = evaluations.stream()
                .filter(e -> e.getNoteFinale() != null && e.getNoteFinale() >= 10)
                .count();

        // Récupérer les inscriptions complètes
        List<Inscription> inscriptions = inscriptionService.getInscriptionsByEtudiant(etudiant.getId());

        model.addAttribute("etudiant", etudiant);
        model.addAttribute("user", etudiant);
        model.addAttribute("modules", modules);
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("totalModules", modules.size());
        model.addAttribute("totalHeures", totalHeures);
        model.addAttribute("totalEvaluations", evaluations.size());
        model.addAttribute("evaluationsValidees", evaluationsValidees);
        model.addAttribute("moyenne", moyenne != null ? String.format("%.2f", moyenne) : "N/A");

        return "etudiant/profil";
    }

    @GetMapping("/emploi-temps")
    public String emploiTemps(@AuthenticationPrincipal User user,
                              @RequestParam(value = "semaine", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebutSemaine,
                              Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());
        LocalDate debutSemaine = dateDebutSemaine != null ? dateDebutSemaine : LocalDate.now();
        LocalDateTime startOfWeek = debutSemaine.atStartOfDay().with(java.time.DayOfWeek.MONDAY);

        List<Seance> seances = getSeancesFiltrees(etudiant)
                .stream()
                .filter(s -> !s.getDateHeure().isBefore(startOfWeek) &&
                        s.getDateHeure().isBefore(startOfWeek.plusDays(7)))
                .sorted(Comparator.comparing(Seance::getDateHeure))
                .collect(Collectors.toList());

        // Grouper les séances par jour
        Map<LocalDate, List<Seance>> seancesParJour = seances.stream()
                .collect(Collectors.groupingBy(s -> s.getDateHeure().toLocalDate()));

        model.addAttribute("seances", seances);
        model.addAttribute("seancesParJour", seancesParJour);
        model.addAttribute("user", etudiant);
        model.addAttribute("debutSemaine", debutSemaine);
        model.addAttribute("joursSemaine", getJoursSemaine(debutSemaine));

        return "etudiant/emploi-temps";
    }

    @GetMapping("/module/{id}")
    public String detailModule(@AuthenticationPrincipal User user,
                               @PathVariable("id") Long id,
                               Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());
        CourseModule module = moduleService.getModuleById(id);

        // Vérifier que l'étudiant est bien inscrit à ce module via inscriptions
        Set<CourseModule> modulesEtudiant = inscriptionService.getModulesByEtudiant(etudiant.getId());
        boolean estInscrit = modulesEtudiant.stream()
                .anyMatch(m -> m.getId().equals(id));

        if (!estInscrit) {
            // Vérifier aussi via l'ancienne méthode
            estInscrit = etudiantService.getModulesEtudiant(etudiant.getId())
                    .stream()
                    .anyMatch(m -> m.getId().equals(id));

            if (!estInscrit) {
                return "redirect:/access-denied";
            }
        }

        // Récupérer les séances de ce module pour le groupe de l'étudiant
        List<Seance> seancesModule = getSeancesFiltrees(etudiant)
                .stream()
                .filter(s -> s.getModule().getId().equals(id))
                .sorted(Comparator.comparing(Seance::getDateHeure))
                .collect(Collectors.toList());

        // Récupérer les évaluations pour ce module
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());
        List<Evaluation> evaluationsModule = evaluations.stream()
                .filter(e -> e.getModule() != null && e.getModule().getId().equals(id))
                .collect(Collectors.toList());

        model.addAttribute("module", module);
        model.addAttribute("seances", seancesModule);
        model.addAttribute("evaluations", evaluationsModule);
        model.addAttribute("user", etudiant);

        return "etudiant/modules/detail";
    }


    @GetMapping("/evaluations")
    public String listEvaluations(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        try {
            Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

            log.info("=== DÉBUT DEBUG ÉVALUATIONS ===");
            log.info("Étudiant connecté - ID: {}, Nom: {}, Email: {}",
                    etudiant.getId(),
                    etudiant.getNom() + " " + etudiant.getPrenom(),
                    etudiant.getEmail());

            // Récupérer TOUTES les évaluations de l'étudiant directement depuis la table evaluation
            List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());
            log.info("Nombre d'évaluations récupérées: {}", evaluations.size());

            // Calculer les statistiques
            double moyenneGenerale = 0;
            double meilleureNote = 0;
            String meilleurModule = null;
            int evaluationsValidees = 0;
            int evaluationsEchec = 0;

            if (!evaluations.isEmpty()) {
                double somme = 0;
                int count = 0;

                for (Evaluation eval : evaluations) {
                    if (eval.getNoteFinale() != null) {
                        somme += eval.getNoteFinale();
                        count++;

                        // Meilleure note
                        if (eval.getNoteFinale() > meilleureNote) {
                            meilleureNote = eval.getNoteFinale();
                            meilleurModule = eval.getModule() != null ? eval.getModule().getNom() : "N/A";
                        }

                        // Compter les évaluations validées/en échec
                        if (eval.getNoteFinale() >= 10) {
                            evaluationsValidees++;
                        } else {
                            evaluationsEchec++;
                        }
                    }
                }

                if (count > 0) {
                    moyenneGenerale = somme / count;
                }
            }

            // Calculer le taux de réussite
            int totalEvaluationsAvecNote = evaluations.stream()
                    .mapToInt(e -> e.getNoteFinale() != null ? 1 : 0)
                    .sum();
            double tauxReussite = 0;
            if (totalEvaluationsAvecNote > 0) {
                tauxReussite = (double) evaluationsValidees / totalEvaluationsAvecNote * 100;
            }

            // Grouper les évaluations par semestre pour les filtres
            Map<String, List<Evaluation>> evaluationsParSemestre = evaluations.stream()
                    .collect(Collectors.groupingBy(
                            eval -> eval.getModule() != null && eval.getModule().getSemestre() != null ?
                                    eval.getModule().getSemestre() : "Non spécifié",
                            TreeMap::new,
                            Collectors.toList()
                    ));

            // Ajouter les attributs au modèle
            model.addAttribute("evaluations", evaluations);
            model.addAttribute("evaluationsParSemestre", evaluationsParSemestre);
            model.addAttribute("user", user);
            model.addAttribute("etudiant", etudiant);
            model.addAttribute("moyenneGenerale", moyenneGenerale);
            model.addAttribute("meilleureNote", meilleureNote);
            model.addAttribute("meilleurModule", meilleurModule);
            model.addAttribute("evaluationsValidees", evaluationsValidees);
            model.addAttribute("evaluationsEchec", evaluationsEchec);
            model.addAttribute("totalEvaluations", evaluations.size());
            model.addAttribute("totalEvaluationsAvecNote", totalEvaluationsAvecNote);
            model.addAttribute("tauxReussite", Math.round(tauxReussite));

            log.info("=== FIN DEBUG ÉVALUATIONS ===");
            log.info("Statistiques - Moyenne: {}, Validés: {}, Échec: {}, Total: {}",
                    moyenneGenerale, evaluationsValidees, evaluationsEchec, evaluations.size());

            return "etudiant/evaluations";

        } catch (Exception e) {
            log.error("Erreur lors du chargement des évaluations: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur lors du chargement de vos évaluations: " + e.getMessage());
            model.addAttribute("user", user);
            return "etudiant/evaluations";
        }
    }

    /**
     * Méthode pour créer des données de test (à supprimer en production)
     */
    private List<Evaluation> createTestEvaluations(Etudiant etudiant) {
        List<Evaluation> testEvaluations = new ArrayList<>();

        try {
            // Liste de modules fictifs pour les tests
            String[] modulesTest = {
                    "Mathématiques Appliquées",
                    "Programmation Java",
                    "Base de Données",
                    "Réseaux Informatiques",
                    "Systèmes d'Exploitation"
            };

            String[] semestres = {"S1", "S2", "S3", "S4", "S5"};
            String[] feedbacks = {
                    "Très bon travail, continuez ainsi !",
                    "Bonne participation en cours",
                    "Projet bien réalisé",
                    "Doit travailler davantage les TD",
                    "Excellent travail sur le projet final"
            };

            Random random = new Random();

            for (int i = 0; i < modulesTest.length; i++) {
                Evaluation eval = new Evaluation();
                eval.setId((long) (i + 1));
                eval.setEtudiant(etudiant);

                // Créer un module fictif
                CourseModule module = new CourseModule();
                module.setId((long) (i + 100));
                module.setNom(modulesTest[i]);
                module.setSemestre(semestres[i]);
                module.setVolumeHoraire(60);
                eval.setModule(module);

                // Générer des notes aléatoires
                eval.setNoteDS(8.0 + random.nextDouble() * 12); // entre 8 et 20
                eval.setNoteTP(10.0 + random.nextDouble() * 10); // entre 10 et 20
                eval.setNoteProjet(12.0 + random.nextDouble() * 8); // entre 12 et 20
                eval.setParticipation(14.0 + random.nextDouble() * 6); // entre 14 et 20
                eval.setFeedback(feedbacks[i]);
                eval.setDateEvaluation(LocalDate.now().minusDays(random.nextInt(30)));
                eval.setCreatedAt(LocalDate.now().minusDays(random.nextInt(30)));

                // Calculer la note finale automatiquement
                eval.calculerNoteFinale();

                testEvaluations.add(eval);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la création des données de test: {}", e.getMessage());
        }

        return testEvaluations;
    }

    /**
     * Endpoint pour ajouter une évaluation de test (développement seulement)
     */
    @GetMapping("/evaluations/add-test")
    @ResponseBody
    public String addTestEvaluation(@AuthenticationPrincipal User user) {
        if (!(user instanceof Etudiant)) {
            return "Non autorisé";
        }

        try {
            Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

            // Créer une évaluation de test
            Evaluation evaluation = new Evaluation();
            evaluation.setEtudiant(etudiant);

            // Récupérer un module existant
            List<CourseModule> modules = moduleService.getAllModules();
            if (!modules.isEmpty()) {
                evaluation.setModule(modules.get(0));
            } else {
                // Créer un module de test
                CourseModule module = new CourseModule();
                module.setNom("Module de Test");
                module.setSemestre("S1");
                module.setVolumeHoraire(45);
                evaluation.setModule(module);
            }

            evaluation.setNoteDS(15.0);
            evaluation.setNoteTP(14.5);
            evaluation.setNoteProjet(16.0);
            evaluation.setParticipation(18.0);
            evaluation.setFeedback("Évaluation de test créée avec succès !");
            evaluation.setDateEvaluation(LocalDate.now());

            // La note finale sera calculée automatiquement

            return "Évaluation de test créée pour l'étudiant ID: " + etudiant.getId();

        } catch (Exception e) {
            return "Erreur: " + e.getMessage();
        }
    }

    /**
     * Endpoint pour voir toutes les évaluations de la base (debug)
     */
    @GetMapping("/evaluations/debug")
    @ResponseBody
    public String debugEvaluations(@AuthenticationPrincipal User user) {
        if (!(user instanceof Etudiant)) {
            return "Non autorisé";
        }

        try {
            Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><style>");
            sb.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            sb.append("table { border-collapse: collapse; width: 100%; }");
            sb.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            sb.append("th { background-color: #f2f2f2; }");
            sb.append("tr:hover { background-color: #f5f5f5; }");
            sb.append("</style></head><body>");

            sb.append("<h1>Debug Évaluations</h1>");
            sb.append("<h3>Étudiant connecté:</h3>");
            sb.append("<p><strong>ID:</strong> ").append(etudiant.getId()).append("</p>");
            sb.append("<p><strong>Nom:</strong> ").append(etudiant.getNom()).append(" ").append(etudiant.getPrenom()).append("</p>");
            sb.append("<p><strong>Email:</strong> ").append(etudiant.getEmail()).append("</p>");

            // Évaluations de l'étudiant
            List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());
            sb.append("<h3>Évaluations de l'étudiant (").append(evaluations.size()).append("):</h3>");

            if (evaluations.isEmpty()) {
                sb.append("<p style='color: red;'>AUCUNE ÉVALUATION TROUVÉE POUR CET ÉTUDIANT !</p>");
            } else {
                sb.append("<table>");
                sb.append("<tr><th>ID</th><th>Module</th><th>Note DS</th><th>Note TP</th><th>Note Projet</th><th>Note Finale</th><th>Date</th></tr>");
                for (Evaluation eval : evaluations) {
                    sb.append("<tr>")
                            .append("<td>").append(eval.getId()).append("</td>")
                            .append("<td>").append(eval.getModule() != null ? eval.getModule().getNom() : "N/A").append("</td>")
                            .append("<td>").append(eval.getNoteDS() != null ? eval.getNoteDS() : "-").append("</td>")
                            .append("<td>").append(eval.getNoteTP() != null ? eval.getNoteTP() : "-").append("</td>")
                            .append("<td>").append(eval.getNoteProjet() != null ? eval.getNoteProjet() : "-").append("</td>")
                            .append("<td>").append(eval.getNoteFinale() != null ? eval.getNoteFinale() : "-").append("</td>")
                            .append("<td>").append(eval.getDateEvaluation() != null ? eval.getDateEvaluation() : "-").append("</td>")
                            .append("</tr>");
                }
                sb.append("</table>");
            }

            // Toutes les évaluations de la base
            List<Evaluation> toutesEvaluations = evaluationService.getAllEvaluations();
            sb.append("<h3>Toutes les évaluations dans la base (").append(toutesEvaluations.size()).append("):</h3>");

            if (!toutesEvaluations.isEmpty()) {
                sb.append("<table>");
                sb.append("<tr><th>ID</th><th>Étudiant ID</th><th>Étudiant Nom</th><th>Module</th><th>Note Finale</th></tr>");
                for (Evaluation eval : toutesEvaluations) {
                    sb.append("<tr>")
                            .append("<td>").append(eval.getId()).append("</td>")
                            .append("<td>").append(eval.getEtudiant() != null ? eval.getEtudiant().getId() : "N/A").append("</td>")
                            .append("<td>").append(eval.getEtudiant() != null ?
                                    eval.getEtudiant().getNom() + " " + eval.getEtudiant().getPrenom() : "N/A").append("</td>")
                            .append("<td>").append(eval.getModule() != null ? eval.getModule().getNom() : "N/A").append("</td>")
                            .append("<td>").append(eval.getNoteFinale() != null ? eval.getNoteFinale() : "-").append("</td>")
                            .append("</tr>");
                }
                sb.append("</table>");
            }

            sb.append("</body></html>");

            return sb.toString();

        } catch (Exception e) {
            return "Erreur: " + e.getMessage();
        }
    }

    @GetMapping("/seances/{moduleId}")
    public String seancesModule(@AuthenticationPrincipal User user,
                                @PathVariable("moduleId") Long moduleId,
                                Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());
        CourseModule module = moduleService.getModuleById(moduleId);

        // Vérifier que l'étudiant est inscrit au module
        Set<CourseModule> modulesEtudiant = inscriptionService.getModulesByEtudiant(etudiant.getId());
        boolean estInscrit = modulesEtudiant.stream()
                .anyMatch(m -> m.getId().equals(moduleId));

        if (!estInscrit) {
            estInscrit = etudiantService.getModulesEtudiant(etudiant.getId())
                    .stream()
                    .anyMatch(m -> m.getId().equals(moduleId));

            if (!estInscrit) {
                return "redirect:/access-denied";
            }
        }

        List<Seance> seances = getSeancesFiltrees(etudiant)
                .stream()
                .filter(s -> s.getModule().getId().equals(moduleId))
                .sorted(Comparator.comparing(Seance::getDateHeure))
                .collect(Collectors.toList());

        // Grouper les séances par semaine
        Map<String, List<Seance>> seancesParSemaine = seances.stream()
                .collect(Collectors.groupingBy(s -> {
                    LocalDate date = s.getDateHeure().toLocalDate();
                    return "Semaine " + date.get(java.time.temporal.WeekFields.of(Locale.FRANCE).weekOfYear());
                }));

        model.addAttribute("module", module);
        model.addAttribute("seances", seances);
        model.addAttribute("seancesParSemaine", seancesParSemaine);
        model.addAttribute("user", etudiant);

        return "etudiant/modules/seances";
    }

    @GetMapping("/notes/{moduleId}")
    public String notesModule(@AuthenticationPrincipal User user,
                              @PathVariable("moduleId") Long moduleId,
                              Model model) {
        if (!(user instanceof Etudiant)) {
            return "redirect:/access-denied";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());
        CourseModule module = moduleService.getModuleById(moduleId);

        // Vérifier que l'étudiant est inscrit au module
        Set<CourseModule> modulesEtudiant = inscriptionService.getModulesByEtudiant(etudiant.getId());
        boolean estInscrit = modulesEtudiant.stream()
                .anyMatch(m -> m.getId().equals(moduleId));

        if (!estInscrit) {
            estInscrit = etudiantService.getModulesEtudiant(etudiant.getId())
                    .stream()
                    .anyMatch(m -> m.getId().equals(moduleId));

            if (!estInscrit) {
                return "redirect:/access-denied";
            }
        }

        // Récupérer les évaluations pour ce module
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiant.getId());
        List<Evaluation> evaluationsModule = evaluations.stream()
                .filter(e -> e.getModule() != null && e.getModule().getId().equals(moduleId))
                .collect(Collectors.toList());

        model.addAttribute("module", module);
        model.addAttribute("evaluations", evaluationsModule);
        model.addAttribute("user", etudiant);

        return "etudiant/modules/notes";
    }

    @GetMapping("/debug-seances")
    @ResponseBody
    public String debugSeances(@AuthenticationPrincipal User user) {
        if (!(user instanceof Etudiant)) {
            return "Not an etudiant";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());
        StringBuilder sb = new StringBuilder();

        sb.append("<html><head>");
        sb.append("<style>");
        sb.append("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }");
        sb.append(".container { max-width: 1200px; margin: 0 auto; }");
        sb.append(".card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        sb.append(".error { color: #dc3545; font-weight: bold; }");
        sb.append(".success { color: #28a745; font-weight: bold; }");
        sb.append(".warning { color: #ffc107; font-weight: bold; }");
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
        sb.append("th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }");
        sb.append("th { background-color: #f8f9fa; font-weight: bold; }");
        sb.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        sb.append("tr:hover { background-color: #f5f5f5; }");
        sb.append(".badge { padding: 3px 8px; border-radius: 12px; font-size: 12px; }");
        sb.append(".badge-primary { background: #007bff; color: white; }");
        sb.append(".badge-success { background: #28a745; color: white; }");
        sb.append(".badge-warning { background: #ffc107; color: black; }");
        sb.append(".badge-danger { background: #dc3545; color: white; }");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<div class='container'>");

        // En-tête
        sb.append("<div class='card'>");
        sb.append("<h1>Debug Séances pour ").append(etudiant.getNom()).append(" ").append(etudiant.getPrenom()).append("</h1>");
        sb.append("<p><strong>ID:</strong> ").append(etudiant.getId()).append("</p>");
        sb.append("<p><strong>Email:</strong> ").append(etudiant.getEmail()).append("</p>");
        sb.append("<p><strong>Groupe:</strong> <span class='badge badge-primary'>").append(etudiant.getGroupe() != null ? etudiant.getGroupe() : "<span class='error'>NULL</span>").append("</span></p>");
        sb.append("<p><strong>Filière:</strong> ").append(etudiant.getFiliere() != null ?
                etudiant.getFiliere().getNom() + " (ID: " + etudiant.getFiliere().getId() + ")" :
                "<span class='error'>NULL</span>").append("</p>");
        sb.append("<p><strong>Niveau:</strong> ").append(etudiant.getNiveau() != null ? etudiant.getNiveau() : "<span class='error'>NULL</span>").append("</p>");
        sb.append("</div>");

        // 1. Modules de l'étudiant (via inscriptions)
        Set<CourseModule> modules = inscriptionService.getModulesByEtudiant(etudiant.getId());
        sb.append("<div class='card'>");
        sb.append("<h2>1. Modules de l'étudiant (via inscriptions)</h2>");
        sb.append("<p><strong>Nombre:</strong> <span class='badge ").append(modules.isEmpty() ? "badge-danger" : "badge-success").append("'>")
                .append(modules.size()).append("</span></p>");

        if (modules.isEmpty()) {
            sb.append("<p class='error'>L'ÉTUDIANT N'A AUCUN MODULE INSCRIT !</p>");
            sb.append("<p>Solution : Inscrivez-vous à des modules depuis la page 'Mes modules'</p>");
        } else {
            sb.append("<table>");
            sb.append("<tr><th>ID</th><th>Nom</th><th>Semestre</th><th>Volume H</th><th>Filière</th><th>Enseignant</th></tr>");
            for (CourseModule m : modules) {
                sb.append("<tr>")
                        .append("<td>").append(m.getId()).append("</td>")
                        .append("<td><strong>").append(m.getNom()).append("</strong></td>")
                        .append("<td><span class='badge badge-warning'>").append(m.getSemestre() != null ? m.getSemestre() : "N/A").append("</span></td>")
                        .append("<td>").append(m.getVolumeHoraire() != null ? m.getVolumeHoraire() + "h" : "0h").append("</td>")
                        .append("<td>").append(m.getFiliere() != null ? m.getFiliere().getNom() : "<span class='error'>NULL</span>").append("</td>")
                        .append("<td>").append(m.getEnseignant() != null ?
                                m.getEnseignant().getNom() + " " + m.getEnseignant().getPrenom() : "<span class='warning'>Non affecté</span>").append("</td>")
                        .append("</tr>");
            }
            sb.append("</table>");
        }
        sb.append("</div>");

        // 2. Séances du groupe (toutes)
        sb.append("<div class='card'>");
        sb.append("<h2>2. Séances du groupe '").append(etudiant.getGroupe() != null ? etudiant.getGroupe() : "NULL").append("'</h2>");

        List<Seance> seancesGroupe = emploiTempsService.getSeancesByGroupeWithDetails(etudiant.getGroupe());
        sb.append("<p><strong>Nombre:</strong> <span class='badge ").append(seancesGroupe.isEmpty() ? "badge-danger" : "badge-success").append("'>")
                .append(seancesGroupe.size()).append("</span></p>");

        if (seancesGroupe.isEmpty()) {
            sb.append("<p class='error'>AUCUNE SÉANCE TROUVÉE POUR CE GROUPE !</p>");
            sb.append("<p>Solutions :</p>");
            sb.append("<ul>");
            sb.append("<li>Créez des séances avec le groupe '").append(etudiant.getGroupe()).append("'</li>");
            sb.append("<li>Vérifiez que les séances existent dans la base de données</li>");
            sb.append("<li>Assurez-vous que le groupe de l'étudiant est correct</li>");
            sb.append("</ul>");
        } else {
            sb.append("<table>");
            sb.append("<tr><th>Module</th><th>Date/Heure</th><th>Groupe</th><th>Filière Module</th><th>Type</th><th>Enseignant</th><th>Salle</th></tr>");
            for (Seance s : seancesGroupe) {
                boolean correspondModule = modules.stream().anyMatch(m -> m.getId().equals(s.getModule().getId()));
                sb.append("<tr style='").append(correspondModule ? "background-color: #e8f5e8;" : "").append("'>")
                        .append("<td><strong>").append(s.getModule() != null ? s.getModule().getNom() : "<span class='error'>NULL</span>").append("</strong><br>")
                        .append("<small>Module ID: ").append(s.getModule() != null ? s.getModule().getId() : "null").append("</small></td>")
                        .append("<td>").append(s.getDateHeure()).append("<br><small>Durée: ").append(s.getDuree()).append("min</small></td>")
                        .append("<td><span class='badge badge-primary'>").append(s.getGroupe()).append("</span></td>")
                        .append("<td>").append(s.getModule() != null && s.getModule().getFiliere() != null ?
                                s.getModule().getFiliere().getNom() + "<br><small>ID: " + s.getModule().getFiliere().getId() + "</small>" :
                                "<span class='warning'>NULL</span>").append("</td>")
                        .append("<td><span class='badge ").append(getBadgeClassForType(s.getTypeSeance())).append("'>")
                        .append(s.getTypeSeance() != null ? s.getTypeSeance() : "N/A").append("</span></td>")
                        .append("<td>").append(s.getEnseignant() != null ?
                                s.getEnseignant().getNom() + " " + s.getEnseignant().getPrenom() : "<span class='warning'>NULL</span>").append("</td>")
                        .append("<td>").append(s.getSalle() != null ? s.getSalle().getNom() : "<span class='warning'>NULL</span>").append("</td>")
                        .append("</tr>");
            }
            sb.append("</table>");

            // Statistiques
            sb.append("<div style='margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 5px;'>");
            sb.append("<h4>Statistiques :</h4>");

            long correspondancesModules = seancesGroupe.stream()
                    .filter(s -> modules.stream().anyMatch(m -> m.getId().equals(s.getModule().getId())))
                    .count();

            Map<String, Long> statsParFiliere = seancesGroupe.stream()
                    .filter(s -> s.getModule() != null && s.getModule().getFiliere() != null)
                    .collect(Collectors.groupingBy(
                            s -> s.getModule().getFiliere().getNom(),
                            Collectors.counting()
                    ));

            sb.append("<p><strong>Séances correspondant aux modules de l'étudiant :</strong> ")
                    .append("<span class='badge ").append(correspondancesModules > 0 ? "badge-success" : "badge-warning").append("'>")
                    .append(correspondancesModules).append("</span></p>");

            sb.append("<p><strong>Répartition par filière :</strong></p>");
            sb.append("<ul>");
            statsParFiliere.forEach((filiere, count) -> {
                sb.append("<li>").append(filiere).append(" : ").append(count).append(" séance(s)</li>");
            });
            sb.append("</ul>");
            sb.append("</div>");
        }
        sb.append("</div>");

        // 3. Test avec la méthode de filtrage
        sb.append("<div class='card'>");
        sb.append("<h2>3. Séances filtrées (méthode getSeancesFiltrees)</h2>");

        List<Seance> seancesFiltrees = getSeancesFiltrees(etudiant);
        sb.append("<p><strong>Séances filtrées :</strong> <span class='badge ").append(seancesFiltrees.isEmpty() ? "badge-warning" : "badge-success").append("'>")
                .append(seancesFiltrees.size()).append("</span></p>");

        if (!seancesFiltrees.isEmpty()) {
            sb.append("<table>");
            sb.append("<tr><th>Module</th><th>Date</th><th>Type</th><th>Correspond à module étudiant</th></tr>");
            for (Seance s : seancesFiltrees) {
                boolean correspondModule = modules.stream().anyMatch(m -> m.getId().equals(s.getModule().getId()));
                sb.append("<tr>")
                        .append("<td>").append(s.getModule().getNom()).append("</td>")
                        .append("<td>").append(s.getDateHeure()).append("</td>")
                        .append("<td>").append(s.getTypeSeance()).append("</td>")
                        .append("<td><span class='badge ").append(correspondModule ? "badge-success" : "badge-warning").append("'>")
                        .append(correspondModule ? "OUI" : "NON (toutes séances groupe)").append("</span></td>")
                        .append("</tr>");
            }
            sb.append("</table>");
        }
        sb.append("</div>");

        sb.append("</div>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private String getBadgeClassForType(String type) {
        if (type == null) return "badge-secondary";
        switch (type.toUpperCase()) {
            case "COURS": return "badge-primary";
            case "TD": return "badge-warning";
            case "TP": return "badge-success";
            default: return "badge-secondary";
        }
    }

    @GetMapping("/debug-data")
    @ResponseBody
    public String debugData(@AuthenticationPrincipal User user) {
        if (!(user instanceof Etudiant)) {
            return "Not an etudiant";
        }

        Etudiant etudiant = etudiantService.getEtudiantById(((Etudiant) user).getId());

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>");
        sb.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        sb.append("h1, h2, h3 { color: #333; }");
        sb.append("ul { list-style-type: none; padding: 0; }");
        sb.append("li { padding: 5px; margin: 2px; background: #f5f5f5; }");
        sb.append(".error { color: red; }");
        sb.append(".success { color: green; }");
        sb.append(".warning { color: orange; }");
        sb.append("</style></head><body>");

        sb.append("<h1>Debug Données Étudiant</h1>");

        sb.append("<h2>Informations étudiant</h2>");
        sb.append("<ul>");
        sb.append("<li><strong>ID:</strong> ").append(etudiant.getId()).append("</li>");
        sb.append("<li><strong>Nom:</strong> ").append(etudiant.getNom()).append(" ").append(etudiant.getPrenom()).append("</li>");
        sb.append("<li><strong>Email:</strong> ").append(etudiant.getEmail()).append("</li>");
        sb.append("<li><strong>Groupe:</strong> ").append(etudiant.getGroupe() != null ? etudiant.getGroupe() : "<span class='error'>NULL</span>").append("</li>");
        sb.append("<li><strong>Niveau:</strong> ").append(etudiant.getNiveau() != null ? etudiant.getNiveau() : "<span class='error'>NULL</span>").append("</li>");
        sb.append("<li><strong>Filière:</strong> ").append(etudiant.getFiliere() != null ?
                etudiant.getFiliere().getNom() + " (ID: " + etudiant.getFiliere().getId() + ")" :
                "<span class='error'>NULL</span>").append("</li>");
        sb.append("</ul>");

        // Modules via inscriptions
        Set<CourseModule> modules = inscriptionService.getModulesByEtudiant(etudiant.getId());
        sb.append("<h2>Modules (via inscriptions) (").append(modules.size()).append(")</h2>");
        if (modules.isEmpty()) {
            sb.append("<p class='error'>Aucun module trouvé via inscriptions!</p>");
            sb.append("<p>Tentative avec l'ancienne méthode...</p>");
            modules = etudiantService.getModulesEtudiant(etudiant.getId());
            sb.append("<p>Modules via ancienne méthode: ").append(modules.size()).append("</p>");
        }

        if (!modules.isEmpty()) {
            sb.append("<ul>");
            for (CourseModule module : modules) {
                sb.append("<li>")
                        .append("<strong>").append(module.getNom()).append("</strong>")
                        .append(" (ID: ").append(module.getId())
                        .append(", Semestre: ").append(module.getSemestre())
                        .append(", Vol.H: ").append(module.getVolumeHoraire() != null ? module.getVolumeHoraire() : "0")
                        .append(", Filière: ").append(module.getFiliere() != null ? module.getFiliere().getNom() : "null")
                        .append(")</li>");
            }
            sb.append("</ul>");
        }

        // Séances filtrées
        List<Seance> seances = getSeancesFiltrees(etudiant);
        sb.append("<h2>Séances filtrées (").append(seances.size()).append(")</h2>");
        if (seances.isEmpty()) {
            sb.append("<p class='error'>Aucune séance trouvée!</p>");
            sb.append("<p>Vérifiez que:");
            sb.append("<ul>");
            sb.append("<li>L'étudiant a un groupe et une filière</li>");
            sb.append("<li>Les séances existent dans la base de données</li>");
            sb.append("<li>Les séances ont le même groupe que l'étudiant</li>");
            sb.append("<li>Les modules des séances ont la même filière que l'étudiant</li>");
            sb.append("</ul></p>");
        } else {
            sb.append("<ul>");
            for (Seance seance : seances) {
                sb.append("<li>")
                        .append("<strong>").append(seance.getModule().getNom()).append("</strong>")
                        .append(" - ").append(seance.getDateHeure())
                        .append(" - Groupe: ").append(seance.getGroupe())
                        .append(" - Type: ").append(seance.getTypeSeance())
                        .append(" - Salle: ").append(seance.getSalle() != null ? seance.getSalle().getNom() : "null")
                        .append(" - Enseignant: ").append(seance.getEnseignant() != null ?
                                seance.getEnseignant().getNom() + " " + seance.getEnseignant().getPrenom() : "null")
                        .append("</li>");
            }
            sb.append("</ul>");
        }

        sb.append("</body></html>");

        return sb.toString();
    }

    // Méthode utilitaire pour générer les jours de la semaine
    private List<LocalDate> getJoursSemaine(LocalDate date) {
        List<LocalDate> jours = new ArrayList<>();
        LocalDate lundi = date.with(java.time.DayOfWeek.MONDAY);
        for (int i = 0; i < 7; i++) {
            jours.add(lundi.plusDays(i));
        }
        return jours;
    }



}