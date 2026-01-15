package com.universite.repository;

import com.universite.model.CourseModule;
import com.universite.model.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    List<Etudiant> findByFiliereId(Integer filiereId);
    List<Etudiant> findByGroupe(String groupe);
    List<Etudiant> findByFiliereIdAndGroupe(Integer filiereId, String groupe);

    @Query("SELECT e FROM Etudiant e WHERE e.filiere.id = :filiereId AND e.niveau = :niveau")
    List<Etudiant> findByFiliereAndNiveau(@Param("filiereId") Integer filiereId, @Param("niveau") String niveau);

    // AJOUTEZ CES 2 MÉTHODES :

    @Query("SELECT DISTINCT m FROM Etudiant e JOIN e.modules m WHERE e.id = :etudiantId")
    Set<CourseModule> findModulesByEtudiantId(@Param("etudiantId") Long etudiantId);

    @Query("SELECT e FROM Etudiant e LEFT JOIN FETCH e.modules WHERE e.id = :etudiantId")
    Optional<Etudiant> findByIdWithModules(@Param("etudiantId") Long etudiantId);
}