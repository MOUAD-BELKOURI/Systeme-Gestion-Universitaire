package com.universite.repository;

import com.universite.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByEtudiantId(Long etudiantId);
    List<Evaluation> findByModuleId(Long moduleId);

    @Query("SELECT e FROM Evaluation e WHERE e.etudiant.id = :etudiantId AND e.module.id = :moduleId")
    Optional<Evaluation> findByEtudiantAndModule(@Param("etudiantId") Long etudiantId,
                                                 @Param("moduleId") Long moduleId);

    @Query("SELECT e FROM Evaluation e WHERE e.module.id = :moduleId ORDER BY e.noteFinale DESC")
    List<Evaluation> findByModuleOrderByNoteFinaleDesc(@Param("moduleId") Long moduleId);
}