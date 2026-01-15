package com.universite.repository;

import com.universite.model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    List<Inscription> findByEtudiantId(Long etudiantId);

    List<Inscription> findByModuleId(Long moduleId);

    List<Inscription> findByFiliereIdAndNiveau(Integer filiereId, String niveau);

    List<Inscription> findBySemestre(String semestre);

    Optional<Inscription> findByEtudiantIdAndModuleId(Long etudiantId, Long moduleId);

    boolean existsByEtudiantIdAndModuleId(Long etudiantId, Long moduleId);

    long countByFiliereId(Integer filiereId);

    long countByModuleId(Long moduleId);

    @Query("SELECT i FROM Inscription i WHERE i.etudiant.id = :etudiantId AND i.semestre = :semestre")
    List<Inscription> findByEtudiantIdAndSemestre(Long etudiantId, String semestre);

    @Query("SELECT i FROM Inscription i WHERE i.filiere.id = :filiereId AND i.anneeScolaire = :anneeScolaire")
    List<Inscription> findByFiliereIdAndAnneeScolaire(Integer filiereId, String anneeScolaire);

    // Ajout de la méthode manquante
    boolean existsByEtudiantId(Long etudiantId);

    // Optionnel : méthode pour vérifier l'existence par module
    boolean existsByModuleId(Long moduleId);
}