package com.universite.repository;

import com.universite.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Trouver un admin par son email
     */
    Optional<Admin> findByEmail(String email);

    /**
     * Vérifier si un admin existe par email
     */
    boolean existsByEmail(String email);

    /**
     * Trouver des admins par département
     */
    List<Admin> findByDepartement(String departement);

    /**
     * Rechercher par département contenant une chaîne
     */
    @Query("SELECT a FROM Admin a WHERE LOWER(a.departement) LIKE LOWER(CONCAT('%', :departement, '%'))")
    List<Admin> searchByDepartement(@Param("departement") String departement);
}