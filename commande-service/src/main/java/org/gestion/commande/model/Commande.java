package org.gestion.commande.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table(name = "COMMANDES")
public class Commande {

    @Id

    private Long id;

    @Column("USER_ID")
    private String userId;

    @Column("REFERENCE")
    private String reference;

    @Column("STATUT")
    private String statut;

    @Column("DATE_COMMANDE")
    private LocalDateTime dateCommande;




    public Commande(Long id, String userId, String reference,
                    String statut, LocalDateTime dateCommande) {
        this.id = id;
        this.userId = userId;
        this.reference = reference;
        this.statut = statut;
        this.dateCommande = dateCommande;

    }
    public Commande() {

    }
    // ===== GETTERS / SETTERS =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) {
        this.dateCommande = dateCommande;    }

}