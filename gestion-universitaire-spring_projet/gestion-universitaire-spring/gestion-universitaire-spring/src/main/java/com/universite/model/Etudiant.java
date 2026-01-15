package com.universite.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "etudiants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Etudiant extends User {

    @ManyToOne
    @JoinColumn(name = "filiere_id")
    private Filiere filiere;

    @Column(name = "niveau")
    private String niveau;

    @Column(name = "groupe")
    private String groupe;

    @Column(name = "competences", columnDefinition = "TEXT")
    private String competences;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "module_etudiants",
            joinColumns = @JoinColumn(name = "etudiant_id"),
            inverseJoinColumns = @JoinColumn(name = "module_id")
    )
    private Set<CourseModule> modules = new HashSet<>();

    @OneToMany(mappedBy = "etudiant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Evaluation> evaluations = new HashSet<>();

    public Etudiant(User user, Filiere filiere, String niveau, String groupe, String competences) {
        this.setId(user.getId());
        this.setNom(user.getNom());
        this.setPrenom(user.getPrenom());
        this.setEmail(user.getEmail());
        this.setPassword(user.getPassword());
        this.setRole(user.getRole());
        this.setCreatedAt(user.getCreatedAt());
        this.setUpdatedAt(user.getUpdatedAt());
        this.filiere = filiere;
        this.niveau = niveau;
        this.groupe = groupe;
        this.competences = competences;
    }

    @Override
    public String toString() {
        return "Etudiant{" +
                "id=" + getId() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", niveau='" + niveau + '\'' +
                ", groupe='" + groupe + '\'' +
                '}';
    }
}