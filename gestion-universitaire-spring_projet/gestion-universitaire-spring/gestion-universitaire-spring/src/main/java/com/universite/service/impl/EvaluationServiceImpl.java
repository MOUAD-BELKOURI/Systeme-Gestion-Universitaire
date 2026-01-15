package com.universite.service.impl;

import com.universite.dto.EvaluationDTO;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.CourseModule;
import com.universite.model.Etudiant;
import com.universite.model.Evaluation;
import com.universite.repository.EtudiantRepository;
import com.universite.repository.EvaluationRepository;
import com.universite.repository.ModuleRepository;
import com.universite.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EtudiantRepository etudiantRepository;
    private final ModuleRepository moduleRepository;

    @Override
    @Transactional
    public Evaluation createEvaluation(EvaluationDTO evaluationDTO) {
        Etudiant etudiant = etudiantRepository.findById(evaluationDTO.getEtudiantId())
                .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", evaluationDTO.getEtudiantId()));
        CourseModule module = moduleRepository.findById(evaluationDTO.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", evaluationDTO.getModuleId()));

        // Vérifier si une évaluation existe déjà
        evaluationRepository.findByEtudiantAndModule(etudiant.getId(), module.getId())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Une évaluation existe déjà pour cet étudiant dans ce module");
                });

        Evaluation evaluation = new Evaluation();
        evaluation.setEtudiant(etudiant);
        evaluation.setModule(module);
        evaluation.setNoteTP(evaluationDTO.getNoteTP());
        evaluation.setNoteDS(evaluationDTO.getNoteDS());
        evaluation.setNoteProjet(evaluationDTO.getNoteProjet());
        evaluation.setParticipation(evaluationDTO.getParticipation());
        evaluation.setFeedback(evaluationDTO.getFeedback());

        return evaluationRepository.save(evaluation);
    }

    @Override
    public List<Evaluation> getEvaluationsByEtudiantAndModule(Long etudiantId, Long moduleId) {
        return evaluationRepository.findByEtudiantId(etudiantId).stream()
                .filter(evaluation -> evaluation.getModule() != null && evaluation.getModule().getId().equals(moduleId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Evaluation updateEvaluation(Long id, EvaluationDTO evaluationDTO) {
        Evaluation evaluation = getEvaluationById(id);

        // Vérifier si l'étudiant ou le module ont changé
        if (!evaluation.getEtudiant().getId().equals(evaluationDTO.getEtudiantId()) ||
                !evaluation.getModule().getId().equals(evaluationDTO.getModuleId())) {

            // Vérifier si une autre évaluation existe déjà pour le nouvel étudiant/module
            evaluationRepository.findByEtudiantAndModule(evaluationDTO.getEtudiantId(), evaluationDTO.getModuleId())
                    .ifPresent(e -> {
                        if (!e.getId().equals(id)) {
                            throw new IllegalArgumentException("Une évaluation existe déjà pour cet étudiant dans ce module");
                        }
                    });

            Etudiant etudiant = etudiantRepository.findById(evaluationDTO.getEtudiantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Étudiant", "id", evaluationDTO.getEtudiantId()));
            CourseModule module = moduleRepository.findById(evaluationDTO.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Module", "id", evaluationDTO.getModuleId()));

            evaluation.setEtudiant(etudiant);
            evaluation.setModule(module);
        }

        evaluation.setNoteTP(evaluationDTO.getNoteTP());
        evaluation.setNoteDS(evaluationDTO.getNoteDS());
        evaluation.setNoteProjet(evaluationDTO.getNoteProjet());
        evaluation.setParticipation(evaluationDTO.getParticipation());
        evaluation.setFeedback(evaluationDTO.getFeedback());

        return evaluationRepository.save(evaluation);
    }

    @Override
    public void deleteEvaluation(Long id) {
        Evaluation evaluation = getEvaluationById(id);
        evaluationRepository.delete(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation", "id", id));
    }

    @Override
    public List<Evaluation> getAllEvaluations() {
        return evaluationRepository.findAll();
    }

    @Override
    public List<Evaluation> getEvaluationsByEtudiant(Long etudiantId) {
        return evaluationRepository.findByEtudiantId(etudiantId);
    }

    @Override
    public List<Evaluation> getEvaluationsByModule(Long moduleId) {
        return evaluationRepository.findByModuleId(moduleId);
    }

    @Override
    public Evaluation getEvaluationByEtudiantAndModule(Long etudiantId, Long moduleId) {
        return evaluationRepository.findByEtudiantAndModule(etudiantId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation", "étudiant et module", etudiantId + "-" + moduleId));
    }

    @Override
    public Double getMoyenneEtudiant(Long etudiantId) {
        List<Evaluation> evaluations = getEvaluationsByEtudiant(etudiantId);
        if (evaluations.isEmpty()) {
            return 0.0;
        }

        double somme = evaluations.stream()
                .mapToDouble(e -> e.getNoteFinale() != null ? e.getNoteFinale() : 0)
                .sum();

        return somme / evaluations.size();
    }

    @Override
    public Double getMoyenneModule(Long moduleId) {
        List<Evaluation> evaluations = getEvaluationsByModule(moduleId);
        if (evaluations.isEmpty()) {
            return 0.0;
        }

        double somme = evaluations.stream()
                .mapToDouble(e -> e.getNoteFinale() != null ? e.getNoteFinale() : 0)
                .sum();

        return somme / evaluations.size();
    }
}