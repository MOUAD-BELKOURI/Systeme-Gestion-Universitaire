package com.universite.controller;

import com.universite.dto.EvaluationDTO;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.*;
import com.universite.service.EvaluationService;
import com.universite.service.ModuleService;
import com.universite.service.EmploiTempsService;
import com.universite.service.EtudiantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/enseignant")
@RequiredArgsConstructor
public class EnseignantController {

    private final ModuleService moduleService;
    private final EmploiTempsService emploiTempsService;
    private final EvaluationService evaluationService;
    private final EtudiantService etudiantService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        model.addAttribute("user", enseignant);

        // Récupérer les modules de l'enseignant
        List<CourseModule> modules = moduleService.getModulesByEnseignant(enseignant.getId());
        model.addAttribute("modules", modules);

        // Récupérer les séances de l'enseignant
        List<Seance> seances = emploiTempsService.getSeancesByEnseignant(enseignant.getId());
        model.addAttribute("seances", seances);

        return "enseignant/dashboard";
    }

    @GetMapping("/modules")
    public String listModules(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        List<CourseModule> modules = moduleService.getModulesByEnseignant(enseignant.getId());
        model.addAttribute("modules", modules);
        model.addAttribute("user", enseignant);

        return "enseignant/modules/list";
    }

    @GetMapping("/modules/{id}")
    public String viewModule(@AuthenticationPrincipal User user,
                             @PathVariable Long id,
                             Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        CourseModule module = moduleService.getModuleById(id);
        List<Seance> seances = emploiTempsService.getSeancesByEnseignant(user.getId())
                .stream()
                .filter(s -> s.getModule().getId().equals(id))
                .toList();

        model.addAttribute("module", module);
        model.addAttribute("seances", seances);
        model.addAttribute("etudiants", module.getEtudiants());
        model.addAttribute("user", enseignant);

        return "enseignant/modules/view";
    }

    @GetMapping("/emploi-temps")
    public String emploiTemps(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        List<Seance> seances = emploiTempsService.getSeancesByEnseignant(enseignant.getId());
        model.addAttribute("seances", seances);
        model.addAttribute("user", enseignant);

        return "enseignant/emploi-temps/list";
    }

    @GetMapping("/evaluations")
    public String listEvaluations(@AuthenticationPrincipal User user, Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        List<CourseModule> modules = moduleService.getModulesByEnseignant(enseignant.getId());
        model.addAttribute("modules", modules);
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/list";
    }

    @GetMapping("/evaluations/module/{moduleId}")
    public String evaluationsModule(@AuthenticationPrincipal User user,
                                    @PathVariable Long moduleId,
                                    Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        CourseModule module = moduleService.getModuleById(moduleId);
        List<Evaluation> evaluations = evaluationService.getEvaluationsByModule(moduleId);

        // Create evaluations map for easy lookup by student ID
        Map<Long, Evaluation> evaluationsMap = evaluations.stream()
                .collect(Collectors.toMap(eval -> eval.getEtudiant().getId(), eval -> eval));

        model.addAttribute("module", module);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("evaluationsMap", evaluationsMap);
        model.addAttribute("etudiants", module.getEtudiants());
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/module";
    }

    @GetMapping("/evaluations/create/{moduleId}/{etudiantId}")
    public String createEvaluationForm(@AuthenticationPrincipal User user,
                                       @PathVariable Long moduleId,
                                       @PathVariable Long etudiantId,
                                       Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        CourseModule module = moduleService.getModuleById(moduleId);
        Etudiant etudiant = module.getEtudiants().stream()
                .filter(e -> e.getId().equals(etudiantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Étudiant non trouvé dans ce module"));

        EvaluationDTO evaluationDTO = new EvaluationDTO();
        evaluationDTO.setEtudiantId(etudiantId);
        evaluationDTO.setModuleId(moduleId);

        model.addAttribute("evaluationDTO", evaluationDTO);
        model.addAttribute("module", module);
        model.addAttribute("etudiant", etudiant);
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/create";
    }

    @PostMapping("/evaluations/create")
    public String createEvaluation(@AuthenticationPrincipal User user,
                                   @Valid @ModelAttribute EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        if (result.hasErrors()) {
            CourseModule module = moduleService.getModuleById(evaluationDTO.getModuleId());
            Etudiant etudiant = module.getEtudiants().stream()
                    .filter(e -> e.getId().equals(evaluationDTO.getEtudiantId()))
                    .findFirst()
                    .orElse(null);

            model.addAttribute("module", module);
            model.addAttribute("etudiant", etudiant);
            model.addAttribute("user", enseignant);
            return "enseignant/evaluations/create";
        }

        try {
            evaluationService.createEvaluation(evaluationDTO);
            redirectAttributes.addFlashAttribute("success", "Évaluation créée avec succès");
            return "redirect:/enseignant/evaluations/module/" + evaluationDTO.getModuleId();
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());

            CourseModule module = moduleService.getModuleById(evaluationDTO.getModuleId());
            Etudiant etudiant = module.getEtudiants().stream()
                    .filter(et -> et.getId().equals(evaluationDTO.getEtudiantId()))
                    .findFirst()
                    .orElse(null);

            model.addAttribute("module", module);
            model.addAttribute("etudiant", etudiant);
            model.addAttribute("user", enseignant);
            return "enseignant/evaluations/create";
        }
    }

    @GetMapping("/evaluations/edit/{id}")
    public String editEvaluationForm(@AuthenticationPrincipal User user,
                                     @PathVariable Long id,
                                     Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        Evaluation evaluation = evaluationService.getEvaluationById(id);
        EvaluationDTO evaluationDTO = new EvaluationDTO();
        evaluationDTO.setId(evaluation.getId());
        evaluationDTO.setEtudiantId(evaluation.getEtudiant().getId());
        evaluationDTO.setModuleId(evaluation.getModule().getId());
        evaluationDTO.setNoteTP(evaluation.getNoteTP());
        evaluationDTO.setNoteDS(evaluation.getNoteDS());
        evaluationDTO.setNoteProjet(evaluation.getNoteProjet());
        evaluationDTO.setParticipation(evaluation.getParticipation());
        evaluationDTO.setFeedback(evaluation.getFeedback());

        model.addAttribute("evaluationDTO", evaluationDTO);
        model.addAttribute("module", evaluation.getModule());
        model.addAttribute("etudiant", evaluation.getEtudiant());
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/edit";
    }

    @PostMapping("/evaluations/edit/{id}")
    public String updateEvaluation(@AuthenticationPrincipal User user,
                                   @PathVariable Long id,
                                   @Valid @ModelAttribute EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        if (result.hasErrors()) {
            Evaluation evaluation = evaluationService.getEvaluationById(id);
            model.addAttribute("module", evaluation.getModule());
            model.addAttribute("etudiant", evaluation.getEtudiant());
            model.addAttribute("user", enseignant);
            return "enseignant/evaluations/edit";
        }

        try {
            evaluationService.updateEvaluation(id, evaluationDTO);
            redirectAttributes.addFlashAttribute("success", "Évaluation mise à jour avec succès");
            return "redirect:/enseignant/evaluations/module/" + evaluationDTO.getModuleId();
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());

            Evaluation evaluation = evaluationService.getEvaluationById(id);
            model.addAttribute("module", evaluation.getModule());
            model.addAttribute("etudiant", evaluation.getEtudiant());
            model.addAttribute("user", enseignant);
            return "enseignant/evaluations/edit";
        }
    }

    @GetMapping("/evaluations/student/{etudiantId}")
    public String evaluationsByStudent(@AuthenticationPrincipal User user,
                                       @PathVariable Long etudiantId,
                                       Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        // Récupérer l'étudiant
        Etudiant etudiant = evaluationService.getEvaluationsByEtudiant(etudiantId).stream()
                .findFirst()
                .map(Evaluation::getEtudiant)
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", etudiantId));

        // Récupérer tous les modules de l'enseignant
        List<CourseModule> modulesEnseignant = moduleService.getModulesByEnseignant(enseignant.getId());

        // Récupérer les évaluations de l'étudiant pour les modules de cet enseignant
        List<Evaluation> evaluations = evaluationService.getEvaluationsByEtudiant(etudiantId).stream()
                .filter(evaluation -> modulesEnseignant.stream()
                        .anyMatch(module -> module.getId().equals(evaluation.getModule().getId())))
                .collect(Collectors.toList());

        // Créer une map des évaluations par module
        Map<Long, Evaluation> evaluationsMap = evaluations.stream()
                .collect(Collectors.toMap(eval -> eval.getModule().getId(), eval -> eval));

        // Créer une liste des modules avec leur statut d'évaluation
        List<Map<String, Object>> modulesWithStatus = modulesEnseignant.stream()
                .map(module -> {
                    Map<String, Object> moduleData = new java.util.HashMap<>();
                    moduleData.put("module", module);
                    moduleData.put("evaluation", evaluationsMap.get(module.getId()));
                    moduleData.put("hasEvaluation", evaluationsMap.containsKey(module.getId()));
                    return moduleData;
                })
                .collect(Collectors.toList());

        model.addAttribute("etudiant", etudiant);
        model.addAttribute("modulesWithStatus", modulesWithStatus);
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/student";
    }

    @GetMapping("/evaluations/student/{etudiantId}/module/{moduleId}/add")
    public String addGradeForStudent(@AuthenticationPrincipal User user,
                                     @PathVariable Long etudiantId,
                                     @PathVariable Long moduleId,
                                     Model model) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        CourseModule module = moduleService.getModuleById(moduleId);
        Etudiant etudiant = module.getEtudiants().stream()
                .filter(e -> e.getId().equals(etudiantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Étudiant non trouvé dans ce module"));

        EvaluationDTO evaluationDTO = new EvaluationDTO();
        evaluationDTO.setEtudiantId(etudiantId);
        evaluationDTO.setModuleId(moduleId);

        model.addAttribute("evaluationDTO", evaluationDTO);
        model.addAttribute("module", module);
        model.addAttribute("etudiant", etudiant);
        model.addAttribute("user", enseignant);

        return "enseignant/evaluations/add-grade";
    }

    @PostMapping("/modules/{moduleId}/add-grade-by-id")
    public String addGradeByStudentId(@AuthenticationPrincipal User user,
                                      @PathVariable Long moduleId,
                                      @RequestParam("studentId") Long studentId,
                                      RedirectAttributes redirectAttributes) {
        if (!(user instanceof Enseignant enseignant)) {
            return "redirect:/access-denied";
        }

        try {
            CourseModule module = moduleService.getModuleById(moduleId);

            // Check if the student is enrolled in this module
            Etudiant etudiant = module.getEtudiants().stream()
                    .filter(e -> e.getId().equals(studentId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Étudiant avec ID " + studentId + " non trouvé dans ce module"));

            // Redirect to the create evaluation form for this student
            return "redirect:/enseignant/evaluations/create/" + moduleId + "/" + studentId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/enseignant/modules/" + moduleId;
        }
    }
}
