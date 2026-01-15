package com.universite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"etudiant_id", "module_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filiere_id", nullable = false)
    private Filiere filiere;

    @Column(name = "annee_scolaire", nullable = false)
    private String anneeScolaire;

    @Column(name = "semestre", nullable = false)
    private String semestre;

    @Column(name = "niveau", nullable = false)
    private String niveau;

    @Column(name = "statut")
    private String statut = "ACTIVE"; // ACTIVE, SUSPENDED, COMPLETED

    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (dateInscription == null) {
            dateInscription = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Constructeur simplifié
    public Inscription(Etudiant etudiant, CourseModule module, Filiere filiere,
                       String anneeScolaire, String semestre, String niveau) {
        this.etudiant = etudiant;
        this.module = module;
        this.filiere = filiere;
        this.anneeScolaire = anneeScolaire;
        this.semestre = semestre;
        this.niveau = niveau;
        this.statut = "ACTIVE";
    }
}