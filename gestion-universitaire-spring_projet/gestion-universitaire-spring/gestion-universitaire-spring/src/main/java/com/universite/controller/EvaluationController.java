package com.universite.controller;

import com.universite.dto.EvaluationDTO;
import com.universite.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    public String listEvaluations(Model model) {
        var evaluations = evaluationService.getAllEvaluations();
        model.addAttribute("evaluations", evaluations);
        return "evaluations/list";
    }

    @GetMapping("/{id}")
    public String viewEvaluation(@PathVariable Long id, Model model) {
        var evaluation = evaluationService.getEvaluationById(id);
        model.addAttribute("evaluation", evaluation);
        return "evaluations/view";
    }

    @GetMapping("/create")
    public String createEvaluationForm(Model model) {
        model.addAttribute("evaluationDTO", new EvaluationDTO());
        return "evaluations/create";
    }

    @PostMapping("/create")
    public String createEvaluation(@Valid @ModelAttribute EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "evaluations/create";
        }

        try {
            evaluationService.createEvaluation(evaluationDTO);
            redirectAttributes.addFlashAttribute("success", "Évaluation créée avec succès");
            return "redirect:/evaluations";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "evaluations/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editEvaluationForm(@PathVariable Long id, Model model) {
        var evaluation = evaluationService.getEvaluationById(id);
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
        return "evaluations/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateEvaluation(@PathVariable Long id,
                                   @Valid @ModelAttribute EvaluationDTO evaluationDTO,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "evaluations/edit";
        }

        try {
            evaluationService.updateEvaluation(id, evaluationDTO);
            redirectAttributes.addFlashAttribute("success", "Évaluation mise à jour avec succès");
            return "redirect:/evaluations";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "evaluations/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteEvaluation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            evaluationService.deleteEvaluation(id);
            redirectAttributes.addFlashAttribute("success", "Évaluation supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/evaluations";
    }
}