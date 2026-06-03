package org.gestion.commande.dto;

import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;

import java.util.List;

public record CommandeAvecLignes(CommandeResponse commande, List<LigneCommande> lignes) {}