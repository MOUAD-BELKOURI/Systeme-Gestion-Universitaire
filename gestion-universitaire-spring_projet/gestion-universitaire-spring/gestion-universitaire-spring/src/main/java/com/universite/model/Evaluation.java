package com.universite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "evaluations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "note_tp")
    private Double noteTP;

    @Column(name = "note_ds")
    private Double noteDS;

    @Column(name = "note_projet")
    private Double noteProjet;

    @Column(name = "participation")
    private Double participation;

    @Column(name = "note_finale")
    private Double noteFinale;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "date_evaluation")
    private LocalDate dateEvaluation;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        if (dateEvaluation == null) {
            dateEvaluation = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
        calculerNoteFinale();
    }

    @PreUpdate
    protected void onUpdate() {
        calculerNoteFinale();
    }

    public void calculerNoteFinale() {
        double total = 0;
        int count = 0;

        if (noteTP != null) {
            total += noteTP * 0.2;
            count++;
        }
        if (noteDS != null) {
            total += noteDS * 0.4;
            count++;
        }
        if (noteProjet != null) {
            total += noteProjet * 0.3;
            count++;
        }
        if (participation != null) {
            total += participation * 0.1;
            count++;
        }

        if (count > 0) {
            noteFinale = total;
        }
    }
}