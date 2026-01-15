package com.universite.service;

import com.universite.dto.EvaluationDTO;
import com.universite.model.Evaluation;

import java.util.List;

public interface EvaluationService {
    Evaluation createEvaluation(EvaluationDTO evaluationDTO);
    Evaluation updateEvaluation(Long id, EvaluationDTO evaluationDTO);
    void deleteEvaluation(Long id);
    Evaluation getEvaluationById(Long id);
    List<Evaluation> getAllEvaluations();
    List<Evaluation> getEvaluationsByEtudiant(Long etudiantId);
    List<Evaluation> getEvaluationsByModule(Long moduleId);
    Evaluation getEvaluationByEtudiantAndModule(Long etudiantId, Long moduleId);
    List<Evaluation> getEvaluationsByEtudiantAndModule(Long etudiantId, Long moduleId); // Ajout de cette méthode
    Double getMoyenneEtudiant(Long etudiantId);
    Double getMoyenneModule(Long moduleId);
}