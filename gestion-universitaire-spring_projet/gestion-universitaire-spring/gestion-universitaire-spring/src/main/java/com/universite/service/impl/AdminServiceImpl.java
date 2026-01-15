package com.universite.service.impl;

import com.universite.dto.RegisterRequest;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.*;
import com.universite.repository.*;
import com.universite.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EtudiantRepository etudiantRepository;
    private final EnseignantRepository enseignantRepository;
    private final ModuleRepository moduleRepository;
    private final SalleRepository salleRepository;
    private final SeanceRepository seanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Admin createAdmin(RegisterRequest registerRequest) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("L'adresse email est déjà utilisée");
        }

        // Récupérer le rôle ADMIN
        Role role = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", "nom", "ROLE_ADMIN"));

        // Créer l'utilisateur de base
        User user = new User();
        user.setNom(registerRequest.getNom());
        user.setPrenom(registerRequest.getPrenom());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Sauvegarder l'utilisateur
        user = userRepository.save(user);

        // Créer le profil admin
        Admin admin = new Admin();
        admin.setId(user.getId());
        admin.setNom(user.getNom());
        admin.setPrenom(user.getPrenom());
        admin.setEmail(user.getEmail());
        admin.setPassword(user.getPassword());
        admin.setRole(user.getRole());
        admin.setCreatedAt(user.getCreatedAt());
        admin.setUpdatedAt(user.getUpdatedAt());
        admin.setDepartement(registerRequest.getCompetences() != null ?
                registerRequest.getCompetences() : "Administration");

        return adminRepository.save(admin);
    }

    @Override
    @Transactional
    public Admin updateAdmin(Long id, Admin adminDetails) {
        Admin admin = getAdminById(id);

        // Mettre à jour les informations de base
        admin.setNom(adminDetails.getNom());
        admin.setPrenom(adminDetails.getPrenom());
        admin.setEmail(adminDetails.getEmail());
        admin.setDepartement(adminDetails.getDepartement());
        admin.setUpdatedAt(LocalDateTime.now());

        // Mettre à jour l'utilisateur associé
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        user.setNom(adminDetails.getNom());
        user.setPrenom(adminDetails.getPrenom());
        user.setEmail(adminDetails.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return adminRepository.save(admin);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        Admin admin = getAdminById(id);

        // Ne pas permettre la suppression du dernier admin
        if (adminRepository.count() <= 1) {
            throw new IllegalStateException("Impossible de supprimer le dernier administrateur");
        }

        // Supprimer l'admin
        adminRepository.delete(admin);

        // Supprimer l'utilisateur associé
        userRepository.deleteById(id);
    }

    @Override
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur", "id", id));
    }

    @Override
    public Admin getAdminByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Administrateur", "email", email));
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public long countAdmins() {
        return adminRepository.count();
    }

    @Override
    public boolean existsByEmail(String email) {
        return adminRepository.existsByEmail(email);
    }

    @Override
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // Récupérer les statistiques
        stats.setTotalEtudiants(etudiantRepository.count());
        stats.setTotalEnseignants(enseignantRepository.count());
        stats.setTotalModules(moduleRepository.count());
        stats.setTotalSalles(salleRepository.count());
        stats.setTotalSeances(seanceRepository.count());
        stats.setTotalAdmins(adminRepository.count());

        // Modules sans enseignant
        stats.setModulesSansEnseignant(moduleRepository.findModulesSansEnseignant().size());

        // Étudiants inscrits ce mois
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        stats.setEtudiantsInscritsCeMois(
                etudiantRepository.findAll().stream()
                        .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(startOfMonth))
                        .count()
        );

        return stats;
    }

    @Override
    public List<Admin> searchByDepartement(String departement) {
        if (departement == null || departement.trim().isEmpty()) {
            return adminRepository.findAll();
        }
        return adminRepository.searchByDepartement(departement.trim());
    }

    @Override
    @Transactional
    public Admin updateProfile(Long adminId, Admin adminDetails) {
        Admin admin = getAdminById(adminId);

        // Mettre à jour les informations de profil
        if (adminDetails.getNom() != null) {
            admin.setNom(adminDetails.getNom());
        }
        if (adminDetails.getPrenom() != null) {
            admin.setPrenom(adminDetails.getPrenom());
        }
        if (adminDetails.getEmail() != null && !adminDetails.getEmail().equals(admin.getEmail())) {
            // Vérifier si le nouvel email n'est pas déjà utilisé
            if (userRepository.existsByEmail(adminDetails.getEmail())) {
                throw new IllegalArgumentException("L'adresse email est déjà utilisée");
            }
            admin.setEmail(adminDetails.getEmail());
        }
        if (adminDetails.getDepartement() != null) {
            admin.setDepartement(adminDetails.getDepartement());
        }

        admin.setUpdatedAt(LocalDateTime.now());

        // Mettre à jour l'utilisateur associé
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", adminId));

        user.setNom(admin.getNom());
        user.setPrenom(admin.getPrenom());
        user.setEmail(admin.getEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return adminRepository.save(admin);
    }

    @Override
    @Transactional
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        Admin admin = getAdminById(adminId);

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
        }

        // Mettre à jour le mot de passe
        String encodedPassword = passwordEncoder.encode(newPassword);
        admin.setPassword(encodedPassword);
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        // Mettre à jour l'utilisateur associé
        User user = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", adminId));
        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void disableAdmin(Long adminId) {
        // Dans une version plus avancée, on pourrait ajouter un champ "enabled" à l'entité User
        // Pour l'instant, on ne supprime pas mais on pourrait marquer comme inactif
        throw new UnsupportedOperationException("Fonctionnalité non implémentée dans cette version");
    }

    @Override
    public void enableAdmin(Long adminId) {
        throw new UnsupportedOperationException("Fonctionnalité non implémentée dans cette version");
    }

    @Override
    public List<Admin> getActiveAdmins() {
        // Dans une version avec gestion d'état
        return getAllAdmins();
    }

    @Override
    public List<Admin> getInactiveAdmins() {
        // Dans une version avec gestion d'état
        return List.of();
    }
}