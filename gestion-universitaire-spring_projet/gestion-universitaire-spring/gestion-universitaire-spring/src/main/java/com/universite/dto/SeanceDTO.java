package com.universite.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeanceDTO {

    private Long id;

    @NotNull(message = "Le module est obligatoire")
    private Long moduleId;

    @NotNull(message = "L'enseignant est obligatoire")
    private Long enseignantId;

    @NotBlank(message = "Le groupe est obligatoire")
    private String groupe;

    @NotNull(message = "La salle est obligatoire")
    private Long salleId;

    @NotNull(message = "La date et heure sont obligatoires")
    @FutureOrPresent(message = "La date doit être aujourd'hui ou dans le futur")
    private LocalDateTime dateHeure;

    @NotNull(message = "La durée est obligatoire")
    private Integer duree; // en minutes

    private String typeSeance; // CM, TD, TP
}