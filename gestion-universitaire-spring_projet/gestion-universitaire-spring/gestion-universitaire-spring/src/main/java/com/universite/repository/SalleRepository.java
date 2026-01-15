package com.universite.repository;

import com.universite.model.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    List<Salle> findByDisponibleTrue();
    List<Salle> findByTypeSalle(String typeSalle);

    @Query("SELECT s FROM Salle s WHERE s.id NOT IN " +
            "(SELECT se.salle.id FROM Seance se WHERE " +
            "se.dateHeure < :endTime AND se.dateHeure > :startTime)")
    List<Salle> findSallesDisponibles(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
}