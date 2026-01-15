package com.universite.dto;

import com.universite.model.CourseModule;
import com.universite.model.Evaluation;
import lombok.Data;

@Data
public class ModuleWithEvaluation {
    private CourseModule module;
    private Evaluation evaluation;
    private boolean hasEvaluation;

    // Getters pratiques pour le template
    public Double getNoteTP() {
        return hasEvaluation && evaluation != null ? evaluation.getNoteTP() : null;
    }

    public Double getNoteDS() {
        return hasEvaluation && evaluation != null ? evaluation.getNoteDS() : null;
    }

    public Double getNoteProjet() {
        return hasEvaluation && evaluation != null ? evaluation.getNoteProjet() : null;
    }

    public Double getParticipation() {
        return hasEvaluation && evaluation != null ? evaluation.getParticipation() : null;
    }

    public Double getNoteFinale() {
        return hasEvaluation && evaluation != null ? evaluation.getNoteFinale() : null;
    }

    public String getFeedback() {
        return hasEvaluation && evaluation != null ? evaluation.getFeedback() : null;
    }

    public java.time.LocalDate getDateEvaluation() {
        return hasEvaluation && evaluation != null ? evaluation.getDateEvaluation() : null;
    }
}