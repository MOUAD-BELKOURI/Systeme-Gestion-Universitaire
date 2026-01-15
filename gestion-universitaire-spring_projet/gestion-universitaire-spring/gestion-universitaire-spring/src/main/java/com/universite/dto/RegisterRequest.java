package com.universite.dto;

import com.universite.model.Role.ERole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    private ERole role;

    // Champs spécifiques à l'étudiant
    private Integer filiereId;
    private String niveau;
    private String groupe;
    private String competences;

    // Champs spécifiques à l'enseignant
    private String specialite;
    private String grade;
    private Integer chargeHoraire;

    // Champs spécifiques à l'administrateur
    private String departement;  // AJOUTEZ CETTE LIGNE
}