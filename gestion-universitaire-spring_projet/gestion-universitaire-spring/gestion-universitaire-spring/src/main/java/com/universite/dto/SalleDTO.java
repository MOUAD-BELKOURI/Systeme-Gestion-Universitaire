package com.universite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalleDTO {
    private Long id;
    private String nom;
    private Integer capacite;
    private String typeSalle;
    private Boolean disponible;
    private Integer nombreSeances;

    public static SalleDTO fromEntity(com.universite.model.Salle salle, Integer nombreSeances) {
        SalleDTO dto = new SalleDTO();
        dto.setId(salle.getId());
        dto.setNom(salle.getNom());
        dto.setCapacite(salle.getCapacite());
        dto.setTypeSalle(salle.getTypeSalle());
        dto.setDisponible(salle.getDisponible());
        dto.setNombreSeances(nombreSeances);
        return dto;
    }
}