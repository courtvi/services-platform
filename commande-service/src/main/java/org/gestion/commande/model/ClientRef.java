package org.gestion.commande.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;


@Table("CLIENT_REF")
public class ClientRef implements Persistable<String> {

    @Id
    @Column("KEYCLOAK_ID")
    private String keycloakId;

    @Column("NUMERO_CLIENT")
    private String numeroClient;

    @Transient
    private boolean isNew = true;

    public ClientRef() {}

    public ClientRef(String keycloakId, String numeroClient) {
        this.keycloakId = keycloakId;
        this.numeroClient = numeroClient;
        this.isNew = true;
    }

    @Override
    public String getId() { return keycloakId; }

    @Override
    public boolean isNew() { return isNew; }

    public String getKeycloakId() { return keycloakId; }
    public String getNumeroClient() { return numeroClient; }
}
