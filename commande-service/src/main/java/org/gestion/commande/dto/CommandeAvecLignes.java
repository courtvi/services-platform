package org.gestion.commande.dto;

import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;

import java.util.List;


public class CommandeAvecLignes {

    private Commande commande;
    private List<LigneCommande> lignes;

    public CommandeAvecLignes(Commande commande, List<LigneCommande> lignes) {
        this.commande = commande;
        this.lignes = lignes;
    }
}