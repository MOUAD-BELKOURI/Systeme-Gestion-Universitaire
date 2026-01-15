package com.universite.repository;

import com.universite.model.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    List<Enseignant> findBySpecialiteContaining(String specialite);

    @Query("SELECT e FROM Enseignant e WHERE LOWER(e.competences) LIKE LOWER(CONCAT('%', :competence, '%'))")
    List<Enseignant> findByCompetence(@Param("competence") String competence);
}