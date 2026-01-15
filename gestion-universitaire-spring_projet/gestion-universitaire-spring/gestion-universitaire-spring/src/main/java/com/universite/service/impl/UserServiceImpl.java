package com.universite.service.impl;

import com.universite.dto.RegisterRequest;
import com.universite.exception.ResourceNotFoundException;
import com.universite.model.*;
import com.universite.repository.*;
import com.universite.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EtudiantRepository etudiantRepository;
    private final EnseignantRepository enseignantRepository;
    private final FiliereRepository filiereRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Utilisateur non trouvé avec l'email : " + email
                        )
                );
    }

    @Override
    @Transactional
    public User createUser(RegisterRequest registerRequest) {
        log.info("=== CRÉATION UTILISATEUR SIMPLIFIÉE ===");

        try {
            // 1. Vérifier l'email
            if (existsByEmail(registerRequest.getEmail())) {
                throw new IllegalArgumentException("Email déjà utilisé: " + registerRequest.getEmail());
            }

            // 2. Déterminer le rôle
            Role.ERole roleEnum = registerRequest.getRole() != null
                    ? registerRequest.getRole()
                    : Role.ERole.ROLE_ETUDIANT;

            log.info("Rôle: {}", roleEnum);

            // 3. Récupérer le rôle
            Role role = roleRepository.findByName(roleEnum)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle non trouvé: " + roleEnum));

            // 4. Créer l'entité selon le rôle
            switch (roleEnum) {
                case ROLE_ETUDIANT:
                    return createEtudiantEntity(registerRequest, role);

                case ROLE_ENSEIGNANT:
                    return createEnseignantEntity(registerRequest, role);

                case ROLE_ADMIN:
                    return createUserEntity(registerRequest, role);

                default:
                    throw new IllegalArgumentException("Rôle inconnu: " + roleEnum);
            }

        } catch (Exception e) {
            log.error("ÉCHEC création utilisateur: {}", e.getMessage(), e);
            throw e; // Relancer l'exception pour le contrôleur
        }
    }

    private User createEtudiantEntity(RegisterRequest request, Role role) {
        log.info("Création d'un étudiant...");

        // Récupérer la filière si fournie
        Filiere filiere = null;
        if (request.getFiliereId() != null) {
            filiere = filiereRepository.findById(request.getFiliereId()).orElse(null);
            log.info("Filière trouvée: {}", filiere != null ? filiere.getNom() : "non spécifiée");
        }

        // Créer directement l'étudiant (pas besoin de créer User d'abord)
        Etudiant etudiant = new Etudiant();
        etudiant.setNom(request.getNom());
        etudiant.setPrenom(request.getPrenom());
        etudiant.setEmail(request.getEmail());
        etudiant.setPassword(passwordEncoder.encode(request.getPassword()));
        etudiant.setRole(role);
        etudiant.setFiliere(filiere);
        etudiant.setNiveau(request.getNiveau());
        etudiant.setGroupe(request.getGroupe());
        etudiant.setCompetences(request.getCompetences());
        etudiant.setCreatedAt(LocalDateTime.now());
        etudiant.setUpdatedAt(LocalDateTime.now());

        log.info("Sauvegarde de l'étudiant...");
        Etudiant saved = etudiantRepository.save(etudiant);
        log.info("Étudiant créé avec ID: {}", saved.getId());

        return saved;
    }

    private User createUserEntity(RegisterRequest request, Role role) {
        // Pour les admins ou utilisateurs génériques
        User user = new User();
        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private User createEnseignantEntity(RegisterRequest request, Role role) {
        // Similaire à createEtudiantEntity mais pour Enseignant
        Enseignant enseignant = new Enseignant();
        enseignant.setNom(request.getNom());
        enseignant.setPrenom(request.getPrenom());
        enseignant.setEmail(request.getEmail());
        enseignant.setPassword(passwordEncoder.encode(request.getPassword()));
        enseignant.setRole(role);
        enseignant.setSpecialite(request.getSpecialite());
        enseignant.setGrade(request.getGrade());
        enseignant.setChargeHoraire(request.getChargeHoraire());
        enseignant.setCompetences(request.getCompetences());
        enseignant.setCreatedAt(LocalDateTime.now());
        enseignant.setUpdatedAt(LocalDateTime.now());

        return enseignantRepository.save(enseignant);
    }
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Utilisateur", "id", id)
                );
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Utilisateur", "email", email)
                );
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }
        userRepository.deleteById(id);
        log.info("Utilisateur supprimé avec ID: {}", id);
    }
}