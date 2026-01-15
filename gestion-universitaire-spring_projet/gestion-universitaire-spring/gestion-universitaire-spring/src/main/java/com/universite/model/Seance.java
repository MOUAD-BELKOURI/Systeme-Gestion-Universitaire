package com.universite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "seances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @ManyToOne
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @Column(name = "groupe", nullable = false)
    private String groupe;

    @ManyToOne
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(name = "duree", nullable = false)
    private Integer duree; // en minutes

    @Column(name = "type_seance")
    private String typeSeance; // CM, TD, TP

    // 🔹 Date de fin persistée (OBLIGATOIRE pour JPQL)
    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 🔹 Avant insertion
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculerDateFin();
    }

    // 🔹 Avant mise à jour
    @PreUpdate
    protected void onUpdate() {
        calculerDateFin();
    }

    // 🔹 Calcul automatique de la date de fin
    private void calculerDateFin() {
        if (dateHeure != null && duree != null) {
            this.dateFin = dateHeure.plusMinutes(duree);
        }
    }
}
