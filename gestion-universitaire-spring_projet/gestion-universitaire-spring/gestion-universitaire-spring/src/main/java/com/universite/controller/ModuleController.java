package com.universite.controller;

import com.universite.dto.ModuleDTO;
import com.universite.model.CourseModule;
import com.universite.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping
    public String listModules(Model model) {
        List<CourseModule> modules = moduleService.getAllModules();
        model.addAttribute("modules", modules);
        return "modules/list";
    }

    @GetMapping("/{id}")
    public String viewModule(@PathVariable Long id, Model model) {
        CourseModule module = moduleService.getModuleById(id);
        model.addAttribute("module", module);
        return "modules/view";
    }

    @GetMapping("/create")
    public String createModuleForm(Model model) {
        model.addAttribute("moduleDTO", new ModuleDTO());
        return "modules/create";
    }

    @PostMapping("/create")
    public String createModule(@Valid @ModelAttribute ModuleDTO moduleDTO,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "modules/create";
        }

        try {
            moduleService.createModule(moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module créé avec succès");
            return "redirect:/modules";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "modules/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editModuleForm(@PathVariable Long id, Model model) {
        CourseModule module = moduleService.getModuleById(id);
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setId(module.getId());
        moduleDTO.setNom(module.getNom());
        moduleDTO.setFiliereId(module.getFiliere().getId());
        moduleDTO.setSemestre(module.getSemestre());
        moduleDTO.setVolumeHoraire(module.getVolumeHoraire());
        moduleDTO.setCompetencesRequises(module.getCompetencesRequises());
        moduleDTO.setEnseignantId(module.getEnseignant() != null ? module.getEnseignant().getId() : null);

        model.addAttribute("moduleDTO", moduleDTO);
        return "modules/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateModule(@PathVariable Long id,
                               @Valid @ModelAttribute ModuleDTO moduleDTO,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "modules/edit";
        }

        try {
            moduleService.updateModule(id, moduleDTO);
            redirectAttributes.addFlashAttribute("success", "Module mis à jour avec succès");
            return "redirect:/modules";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "modules/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            moduleService.deleteModule(id);
            redirectAttributes.addFlashAttribute("success", "Module supprimé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/modules";
    }
}