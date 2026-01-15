package com.universite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "salles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "capacite")
    private Integer capacite;

    @Column(name = "type_salle")
    private String typeSalle;

    @Column(name = "disponible")
    private Boolean disponible = true;

    @JsonIgnore
    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Seance> seances = new HashSet<>();

    // Méthode helper pour vérifier si la salle est utilisée
    public boolean isUtilisee() {
        return !seances.isEmpty();
    }

    // Méthode pour obtenir le nombre de séances
    public int getNombreSeances() {
        return seances.size();
    }
}