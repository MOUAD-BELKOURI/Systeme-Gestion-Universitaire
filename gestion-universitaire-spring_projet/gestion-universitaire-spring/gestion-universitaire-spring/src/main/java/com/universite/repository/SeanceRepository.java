package com.universite.repository;

import com.universite.model.Seance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, Long> {

    List<Seance> findByEnseignantId(Long enseignantId);
    List<Seance> findByGroupe(String groupe);
    List<Seance> findBySalleId(Long salleId);
    List<Seance> findByModuleId(Long moduleId);

    // 🔹 Conflit enseignant
    @Query("""
        SELECT s FROM Seance s
        WHERE s.enseignant.id = :enseignantId
        AND s.dateHeure < :endTime
        AND s.dateFin > :startTime
    """)
    List<Seance> findConflitsEnseignant(
            @Param("enseignantId") Long enseignantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 🔹 Conflit groupe
    @Query("""
        SELECT s FROM Seance s
        WHERE s.groupe = :groupe
        AND s.dateHeure < :endTime
        AND s.dateFin > :startTime
    """)
    List<Seance> findConflitsGroupe(
            @Param("groupe") String groupe,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 🔹 Conflit salle
    @Query("""
        SELECT s FROM Seance s
        WHERE s.salle.id = :salleId
        AND s.dateHeure < :endTime
        AND s.dateFin > :startTime
    """)
    List<Seance> findConflitsSalle(
            @Param("salleId") Long salleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 🔹 Récupérer les séances par groupe et filière avec jointures
    @Query("""
        SELECT DISTINCT s FROM Seance s
        JOIN FETCH s.module m
        JOIN FETCH m.filiere f
        JOIN FETCH s.enseignant e
        JOIN FETCH s.salle sa
        WHERE s.groupe = :groupe
        AND m.filiere.id = :filiereId
        ORDER BY s.dateHeure
    """)
    List<Seance> findByGroupeAndFiliere(@Param("groupe") String groupe,
                                        @Param("filiereId") Integer filiereId);

    // 🔹 Récupérer toutes les séances avec jointures
    @Query("""
        SELECT DISTINCT s FROM Seance s
        LEFT JOIN FETCH s.module m
        LEFT JOIN FETCH m.filiere
        LEFT JOIN FETCH s.enseignant
        LEFT JOIN FETCH s.salle
        ORDER BY s.dateHeure
    """)
    List<Seance> findAllWithDetails();

    // 🔹 Récupérer les séances par étudiant - CORRIGÉ (Version simplifiée)
    @Query("""
        SELECT DISTINCT s FROM Seance s
        JOIN FETCH s.module m
        LEFT JOIN FETCH m.filiere f
        LEFT JOIN FETCH s.enseignant e
        LEFT JOIN FETCH s.salle sa
        WHERE s.groupe IN (SELECT etu.groupe FROM Etudiant etu WHERE etu.id = :etudiantId)
        ORDER BY s.dateHeure
    """)
    List<Seance> findSeancesByEtudiantId(@Param("etudiantId") Long etudiantId);

    // 🔹 Récupérer les séances par groupe avec tous les détails
    @Query("""
        SELECT DISTINCT s FROM Seance s
        LEFT JOIN FETCH s.module m
        LEFT JOIN FETCH m.filiere f
        LEFT JOIN FETCH s.enseignant e
        LEFT JOIN FETCH s.salle sa
        WHERE s.groupe = :groupe
        ORDER BY s.dateHeure
    """)
    List<Seance> findByGroupeWithDetails(@Param("groupe") String groupe);

    // 🔹 Récupérer les séances par date
    @Query("""
        SELECT s FROM Seance s
        JOIN FETCH s.module m
        JOIN FETCH m.filiere f
        JOIN FETCH s.enseignant e
        JOIN FETCH s.salle sa
        WHERE s.groupe = :groupe
        AND m.filiere.id = :filiereId
        AND DATE(s.dateHeure) = DATE(:date)
        ORDER BY s.dateHeure
    """)
    List<Seance> findByGroupeFiliereAndDate(
            @Param("groupe") String groupe,
            @Param("filiereId") Integer filiereId,
            @Param("date") LocalDateTime date);
}