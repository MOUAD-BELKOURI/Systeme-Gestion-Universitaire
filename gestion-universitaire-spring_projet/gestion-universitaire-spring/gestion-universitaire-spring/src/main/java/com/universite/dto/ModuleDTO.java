package com.universite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {

    private Long id;

    @NotBlank(message = "Le nom du module est obligatoire")
    private String nom;

    @NotNull(message = "La filière est obligatoire")
    private Integer filiereId;

    private String semestre;

    @NotNull(message = "Le volume horaire est obligatoire")
    private Integer volumeHoraire;

    private String competencesRequises;

    private Long enseignantId;
}