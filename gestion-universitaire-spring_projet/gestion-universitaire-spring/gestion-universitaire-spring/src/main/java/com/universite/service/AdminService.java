package com.universite.service;

import com.universite.dto.RegisterRequest;
import com.universite.model.Admin;
import com.universite.model.Etudiant;
import com.universite.model.User;

import java.util.List;

public interface AdminService {

    /**
     * Créer un nouvel administrateur
     */
    Admin createAdmin(RegisterRequest registerRequest);

    /**
     * Mettre à jour un administrateur existant
     */
    Admin updateAdmin(Long id, Admin admin);

    /**
     * Supprimer un administrateur
     */
    void deleteAdmin(Long id);

    /**
     * Récupérer un administrateur par son ID
     */
    Admin getAdminById(Long id);

    /**
     * Récupérer un administrateur par son email
     */
    Admin getAdminByEmail(String email);

    /**
     * Récupérer tous les administrateurs
     */
    List<Admin> getAllAdmins();

    /**
     * Compter le nombre total d'administrateurs
     */
    long countAdmins();

    /**
     * Vérifier si un administrateur existe par email
     */
    boolean existsByEmail(String email);

    /**
     * Récupérer les statistiques pour le tableau de bord admin
     */
    DashboardStats getDashboardStats();

    /**
     * Rechercher des administrateurs par département
     */
    List<Admin> searchByDepartement(String departement);

    /**
     * Mettre à jour les informations de profil d'un admin
     */
    Admin updateProfile(Long adminId, Admin adminDetails);

    /**
     * Changer le mot de passe d'un administrateur
     */
    void changePassword(Long adminId, String oldPassword, String newPassword);

    /**
     * Désactiver un compte administrateur
     */
    void disableAdmin(Long adminId);

    /**
     * Activer un compte administrateur
     */
    void enableAdmin(Long adminId);

    /**
     * Récupérer les administrateurs actifs
     */
    List<Admin> getActiveAdmins();

    /**
     * Récupérer les administrateurs inactifs
     */
    List<Admin> getInactiveAdmins();

    /**
     * Classe pour les statistiques du tableau de bord
     */
    class DashboardStats {
        private long totalEtudiants;
        private long totalEnseignants;
        private long totalModules;
        private long totalSalles;
        private long totalSeances;
        private long totalAdmins;
        private long etudiantsInscritsCeMois;
        private long modulesSansEnseignant;
        private long conflitsEmploiTemps;

        // Getters et Setters
        public long getTotalEtudiants() {
            return totalEtudiants;
        }

        public void setTotalEtudiants(long totalEtudiants) {
            this.totalEtudiants = totalEtudiants;
        }

        public long getTotalEnseignants() {
            return totalEnseignants;
        }

        public void setTotalEnseignants(long totalEnseignants) {
            this.totalEnseignants = totalEnseignants;
        }

        public long getTotalModules() {
            return totalModules;
        }

        public void setTotalModules(long totalModules) {
            this.totalModules = totalModules;
        }

        public long getTotalSalles() {
            return totalSalles;
        }

        public void setTotalSalles(long totalSalles) {
            this.totalSalles = totalSalles;
        }

        public long getTotalSeances() {
            return totalSeances;
        }

        public void setTotalSeances(long totalSeances) {
            this.totalSeances = totalSeances;
        }

        public long getTotalAdmins() {
            return totalAdmins;
        }

        public void setTotalAdmins(long totalAdmins) {
            this.totalAdmins = totalAdmins;
        }

        public long getEtudiantsInscritsCeMois() {
            return etudiantsInscritsCeMois;
        }

        public void setEtudiantsInscritsCeMois(long etudiantsInscritsCeMois) {
            this.etudiantsInscritsCeMois = etudiantsInscritsCeMois;
        }

        public long getModulesSansEnseignant() {
            return modulesSansEnseignant;
        }

        public void setModulesSansEnseignant(long modulesSansEnseignant) {
            this.modulesSansEnseignant = modulesSansEnseignant;
        }

        public long getConflitsEmploiTemps() {
            return conflitsEmploiTemps;
        }

        public void setConflitsEmploiTemps(long conflitsEmploiTemps) {
            this.conflitsEmploiTemps = conflitsEmploiTemps;
        }
    }
}