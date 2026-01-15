package com.universite.controller;

import com.universite.dto.*;
import com.universite.model.*;
// En haut du fichier, avec les autres imports
import java.time.LocalDate;  // Ajoutez cette ligne
import java.time.LocalDateTime;

import com.universite.repository.*;
import com.universite.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final EtudiantService etudiantService;
    private final EnseignantService enseignantService;
    private final ModuleService moduleService;
    private final EmploiTempsService emploiTempsService;
    private final FiliereRepository filiereRepository;
    private final SalleRepository salleRepository;
    private final AdminService adminService;
    private final FiliereService filiereService;
    private final SalleService salleService;
    private final InscriptionService inscriptionService;
    private final InscriptionRepository inscriptionRepository;
    private final ModuleRepository moduleRepository;  // Ajoutez cette ligne
    private final EnseignantRepository enseignantRepository;  // Ajoutez si besoin

    // ============ DASHBOARD ============
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        log.info("=== ADMIN DASHBOARD ACCESSED ===");

        if (user == null) {
            log.error("User is null! Redirecting to login");
            return "redirect:/login";
        }

        log.info("User: {} {} ({})", user.getPrenom(), user.getNom(), user.getEmail());
        log.info("User role: {}", user.getRole() != null ? user.getRole().getName() : "NO ROLE");

        model.addAttribute("user", user);

        try {
            // Récupérer les statistiques de base
            long totalEtudiants = etudiantService.getAllEtudiants().size();
            long totalEnseignants = enseignantService.getAllEnseignants().size();
            long totalModules = moduleService.getAllModules().size();
            long totalSalles = salleService.getAllSalles().size();
            long totalFilieres = filiereService.getAllFilieres().size();

            // Statistiques des inscriptions
            Map<String, Object> inscriptionStats = inscriptionService.getInscriptionStatistics();

            log.info("Stats - Étudiants: {}, Enseignants: {}, Modules: {}, Salles: {}, Filieres: {}",
                    totalEtudiants, totalEnseignants, totalModules, totalSalles, totalFilieres);

            model.addAttribute("totalEtudiants", totalEtudiants);
            model.addAttribute("totalEnseignants", totalEnseignants);
            model.addAttribute("totalModules", totalModules);
            model.addAttribute("totalSalles", totalSalles);
            model.addAttribute("totalFilieres", totalFilieres);
            model.addAttribute("inscriptionStats", inscriptionStats);

            // Récupérer la répartition par filière (pour le graphique)
            List<Filiere> filieres = filiereService.getAllFilieres();
            Map<String, Integer> repartitionParFiliere = new LinkedHashMap<>();

            for (Filiere filiere : filieres) {
                try {
                    int nombreEtudiants = etudiantService.getEtudiantsByFiliere(filiere.getId()).size();
                    repartitionParFiliere.put(filiere.getNom(), nombreEtudiants);
                } catch (Exception e) {
                    log.warn("Erreur lors du comptage des étudiants pour la filière {}: {}",
                            filiere.getNom(), e.getMessage());
                    repartitionParFiliere.put(filiere.getNom(), 0);
                }
            }

            log.info("Répartition par filière: {}", repartitionParFiliere);
            model.addAttribute("repartitionParFiliere", repartitionParFiliere);

            // Dernières inscriptions
            List<Inscription> dernieresInscriptions = inscriptionService.getAllInscriptions()
                    .stream()
                    .sorted(Comparator.comparing(Inscription::getDateInscription).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("dernieresInscriptions", dernieresInscriptions);

        } catch (Exception e) {
            log.error("Error in dashboard: {}", e.getMessage(), e);
            model.addAttribute("totalEtudiants", 0);
            model.addAttribute("totalEnseignants", 0);
            model.addAttribute("totalModules", 0);
            model.addAttribute("totalSalles", 0);
            model.addAttribute("totalFilieres", 0);
            model.addAttribute("error", "Erreur lors du chargement des statistiques");
            model.addAttribute("repartitionParFiliere", new HashMap<>());
        }

        return "admin/dashboard";
    }

    // ============ GESTION DES INSCRIPTIONS ============
    @GetMapping("/inscriptions")
    public String listInscriptions(Model model, @AuthenticationPrincipal User user,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) Integer filiereId,
                                   @RequestParam(required = false) String niveau,
                                   @RequestParam(required = false) String semestre,
                                   @RequestParam(required = false) String statut,
                                   @RequestParam(defaultValue = "0") int page) {
        log.info("=== ACCESSING INSCRIPTIONS LIST ===");
        log.info("Search: {}, Filière: {}, Niveau: {}, Semestre: {}, Statut: {}",
                search, filiereId, niveau, semestre, statut);

        try {
            // Récupérer les inscriptions avec filtres
            List<Inscription> inscriptions = inscriptionService.searchInscriptions(
                    search, filiereId, niveau, semestre, statut);

            // Trier par date d'inscription (plus récent d'abord)
            inscriptions.sort(Comparator.comparing(Inscription::getDateInscription).reversed());

            // Pagination
            int pageSize = 15;
            int totalItems = inscriptions.size();
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);

            int start = Math.min(page * pageSize, totalItems);
            int end = Math.min(start + pageSize, totalItems);

            List<Inscription> paginatedInscriptions = inscriptions.subList(start, end);

            // Statistiques
            long totalInscriptions = inscriptions.size();
            long inscriptionsActives = inscriptions.stream()
                    .filter(i -> "ACTIVE".equals(i.getStatut()))
                    .count();
            long inscriptionsCompletees = inscriptions.stream()
                    .filter(i -> "COMPLETED".equals(i.getStatut()))
                    .count();

            // Répartition par filière
            Map<String, Long> statsParFiliere = inscriptions.stream()
                    .filter(i -> i.getFiliere() != null)
                    .collect(Collectors.groupingBy(
                            i -> i.getFiliere().getNom(),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            model.addAttribute("inscriptions", paginatedInscriptions);
            model.addAttribute("filieres", filiereService.getAllFilieres());
            model.addAttribute("user", user);
            model.addAttribute("search", search);
            model.addAttribute("filiereId", filiereId);
            model.addAttribute("niveau", niveau);
            model.addAttribute("semestre", semestre);
            model.addAttribute("statut", statut);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalInscriptions", totalInscriptions);
            model.addAttribute("inscriptionsActives", inscriptionsActives);
            model.addAttribute("inscriptionsCompletees", inscriptionsCompletees);
            model.addAttribute("statsParFiliere", statsParFiliere);

            return "admin/inscriptions/list";

        } catch (Exception e) {
            log.error("Error accessing inscriptions list: {}", e.getMessage(), e);
            model.addAttribute("inscriptions", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des inscriptions: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/inscriptions/list";
        }
    }

    @GetMapping("/inscriptions/affecter")
    public String affecterEtudiantsForm(Model model, @AuthenticationPrincipal User user) {
        log.info("=== ACCESSING AFFECTATION FORM ===");

        try {
            List<Filiere> filieres = filiereService.getAllFilieres();
            List<CourseModule> modules = moduleService.getAllModules();
            List<Etudiant> etudiants = etudiantService.getAllEtudiants();

            model.addAttribute("filieres", filieres);
            model.addAttribute("modules", modules);
            model.addAttribute("etudiants", etudiants);
            model.addAttribute("user", user);

            return "admin/inscriptions/affecter";

        } catch (Exception e) {
            log.error("Error accessing affectation form: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur lors du chargement du formulaire: " + e.getMessage());
            model.addAttribute("user", user);
            return "redirect:/admin/inscriptions";
        }
    }

    @PostMapping("/inscriptions/affecter-mass")
    public String affecterEtudiantsMass(@RequestParam Integer filiereId,
                                        @RequestParam String niveau,
                                        @RequestParam String semestre,
                                        @RequestParam Long moduleId,
                                        @RequestParam(required = false) String anneeScolaire,
                                        @AuthenticationPrincipal User user,
                                        RedirectAttributes redirectAttributes) {
        log.info("=== MASS AFFECTATION ===");
        log.info("Filière: {}, Niveau: {}, Semestre: {}, Module: {}",
                filiereId, niveau, semestre, moduleId);

        try {
            List<Etudiant> etudiants = etudiantService.getEtudiantsByFiliereAndNiveau(filiereId, niveau);

            if (etudiants.isEmpty()) {
                redirectAttributes.addFlashAttribute("warning",
                        "Aucun étudiant trouvé pour cette filière et niveau");
                return "redirect:/admin/inscriptions/affecter";
            }

            CourseModule module = moduleService.getModuleById(moduleId);

            int count = 0;
            int dejaInscrits = 0;
            List<String> erreurs = new ArrayList<>();

            for (Etudiant etudiant : etudiants) {
                try {
                    if (inscriptionRepository.existsByEtudiantIdAndModuleId(etudiant.getId(), moduleId)) {
                        dejaInscrits++;
                        continue;
                    }

                    Inscription inscription = new Inscription(
                            etudiant,
                            module,
                            etudiant.getFiliere(),
                            anneeScolaire != null ? anneeScolaire : getAnneeScolaireActuelle(),
                            semestre,
                            niveau
                    );
                    inscription.setStatut("ACTIVE");
                    inscriptionRepository.save(inscription);
                    count++;

                } catch (Exception e) {
                    erreurs.add(etudiant.getNom() + " " + etudiant.getPrenom() + ": " + e.getMessage());
                }
            }

            if (count > 0) {
                redirectAttributes.addFlashAttribute("success",
                        count + " étudiants affectés au module " + module.getNom());
            }
            if (dejaInscrits > 0) {
                redirectAttributes.addFlashAttribute("info",
                        dejaInscrits + " étudiants étaient déjà inscrits à ce module");
            }
            if (!erreurs.isEmpty()) {
                redirectAttributes.addFlashAttribute("warning",
                        erreurs.size() + " erreurs lors de l'affectation");
            }

            return "redirect:/admin/inscriptions";

        } catch (Exception e) {
            log.error("Error in mass affectation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/inscriptions/affecter";
        }
    }

    @PostMapping("/inscriptions/affecter-individuel")
    public String affecterEtudiantIndividuel(@RequestParam Long etudiantId,
                                             @RequestParam Long moduleId,
                                             @RequestParam String semestre,
                                             @RequestParam(required = false) String statut,
                                             @RequestParam(required = false) String anneeScolaire,
                                             @AuthenticationPrincipal User user,
                                             RedirectAttributes redirectAttributes) {
        log.info("=== INDIVIDUAL AFFECTATION ===");
        log.info("Étudiant: {}, Module: {}, Semestre: {}", etudiantId, moduleId, semestre);

        try {
            Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);
            CourseModule module = moduleService.getModuleById(moduleId);

            if (inscriptionRepository.existsByEtudiantIdAndModuleId(etudiantId, moduleId)) {
                redirectAttributes.addFlashAttribute("warning",
                        "L'étudiant est déjà inscrit à ce module");
                return "redirect:/admin/inscriptions";
            }

            Inscription inscription = new Inscription(
                    etudiant,
                    module,
                    etudiant.getFiliere(),
                    anneeScolaire != null ? anneeScolaire : getAnneeScolaireActuelle(),
                    semestre,
                    etudiant.getNiveau()
            );

            inscription.setStatut(statut != null && !statut.isEmpty() ? statut : "ACTIVE");
            inscriptionRepository.save(inscription);

            redirectAttributes.addFlashAttribute("success",
                    "Étudiant " + etudiant.getNom() + " " + etudiant.getPrenom() +
                            " affecté au module " + module.getNom());

            return "redirect:/admin/inscriptions";

        } catch (Exception e) {
            log.error("Error in individual affectation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/inscriptions/affecter";
        }
    }

    @PostMapping("/inscriptions/update/{id}")
    public String updateInscription(@PathVariable Long id,
                                    @RequestParam String statut,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        log.info("=== UPDATING INSCRIPTION ID: {} ===", id);

        try {
            inscriptionService.updateStatut(id, statut);
            redirectAttributes.addFlashAttribute("success", "Statut de l'inscription mis à jour avec succès");
            return "redirect:/admin/inscriptions";

        } catch (Exception e) {
            log.error("Error updating inscription: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/inscriptions";
        }
    }

    @PostMapping("/inscriptions/delete/{id}")
    public String deleteInscription(@PathVariable Long id,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        log.info("=== DELETING INSCRIPTION ID: {} ===", id);

        try {
            inscriptionService.desinscrireEtudiantByInscriptionId(id);
            redirectAttributes.addFlashAttribute("success", "Inscription supprimée avec succès");
            return "redirect:/admin/inscriptions";

        } catch (Exception e) {
            log.error("Error deleting inscription: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/inscriptions";
        }
    }

    @GetMapping("/inscriptions/{id}")
    public String inscriptionDetails(@PathVariable Long id,
                                     Model model,
                                     @AuthenticationPrincipal User user,
                                     RedirectAttributes redirectAttributes) {
        log.info("=== ACCESSING INSCRIPTION DETAILS ID: {} ===", id);

        try {
            Inscription inscription = inscriptionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Inscription non trouvée"));

            model.addAttribute("inscription", inscription);
            model.addAttribute("user", user);
            return "admin/inscriptions/details";

        } catch (Exception e) {
            log.error("Error accessing inscription details: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Inscription non trouvée: " + e.getMessage());
            return "redirect:/admin/inscriptions";
        }
    }

    @GetMapping("/inscriptions/export")
    public String exportInscriptions(@RequestParam(required = false) Integer filiereId,
                                     @RequestParam(required = false) String niveau,
                                     @RequestParam(required = false) String semestre,
                                     Model model) {
        log.info("=== EXPORTING INSCRIPTIONS ===");

        try {
            List<Inscription> inscriptions = inscriptionService.getInscriptionsByFiliereNiveauSemestre(
                    filiereId, niveau, semestre);

            model.addAttribute("inscriptions", inscriptions);
            model.addAttribute("exportDate", LocalDateTime.now());
            model.addAttribute("filiere", filiereId != null ?
                    filiereService.getFiliereById(filiereId).getNom() : "Toutes filières");

            return "admin/inscriptions/export";
        } catch (Exception e) {
            log.error("Error exporting inscriptions: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur lors de l'export: " + e.getMessage());
            return "redirect:/admin/inscriptions";
        }
    }

    // ============ GESTION DES ÉTUDIANTS ============
    @GetMapping("/etudiants")
    public String listEtudiants(Model model, @AuthenticationPrincipal User user) {
        try {
            List<Etudiant> etudiants = etudiantService.getAllEtudiants();
            model.addAttribute("etudiants", etudiants);
            model.addAttribute("user", user);
            return "admin/etudiants/list";
        } catch (Exception e) {
            log.error("Error accessing etudiants list: {}", e.getMessage(), e);
            model.addAttribute("etudiants", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des étudiants");
            model.addAttribute("user", user);
            return "admin/etudiants/list";
        }
    }

    @GetMapping("/etudiants/create")
    public String createEtudiantForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("user", user);
        return "admin/etudiants/create";
    }

    @PostMapping("/etudiants/create")
    public String createEtudiant(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult result,
                                 Model model,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {

        if (request.getNom() == null || request.getNom().trim().isEmpty()) {
            result.rejectValue("nom", "NotEmpty", "Le nom est obligatoire");
        }
        if (request.getPrenom() == null || request.getPrenom().trim().isEmpty()) {
            result.rejectValue("prenom", "NotEmpty", "Le prénom est obligatoire");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            result.rejectValue("email", "NotEmpty", "L'email est obligatoire");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.rejectValue("password", "NotEmpty", "Le mot de passe est obligatoire");
        }

        if (result.hasErrors()) {
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("user", user);
            return "admin/etudiants/create";
        }

        try {
            if (userService.existsByEmail(request.getEmail())) {
                model.addAttribute("error", "Cet email est déjà utilisé");
                model.addAttribute("filieres", filiereRepository.findAll());
                model.addAttribute("user", user);
                return "admin/etudiants/create";
            }

            request.setRole(Role.ERole.ROLE_ETUDIANT);
            userService.createUser(request);

            redirectAttributes.addFlashAttribute("success", "Étudiant créé avec succès");
            return "redirect:/admin/etudiants";

        } catch (Exception e) {
            log.error("Erreur création étudiant: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("user", user);
            return "admin/etudiants/create";
        }
    }

    @GetMapping("/etudiants/{id}")
    public String etudiantDetails(@PathVariable Long id, Model model,
                                  @AuthenticationPrincipal User user) {
        try {
            Etudiant etudiant = etudiantService.getEtudiantById(id);
            List<Inscription> inscriptions = inscriptionService.getInscriptionsByEtudiant(id);

            model.addAttribute("etudiant", etudiant);
            model.addAttribute("inscriptions", inscriptions);
            model.addAttribute("user", user);
            return "admin/etudiants/details";
        } catch (Exception e) {
            log.error("Error accessing student details: {}", e.getMessage(), e);
            return "redirect:/admin/etudiants";
        }
    }

    // ============ GESTION DES ENSEIGNANTS ============
    @GetMapping("/enseignants")
    public String listEnseignants(Model model, @AuthenticationPrincipal User user) {
        try {
            List<Enseignant> enseignants = enseignantService.getAllEnseignants();
            model.addAttribute("enseignants", enseignants);
            model.addAttribute("user", user);
            return "admin/enseignants/list";
        } catch (Exception e) {
            log.error("Error accessing enseignants list: {}", e.getMessage(), e);
            model.addAttribute("enseignants", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des enseignants");
            model.addAttribute("user", user);
            return "admin/enseignants/list";
        }
    }

    @GetMapping("/enseignants/create")
    public String createEnseignantForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("user", user);
        return "admin/enseignants/create";
    }

    @PostMapping("/enseignants/create")
    public String createEnseignant(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                   BindingResult result,
                                   Model model,
                                   @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {

        if (request.getNom() == null || request.getNom().trim().isEmpty()) {
            result.rejectValue("nom", "NotEmpty", "Le nom est obligatoire");
        }
        if (request.getPrenom() == null || request.getPrenom().trim().isEmpty()) {
            result.rejectValue("prenom", "NotEmpty", "Le prénom est obligatoire");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            result.rejectValue("email", "NotEmpty", "L'email est obligatoire");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.rejectValue("password", "NotEmpty", "Le mot de passe est obligatoire");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/enseignants/create";
        }

        try {
            if (userService.existsByEmail(request.getEmail())) {
                model.addAttribute("error", "Cet email est déjà utilisé");
                model.addAttribute("user", user);
                return "admin/enseignants/create";
            }

            request.setRole(Role.ERole.ROLE_ENSEIGNANT);
            userService.createUser(request);

            redirectAttributes.addFlashAttribute("success", "Enseignant créé avec succès");
            return "redirect:/admin/enseignants";

        } catch (Exception e) {
            log.error("Erreur création enseignant: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/enseignants/create";
        }
    }

    // ============ GESTION DES MODULES ============
    @GetMapping("/modules")
    public String listModules(Model model, @AuthenticationPrincipal User user) {
        try {
            List<CourseModule> modules = moduleService.getAllModules();
            model.addAttribute("modules", modules);
            model.addAttribute("user", user);
            return "admin/modules/list";
        } catch (Exception e) {
            log.error("Error accessing modules list: {}", e.getMessage(), e);
            model.addAttribute("modules", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des modules");
            model.addAttribute("user", user);
            return "admin/modules/list";
        }
    }

    @GetMapping("/modules/create")
    public String createModuleForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("moduleDTO", new ModuleDTO());
        model.addAttribute("filieres", filiereRepository.findAll());
        model.addAttribute("enseignants", enseignantService.getAllEnseignants());
        model.addAttribute("user", user);
        return "admin/modules/edit";
    }

    @PostMapping("/modules/create")
    public String createModule(@Valid @ModelAttribute("moduleDTO") ModuleDTO moduleDTO,
                               BindingResult result,
                               Model model,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("user", user);
            return "admin/modules/edit";
        }

        try {
            moduleService.createModule(moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module créé avec succès");
            return "redirect:/admin/modules";
        } catch (Exception e) {
            log.error("Erreur création module: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("user", user);
            return "admin/modules/edit";
        }
    }

    @GetMapping("/modules/edit/{id}")
    public String editModuleForm(@PathVariable Long id,
                                 Model model,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        try {
            CourseModule module = moduleService.getModuleById(id);
            ModuleDTO moduleDTO = new ModuleDTO();
            moduleDTO.setNom(module.getNom());
            moduleDTO.setSemestre(module.getSemestre());
            moduleDTO.setVolumeHoraire(module.getVolumeHoraire());
            moduleDTO.setCompetencesRequises(module.getCompetencesRequises());
            moduleDTO.setFiliereId(module.getFiliere() != null ? module.getFiliere().getId() : null);
            moduleDTO.setEnseignantId(module.getEnseignant() != null ? module.getEnseignant().getId() : null);

            model.addAttribute("moduleDTO", moduleDTO);
            model.addAttribute("moduleId", id);
            model.addAttribute("moduleCreatedAt", module.getCreatedAt());
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("user", user);
            return "admin/modules/edit";
        } catch (Exception e) {
            log.error("Error accessing edit module form: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Module non trouvé: " + e.getMessage());
            return "redirect:/admin/modules";
        }
    }

    @PostMapping("/modules/update/{id}")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute("moduleDTO") ModuleDTO moduleDTO,
                               BindingResult result,
                               Model model,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("moduleId", id);
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("user", user);
            return "admin/modules/edit";
        }

        try {
            moduleService.updateModule(id, moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module mis à jour avec succès");
            return "redirect:/admin/modules";
        } catch (Exception e) {
            log.error("Erreur mise à jour module: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("moduleId", id);
            model.addAttribute("filieres", filiereRepository.findAll());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("user", user);
            return "admin/modules/edit";
        }
    }

    @PostMapping("/modules/delete/{id}")
    public String deleteModule(@PathVariable Long id,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            moduleService.deleteModule(id);
            redirectAttributes.addFlashAttribute("success", "Module supprimé avec succès");
            return "redirect:/admin/modules";
        } catch (Exception e) {
            log.error("Erreur suppression module: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/modules";
        }
    }

    @GetMapping("/modules/{id}")
    public String moduleDetails(@PathVariable Long id, Model model,
                                @AuthenticationPrincipal User user) {
        try {
            CourseModule module = moduleService.getModuleById(id);
            List<Inscription> inscriptions = inscriptionService.getInscriptionsByModule(id);

            model.addAttribute("module", module);
            model.addAttribute("inscriptions", inscriptions);
            model.addAttribute("user", user);
            return "admin/modules/details";
        } catch (Exception e) {
            log.error("Error accessing module details: {}", e.getMessage(), e);
            return "redirect:/admin/modules";
        }
    }

    // ============ GESTION DES FILIÈRES ============
    @GetMapping("/filieres")
    public String listFilieres(Model model, @AuthenticationPrincipal User user) {
        try {
            List<Filiere> filieres = filiereService.getAllFilieres();
            model.addAttribute("filieres", filieres);
            model.addAttribute("user", user);
            return "admin/filieres/list";
        } catch (Exception e) {
            log.error("Error accessing filieres list: {}", e.getMessage(), e);
            model.addAttribute("filieres", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des filières");
            model.addAttribute("user", user);
            return "admin/filieres/list";
        }
    }

    @GetMapping("/filieres/create")
    public String createFiliereForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("filiereDTO", new FiliereDTO());
        model.addAttribute("user", user);
        return "admin/filieres/create";
    }

    @PostMapping("/filieres/create")
    public String createFiliere(@Valid @ModelAttribute("filiereDTO") FiliereDTO filiereDTO,
                                BindingResult result,
                                Model model,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/filieres/create";
        }

        try {
            filiereService.createFiliere(filiereDTO);
            redirectAttributes.addFlashAttribute("success", "Filière créée avec succès");
            return "redirect:/admin/filieres";
        } catch (Exception e) {
            log.error("Erreur création filière: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/filieres/create";
        }
    }

    @GetMapping("/filieres/edit/{id}")
    public String editFiliereForm(@PathVariable Integer id,
                                  Model model,
                                  @AuthenticationPrincipal User user,
                                  RedirectAttributes redirectAttributes) {
        try {
            Filiere filiere = filiereService.getFiliereById(id);
            FiliereDTO filiereDTO = new FiliereDTO();
            filiereDTO.setNom(filiere.getNom());
            filiereDTO.setDescription(filiere.getDescription());

            model.addAttribute("filiereDTO", filiereDTO);
            model.addAttribute("filiereId", id);
            model.addAttribute("user", user);
            return "admin/filieres/edit";
        } catch (Exception e) {
            log.error("Error accessing edit filiere form: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Filière non trouvée: " + e.getMessage());
            return "redirect:/admin/filieres";
        }
    }

    @PostMapping("/filieres/update/{id}")
    public String updateFiliere(@PathVariable Integer id,
                                @Valid @ModelAttribute("filiereDTO") FiliereDTO filiereDTO,
                                BindingResult result,
                                Model model,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("filiereId", id);
            model.addAttribute("user", user);
            return "admin/filieres/edit";
        }

        try {
            filiereService.updateFiliere(id, filiereDTO);
            redirectAttributes.addFlashAttribute("success", "Filière mise à jour avec succès");
            return "redirect:/admin/filieres";
        } catch (Exception e) {
            log.error("Erreur mise à jour filière: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("filiereId", id);
            model.addAttribute("user", user);
            return "admin/filieres/edit";
        }
    }

    @PostMapping("/filieres/delete/{id}")
    public String deleteFiliere(@PathVariable Integer id,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            filiereService.deleteFiliere(id);
            redirectAttributes.addFlashAttribute("success", "Filière supprimée avec succès");
            return "redirect:/admin/filieres";
        } catch (Exception e) {
            log.error("Erreur suppression filière: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/filieres";
        }
    }

    // ============ GESTION DES SALLES ============
    @GetMapping("/salles")
    public String listSalles(Model model,
                             @AuthenticationPrincipal User user,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) String type,
                             @RequestParam(required = false) String disponible,
                             @RequestParam(defaultValue = "0") int page) {
        log.info("=== ACCESSING SALLES LIST ===");

        try {
            List<Salle> salles = salleService.getAllSalles();

            // Filtrage
            if (search != null && !search.isEmpty()) {
                salles = salles.stream()
                        .filter(s -> s.getNom().toLowerCase().contains(search.toLowerCase()) ||
                                (s.getTypeSalle() != null &&
                                        s.getTypeSalle().toLowerCase().contains(search.toLowerCase())))
                        .collect(Collectors.toList());
            }

            if (type != null && !type.isEmpty()) {
                salles = salles.stream()
                        .filter(s -> s.getTypeSalle() != null && s.getTypeSalle().equals(type))
                        .collect(Collectors.toList());
            }

            if (disponible != null && !disponible.isEmpty()) {
                boolean isDisponible = Boolean.parseBoolean(disponible);
                salles = salles.stream()
                        .filter(s -> s.getDisponible() == isDisponible)
                        .collect(Collectors.toList());
            }

            // Pagination
            int pageSize = 10;
            int totalItems = salles.size();
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);

            int start = Math.min(page * pageSize, totalItems);
            int end = Math.min(start + pageSize, totalItems);

            List<Salle> paginatedSalles = salles.subList(start, end);

            // Convertir en DTO
            List<SalleDTO> salleDTOs = paginatedSalles.stream()
                    .map(salle -> {
                        Integer nombreSeances = salleService.getNombreSeances(salle.getId());
                        return SalleDTO.fromEntity(salle, nombreSeances);
                    })
                    .collect(Collectors.toList());

            // Statistiques
            long totalSalles = salleService.getAllSalles().size();
            long sallesDisponibles = salleService.getSallesDisponibles().size();
            long sallesOccupees = totalSalles - sallesDisponibles;

            model.addAttribute("salles", salleDTOs);
            model.addAttribute("user", user);
            model.addAttribute("search", search);
            model.addAttribute("type", type);
            model.addAttribute("disponible", disponible);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalSalles", totalSalles);
            model.addAttribute("sallesDisponibles", sallesDisponibles);
            model.addAttribute("sallesOccupees", sallesOccupees);

            return "admin/salles/list";

        } catch (Exception e) {
            log.error("Error accessing salles list: {}", e.getMessage(), e);
            model.addAttribute("salles", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des salles");
            model.addAttribute("user", user);
            return "admin/salles/list";
        }
    }

    @GetMapping("/salles/create")
    public String createSalleForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("salleDTO", new SalleDTO());
        model.addAttribute("user", user);
        return "admin/salles/create";
    }

    @PostMapping("/salles/create")
    public String createSalle(@Valid @ModelAttribute("salleDTO") SalleDTO salleDTO,
                              BindingResult result,
                              Model model,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/salles/create";
        }

        try {
            Salle salle = new Salle();
            salle.setNom(salleDTO.getNom());
            salle.setTypeSalle(salleDTO.getTypeSalle());
            salle.setCapacite(salleDTO.getCapacite());
            salle.setDisponible(salleDTO.getDisponible() != null ? salleDTO.getDisponible() : true);

            Salle savedSalle = salleService.createSalle(salle);

            redirectAttributes.addFlashAttribute("success", "Salle créée avec succès");
            return "redirect:/admin/salles";

        } catch (Exception e) {
            log.error("Erreur création salle: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/salles/create";
        }
    }

    @GetMapping("/salles/edit/{id}")
    public String editSalleForm(@PathVariable Long id,
                                Model model,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            Salle salle = salleService.getSalleById(id);
            SalleDTO salleDTO = new SalleDTO();
            salleDTO.setNom(salle.getNom());
            salleDTO.setTypeSalle(salle.getTypeSalle());
            salleDTO.setCapacite(salle.getCapacite());
            salleDTO.setDisponible(salle.getDisponible());

            Integer nombreSeances = salleService.getNombreSeances(id);

            model.addAttribute("salleDTO", salleDTO);
            model.addAttribute("salleId", id);
            model.addAttribute("nombreSeances", nombreSeances);
            model.addAttribute("user", user);
            return "admin/salles/edit";

        } catch (Exception e) {
            log.error("Error accessing edit salle form: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Salle non trouvée: " + e.getMessage());
            return "redirect:/admin/salles";
        }
    }

    @PostMapping("/salles/update/{id}")
    public String updateSalle(@PathVariable Long id,
                              @Valid @ModelAttribute("salleDTO") SalleDTO salleDTO,
                              BindingResult result,
                              Model model,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("salleId", id);
            model.addAttribute("nombreSeances", salleService.getNombreSeances(id));
            model.addAttribute("user", user);
            return "admin/salles/edit";
        }

        try {
            Salle salleDetails = new Salle();
            salleDetails.setNom(salleDTO.getNom());
            salleDetails.setTypeSalle(salleDTO.getTypeSalle());
            salleDetails.setCapacite(salleDTO.getCapacite());
            salleDetails.setDisponible(salleDTO.getDisponible());

            Salle updatedSalle = salleService.updateSalle(id, salleDetails);

            redirectAttributes.addFlashAttribute("success", "Salle mise à jour avec succès");
            return "redirect:/admin/salles";

        } catch (Exception e) {
            log.error("Erreur mise à jour salle: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("salleId", id);
            model.addAttribute("nombreSeances", salleService.getNombreSeances(id));
            model.addAttribute("user", user);
            return "admin/salles/edit";
        }
    }

    @PostMapping("/salles/delete/{id}")
    public String deleteSalle(@PathVariable Long id,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        try {
            salleService.deleteSalle(id);
            redirectAttributes.addFlashAttribute("success", "Salle supprimée avec succès");
            return "redirect:/admin/salles";

        } catch (Exception e) {
            log.error("Erreur suppression salle: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/salles";
        }
    }

    @GetMapping("/salles/{id}")
    public String salleDetails(@PathVariable Long id,
                               Model model,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            Salle salle = salleService.getSalleById(id);
            List<Seance> seances = salleRepository.findById(id)
                    .map(s -> new ArrayList<>(s.getSeances()))
                    .orElse(new ArrayList<>());

            Integer nombreSeances = salleService.getNombreSeances(id);
            SalleDTO salleDTO = SalleDTO.fromEntity(salle, nombreSeances);

            model.addAttribute("salle", salleDTO);
            model.addAttribute("seances", seances);
            model.addAttribute("user", user);
            return "admin/salles/details";

        } catch (Exception e) {
            log.error("Error accessing salle details: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Salle non trouvée: " + e.getMessage());
            return "redirect:/admin/salles";
        }
    }

    // ============ GESTION DES ADMINISTRATEURS ============
    @GetMapping("/admins")
    public String listAdmins(Model model, @AuthenticationPrincipal User user) {
        try {
            List<Admin> admins = adminService.getAllAdmins();
            model.addAttribute("admins", admins);
            model.addAttribute("user", user);
            return "admin/admins/list";
        } catch (Exception e) {
            log.error("Error accessing admins list: {}", e.getMessage(), e);
            model.addAttribute("admins", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des administrateurs");
            model.addAttribute("user", user);
            return "admin/admins/list";
        }
    }

    @GetMapping("/admins/create")
    public String createAdminForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("user", user);
        return "admin/admins/create";
    }

    @PostMapping("/admins/create")
    public String createAdmin(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                              BindingResult result,
                              Model model,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {

        // Validation
        if (request.getNom() == null || request.getNom().trim().isEmpty()) {
            result.rejectValue("nom", "NotEmpty", "Le nom est obligatoire");
        }
        if (request.getPrenom() == null || request.getPrenom().trim().isEmpty()) {
            result.rejectValue("prenom", "NotEmpty", "Le prénom est obligatoire");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            result.rejectValue("email", "NotEmpty", "L'email est obligatoire");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            result.rejectValue("password", "NotEmpty", "Le mot de passe est obligatoire");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/admins/create";
        }

        try {
            if (userService.existsByEmail(request.getEmail())) {
                model.addAttribute("error", "Cet email est déjà utilisé");
                model.addAttribute("user", user);
                return "admin/admins/create";
            }

            request.setRole(Role.ERole.ROLE_ADMIN);
            adminService.createAdmin(request);

            redirectAttributes.addFlashAttribute("success", "Administrateur créé avec succès");
            return "redirect:/admin/admins";

        } catch (Exception e) {
            log.error("Erreur création admin: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/admins/create";
        }
    }

    @GetMapping("/admins/edit/{id}")
    public String editAdminForm(@PathVariable Long id,
                                Model model,
                                @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            Admin admin = adminService.getAdminById(id);
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setNom(admin.getNom());
            registerRequest.setPrenom(admin.getPrenom());
            registerRequest.setEmail(admin.getEmail());
            registerRequest.setDepartement(admin.getDepartement());

            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("adminId", id);
            model.addAttribute("user", user);
            return "admin/admins/edit";
        } catch (Exception e) {
            log.error("Error accessing edit admin form: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Administrateur non trouvé: " + e.getMessage());
            return "redirect:/admin/admins";
        }
    }

    @PostMapping("/admins/update/{id}")
    public String updateAdmin(@PathVariable Long id,
                              @Valid @ModelAttribute("registerRequest") RegisterRequest request,
                              BindingResult result,
                              Model model,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        try {
            if (result.hasErrors()) {
                model.addAttribute("adminId", id);
                model.addAttribute("user", user);
                return "admin/admins/edit";
            }

            Admin existingAdmin = adminService.getAdminById(id);
            Admin adminDetails = new Admin();
            adminDetails.setId(id);
            adminDetails.setNom(request.getNom());
            adminDetails.setPrenom(request.getPrenom());
            adminDetails.setEmail(request.getEmail());
            adminDetails.setDepartement(request.getDepartement());

            if (!existingAdmin.getEmail().equals(request.getEmail())) {
                if (userService.existsByEmail(request.getEmail())) {
                    model.addAttribute("error", "Cet email est déjà utilisé");
                    model.addAttribute("adminId", id);
                    model.addAttribute("user", user);
                    return "admin/admins/edit";
                }
            }

            adminService.updateAdmin(id, adminDetails);

            redirectAttributes.addFlashAttribute("success", "Administrateur mis à jour avec succès");
            return "redirect:/admin/admins";

        } catch (Exception e) {
            log.error("Erreur mise à jour admin: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("adminId", id);
            model.addAttribute("user", user);
            return "admin/admins/edit";
        }
    }

    @PostMapping("/admins/delete/{id}")
    public String deleteAdmin(@PathVariable Long id,
                              @AuthenticationPrincipal User user,
                              RedirectAttributes redirectAttributes) {
        try {
            if (id.equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer votre propre compte");
                return "redirect:/admin/admins";
            }

            adminService.deleteAdmin(id);
            redirectAttributes.addFlashAttribute("success", "Administrateur supprimé avec succès");
            return "redirect:/admin/admins";

        } catch (Exception e) {
            log.error("Erreur suppression admin: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/admins";
        }
    }

    // ============ GESTION DES AFFECTATIONS ENSEIGNANTS ============
    @GetMapping("/affectations")
    public String affectations(Model model, @AuthenticationPrincipal User user) {
        try {
            List<CourseModule> modulesSansEnseignant = moduleService.getModulesSansEnseignant();
            List<Enseignant> enseignants = enseignantService.getAllEnseignants();

            model.addAttribute("modulesSansEnseignant", modulesSansEnseignant);
            model.addAttribute("enseignants", enseignants);
            model.addAttribute("user", user);
            return "admin/affectations/list";
        } catch (Exception e) {
            log.error("Error accessing affectations: {}", e.getMessage(), e);
            model.addAttribute("modulesSansEnseignant", new ArrayList<>());
            model.addAttribute("enseignants", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement des affectations");
            model.addAttribute("user", user);
            return "admin/affectations/list";
        }
    }

    @PostMapping("/affectations/affecter")
    public String affecterEnseignant(@RequestParam Long moduleId,
                                     @RequestParam Long enseignantId,
                                     @AuthenticationPrincipal User user,
                                     RedirectAttributes redirectAttributes) {
        try {
            moduleService.affecterEnseignant(moduleId, enseignantId);
            redirectAttributes.addFlashAttribute("success", "Enseignant affecté avec succès");
            return "redirect:/admin/affectations";
        } catch (Exception e) {
            log.error("Error affecting teacher: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/affectations";
        }
    }

    // ============ GESTION DE L'EMPLOI DU TEMPS ============
    @GetMapping("/emploi-temps")
    public String emploiTemps(Model model, @AuthenticationPrincipal User user) {
        try {
            List<Seance> seances = emploiTempsService.getAllSeances();
            model.addAttribute("seances", seances);
            model.addAttribute("user", user);
            return "admin/emploi-temps/list";
        } catch (Exception e) {
            log.error("Error accessing emploi-temps: {}", e.getMessage(), e);
            model.addAttribute("seances", new ArrayList<>());
            model.addAttribute("error", "Erreur lors du chargement de l'emploi du temps");
            model.addAttribute("user", user);
            return "admin/emploi-temps/list";
        }
    }

    @GetMapping("/emploi-temps/create")
    public String createSeanceForm(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("seanceDTO", new SeanceDTO());
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("enseignants", enseignantService.getAllEnseignants());
        model.addAttribute("salles", salleRepository.findAll());
        model.addAttribute("user", user);
        return "admin/emploi-temps/create";
    }

    @PostMapping("/emploi-temps/create")
    public String createSeance(@Valid @ModelAttribute("seanceDTO") SeanceDTO seanceDTO,
                               BindingResult result,
                               Model model,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            model.addAttribute("user", user);
            return "admin/emploi-temps/create";
        }

        try {
            emploiTempsService.createSeance(seanceDTO);
            redirectAttributes.addFlashAttribute("success", "Séance créée avec succès");
            return "redirect:/admin/emploi-temps";
        } catch (Exception e) {
            log.error("Erreur création séance: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            model.addAttribute("user", user);
            return "admin/emploi-temps/create";
        }
    }

    @PostMapping("/emploi-temps/delete/{id}")
    public String deleteSeance(@PathVariable Long id,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            emploiTempsService.deleteSeance(id);
            redirectAttributes.addFlashAttribute("success", "Séance supprimée avec succès");
            return "redirect:/admin/emploi-temps";
        } catch (Exception e) {
            log.error("Erreur suppression séance: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/admin/emploi-temps";
        }
    }

    // ============ PROFIL ADMIN ============
    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Admin)) {
            return "redirect:/access-denied";
        }
        model.addAttribute("user", user);
        return "admin/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Admin)) {
            return "redirect:/access-denied";
        }

        RegisterRequest request = new RegisterRequest();
        request.setNom(user.getNom());
        request.setPrenom(user.getPrenom());
        request.setEmail(user.getEmail());
        request.setDepartement(((Admin) user).getDepartement());

        model.addAttribute("registerRequest", request);
        model.addAttribute("user", user);
        return "admin/profile/edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                BindingResult result,
                                @AuthenticationPrincipal User user,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!(user instanceof Admin)) {
            return "redirect:/access-denied";
        }

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/profile/edit";
        }

        try {
            Admin adminDetails = new Admin();
            adminDetails.setId(user.getId());
            adminDetails.setNom(request.getNom());
            adminDetails.setPrenom(request.getPrenom());
            adminDetails.setEmail(request.getEmail());
            adminDetails.setDepartement(request.getDepartement());

            if (!user.getEmail().equals(request.getEmail()) &&
                    userService.existsByEmail(request.getEmail())) {
                model.addAttribute("error", "Cet email est déjà utilisé");
                model.addAttribute("user", user);
                return "admin/profile/edit";
            }

            adminService.updateAdmin(user.getId(), adminDetails);

            redirectAttributes.addFlashAttribute("success", "Profil mis à jour avec succès");
            return "redirect:/admin/profile";

        } catch (Exception e) {
            log.error("Erreur mise à jour profil: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("user", user);
            return "admin/profile/edit";
        }
    }

    // ============ MÉTHODES UTILITAIRES ============
    private String getAnneeScolaireActuelle() {
        int currentYear = LocalDateTime.now().getYear();
        int nextYear = currentYear + 1;
        return currentYear + "-" + nextYear;
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    @GetMapping("/check-email/{email}")
    @ResponseBody
    public String checkEmailAvailability(@PathVariable String email) {
        try {
            boolean exists = userService.existsByEmail(email);
            if (exists) {
                try {
                    adminService.getAdminByEmail(email);
                    return "already_admin";
                } catch (Exception e) {
                    return "already_other";
                }
            }
            return "available";
        } catch (Exception e) {
            return "error";
        }
    }

    @GetMapping("/affectations/affecter-module")
    public String affecterModuleForm(Model model, @AuthenticationPrincipal User user) {
        try {
            List<CourseModule> modules = moduleService.getAllModules();
            List<Filiere> filieres = filiereService.getAllFilieres();
            List<Enseignant> enseignants = enseignantService.getAllEnseignants();

            // Récupérer l'année en cours
            int currentYear = LocalDate.now().getYear();

            model.addAttribute("modules", modules);
            model.addAttribute("filieres", filieres);
            model.addAttribute("enseignants", enseignants);
            model.addAttribute("currentYear", currentYear);
            model.addAttribute("user", user);

            return "admin/affectations/affecter-module";

        } catch (Exception e) {
            log.error("Error loading affectation form: {}", e.getMessage(), e);
            model.addAttribute("error", "Erreur lors du chargement du formulaire");
            model.addAttribute("user", user);
            return "redirect:/admin/modules";
        }
    }

    @PostMapping("/affectations/affecter-complet")
    public String affecterComplet(@RequestParam Long moduleId,
                                  @RequestParam Integer filiereId,
                                  @RequestParam Long enseignantId,
                                  @RequestParam(required = false) String semestre,
                                  @RequestParam(required = false) String anneeScolaire,
                                  @RequestParam(required = false) String statut,
                                  @RequestParam(required = false) List<Long> etudiantIds,
                                  @AuthenticationPrincipal User user,
                                  RedirectAttributes redirectAttributes) {
        try {
            // 1. Récupérer le module
            CourseModule module = moduleService.getModuleById(moduleId);

            // 2. Affecter la filière
            Filiere filiere = filiereService.getFiliereById(filiereId);
            module.setFiliere(filiere);

            // 3. Affecter l'enseignant
            Enseignant enseignant = enseignantService.getEnseignantById(enseignantId);
            module.setEnseignant(enseignant);

            // Sauvegarder les modifications du module
            moduleRepository.save(module);

            // 4. Inscrire les étudiants sélectionnés
            int etudiantsInscrits = 0;
            if (etudiantIds != null && !etudiantIds.isEmpty()) {
                for (Long etudiantId : etudiantIds) {
                    try {
                        Etudiant etudiant = etudiantService.getEtudiantById(etudiantId);

                        // Vérifier si l'étudiant n'est pas déjà inscrit
                        if (!inscriptionRepository.existsByEtudiantIdAndModuleId(etudiantId, moduleId)) {
                            Inscription inscription = new Inscription(
                                    etudiant,
                                    module,
                                    filiere,
                                    anneeScolaire != null ? anneeScolaire : getAnneeScolaireActuelle(),
                                    semestre != null ? semestre : module.getSemestre(),
                                    etudiant.getNiveau()
                            );
                            inscription.setStatut(statut != null ? statut : "ACTIVE");
                            inscriptionRepository.save(inscription);
                            etudiantsInscrits++;
                        }
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'inscription de l'étudiant {}: {}", etudiantId, e.getMessage());
                    }
                }
            }

            // Message de succès
            StringBuilder message = new StringBuilder();
            message.append("Module ").append(module.getNom()).append(" affecté avec succès !");
            message.append("\n• Filière : ").append(filiere.getNom());
            message.append("\n• Enseignant : ").append(enseignant.getNom()).append(" ").append(enseignant.getPrenom());

            if (etudiantsInscrits > 0) {
                message.append("\n• ").append(etudiantsInscrits).append(" étudiant(s) inscrit(s)");
            }

            redirectAttributes.addFlashAttribute("success", message.toString());
            return "redirect:/admin/modules";

        } catch (Exception e) {
            log.error("Error in complete affectation: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'affectation : " + e.getMessage());
            return "redirect:/admin/affectations/affecter-module";
        }
    }
}