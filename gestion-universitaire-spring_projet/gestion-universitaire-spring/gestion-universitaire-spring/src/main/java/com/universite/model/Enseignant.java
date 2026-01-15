package com.universite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "enseignants")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"modules", "seances"})
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Enseignant extends User {

    @Column(name = "specialite")
    private String specialite;

    @Column(name = "grade")
    private String grade;

    @Column(name = "charge_horaire")
    private Integer chargeHoraire;

    @Column(name = "competences", columnDefinition = "TEXT")
    private String competences;

    @OneToMany(mappedBy = "enseignant", fetch = FetchType.LAZY)
    private Set<CourseModule> modules = new HashSet<>();

    @OneToMany(mappedBy = "enseignant", fetch = FetchType.LAZY)
    private Set<Seance> seances = new HashSet<>();

    public Enseignant(User user, String specialite, String grade, Integer chargeHoraire, String competences) {
        this.setId(user.getId());
        this.setNom(user.getNom());
        this.setPrenom(user.getPrenom());
        this.setEmail(user.getEmail());
        this.setPassword(user.getPassword());
        this.setRole(user.getRole());
        this.setCreatedAt(user.getCreatedAt());
        this.setUpdatedAt(user.getUpdatedAt());
        this.specialite = specialite;
        this.grade = grade;
        this.chargeHoraire = chargeHoraire;
        this.competences = competences;
    }
}