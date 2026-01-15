package com.universite.controller;

import com.universite.dto.SeanceDTO;
import com.universite.model.Seance;
import com.universite.service.EmploiTempsService;
import com.universite.service.ModuleService;
import com.universite.service.EnseignantService;
import com.universite.repository.SalleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/emploi-temps")
@RequiredArgsConstructor
public class EmploiTempsController {

    private final EmploiTempsService emploiTempsService;
    private final ModuleService moduleService;
    private final EnseignantService enseignantService;
    private final SalleRepository salleRepository;

    @GetMapping
    public String listSeances(Model model) {
        List<Seance> seances = emploiTempsService.getAllSeances();
        model.addAttribute("seances", seances);
        return "emploi-temps/list";
    }

    @GetMapping("/{id}")
    public String viewSeance(@PathVariable Long id, Model model) {
        var seance = emploiTempsService.getSeanceById(id);
        model.addAttribute("seance", seance);
        return "emploi-temps/view";
    }

    @GetMapping("/create")
    public String createSeanceForm(Model model) {
        model.addAttribute("seanceDTO", new SeanceDTO());
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("enseignants", enseignantService.getAllEnseignants());
        model.addAttribute("salles", salleRepository.findAll());
        return "emploi-temps/create";
    }

    @PostMapping("/create")
    public String createSeance(@Valid @ModelAttribute SeanceDTO seanceDTO,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            return "emploi-temps/create";
        }

        try {
            emploiTempsService.createSeance(seanceDTO);
            redirectAttributes.addFlashAttribute("success", "Séance créée avec succès");
            return "redirect:/emploi-temps";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            return "emploi-temps/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editSeanceForm(@PathVariable Long id, Model model) {
        var seance = emploiTempsService.getSeanceById(id);
        SeanceDTO seanceDTO = new SeanceDTO();
        seanceDTO.setId(seance.getId());
        seanceDTO.setModuleId(seance.getModule().getId());
        seanceDTO.setEnseignantId(seance.getEnseignant().getId());
        seanceDTO.setGroupe(seance.getGroupe());
        seanceDTO.setSalleId(seance.getSalle().getId());
        seanceDTO.setDateHeure(seance.getDateHeure());
        seanceDTO.setDuree(seance.getDuree());
        seanceDTO.setTypeSeance(seance.getTypeSeance());

        model.addAttribute("seanceDTO", seanceDTO);
        model.addAttribute("modules", moduleService.getAllModules());
        model.addAttribute("enseignants", enseignantService.getAllEnseignants());
        model.addAttribute("salles", salleRepository.findAll());

        return "emploi-temps/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateSeance(@PathVariable Long id,
                               @Valid @ModelAttribute SeanceDTO seanceDTO,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            return "emploi-temps/edit";
        }

        try {
            emploiTempsService.updateSeance(id, seanceDTO);
            redirectAttributes.addFlashAttribute("success", "Séance mise à jour avec succès");
            return "redirect:/emploi-temps";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("modules", moduleService.getAllModules());
            model.addAttribute("enseignants", enseignantService.getAllEnseignants());
            model.addAttribute("salles", salleRepository.findAll());
            return "emploi-temps/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSeance(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            emploiTempsService.deleteSeance(id);
            redirectAttributes.addFlashAttribute("success", "Séance supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/emploi-temps";
    }

    @GetMapping("/enseignant/{enseignantId}")
    public String getSeancesByEnseignant(@PathVariable Long enseignantId, Model model) {
        List<Seance> seances = emploiTempsService.getSeancesByEnseignant(enseignantId);
        model.addAttribute("seances", seances);
        return "emploi-temps/enseignant";
    }

    @GetMapping("/groupe/{groupe}")
    public String getSeancesByGroupe(@PathVariable String groupe, Model model) {
        List<Seance> seances = emploiTempsService.getSeancesByGroupe(groupe);
        model.addAttribute("seances", seances);
        return "emploi-temps/groupe";
    }

    @GetMapping("/semaine")
    public String getSeancesSemaine(Model model) {
        LocalDateTime startOfWeek = LocalDateTime.now().with(java.time.DayOfWeek.MONDAY);
        List<Seance> seances = emploiTempsService.getSeancesPourSemaine(startOfWeek);
        model.addAttribute("seances", seances);
        model.addAttribute("debutSemaine", startOfWeek.toLocalDate());
        return "emploi-temps/semaine";
    }
}