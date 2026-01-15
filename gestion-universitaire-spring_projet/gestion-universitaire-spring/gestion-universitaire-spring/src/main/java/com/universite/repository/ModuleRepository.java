package com.universite.repository;

import com.universite.model.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<CourseModule, Long> {

    // Méthodes existantes
    List<CourseModule> findByFiliereId(Integer filiereId);
    List<CourseModule> findByEnseignantId(Long enseignantId);
    List<CourseModule> findByFiliereIdAndSemestre(Integer filiereId, String semestre);

    @Query("SELECT m FROM CourseModule m WHERE m.enseignant IS NULL")
    List<CourseModule> findModulesSansEnseignant();

    @Query("SELECT DISTINCT s.module FROM Seance s WHERE s.groupe = :groupe AND s.module.filiere.id = :filiereId")
    List<CourseModule> findModulesByGroupeAndFiliere(@Param("groupe") String groupe,
                                                     @Param("filiereId") Integer filiereId);

    // NOUVELLES MÉTHODES - SIMPLIFIÉES
    @Query("SELECT m FROM CourseModule m WHERE m.filiere.id = :filiereId AND m.semestre LIKE CONCAT(:semestrePrefix, '%')")
    List<CourseModule> findByFiliereIdAndSemestreStartingWith(@Param("filiereId") Integer filiereId,
                                                              @Param("semestrePrefix") String semestrePrefix);

    // Version temporaire (sans le champ niveau)
    @Query("SELECT m FROM CourseModule m WHERE m.filiere.id = :filiereId")
    List<CourseModule> findByFiliereIdOnly(@Param("filiereId") Integer filiereId);

    // Méthode pour trouver par niveau (si le champ existe)
    default List<CourseModule> findByFiliereIdAndNiveau(Integer filiereId, String niveau) {
        // Méthode par défaut qui peut être redéfinie si le champ niveau existe
        return findByFiliereIdOnly(filiereId);
    }
}