package org.gestion.devis.service;

import org.gestion.devis.dto.DevisRequest;
import org.gestion.devis.dto.DevisResponse;
import org.gestion.devis.model.Devis;
import org.gestion.devis.repository.DevisRepository;

import org.springframework.stereotype.Service;


import java.util.List;


@Service
public class DevisService {

   private final DevisRepository devisRepository;

    public DevisService(DevisRepository devisRepository) {
        this.devisRepository = devisRepository;
    }

    public void createDevis(DevisRequest devisRequest) {
        Devis devis = new Devis();
        devis.setReference(devisRequest.getReference());
        devis.setNom(devisRequest.getNom());
        devis.setDate(devisRequest.getDate());
        devis.setAdresse(devisRequest.getAdresse());
        devis.setRaisonSociale(devisRequest.getRaisonSociale());

        devisRepository.save(devis);
    }


    private DevisResponse mapToDevisResponse(Devis devis) {
        DevisResponse devisResponse = new DevisResponse();
        devisResponse.setId(devis.getId());
        devisResponse.setReference(devis.getReference());
        devisResponse.setNom(devis.getNom());
        devisResponse.setAdresse(devis.getAdresse());
        devisResponse.setRaisonSociale(devis.getRaisonSociale());

        return devisResponse;
    }
}