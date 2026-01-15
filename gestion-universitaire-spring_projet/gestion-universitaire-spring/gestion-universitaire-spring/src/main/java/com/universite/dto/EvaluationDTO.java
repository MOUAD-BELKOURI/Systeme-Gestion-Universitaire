package com.universite.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDTO {

    private Long id;

    @NotNull(message = "L'étudiant est obligatoire")
    private Long etudiantId;

    @NotNull(message = "Le module est obligatoire")
    private Long moduleId;

    @Min(value = 0, message = "La note TP doit être >= 0")
    @Max(value = 20, message = "La note TP doit être <= 20")
    private Double noteTP;

    @Min(value = 0, message = "La note DS doit être >= 0")
    @Max(value = 20, message = "La note DS doit être <= 20")
    private Double noteDS;

    @Min(value = 0, message = "La note projet doit être >= 0")
    @Max(value = 20, message = "La note projet doit être <= 20")
    private Double noteProjet;

    @Min(value = 0, message = "La participation doit être >= 0")
    @Max(value = 20, message = "La participation doit être <= 20")
    private Double participation;

    private String feedback;
}