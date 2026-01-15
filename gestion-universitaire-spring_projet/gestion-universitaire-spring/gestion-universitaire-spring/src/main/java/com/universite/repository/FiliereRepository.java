package com.universite.repository;

import com.universite.model.Filiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FiliereRepository extends JpaRepository<Filiere, Integer> {
    boolean existsByNom(String nom);
    Filiere findByNom(String nom);
}