package org.gestion.devis.service;

import org.gestion.devis.dto.DevisRequest;
import org.gestion.devis.dto.LigneDevisRequest;
import org.gestion.devis.dto.ProduitInfo;
import org.gestion.devis.model.LigneDevis;
import org.gestion.devis.model.StatutVersion;
import org.gestion.devis.model.VersionDevis;
import org.gestion.devis.repository.DevisRepository;
import org.gestion.devis.repository.LigneDevisRepository;
import org.gestion.devis.repository.VersionDevisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevisServiceTest {

    private static final Long DEVIS_ID = 5L;

    @Mock DevisRepository devisRepository;
    @Mock VersionDevisRepository versionRepository;
    @Mock LigneDevisRepository ligneRepository;
    @Mock ProduitClient produitClient;

    private DevisService devisService;

    private final ProduitInfo miel =
            new ProduitInfo(3L, "Miel", "pot", new BigDecimal("8.50"), new BigDecimal("5.50"));

    @BeforeEach
    void setUp() {
        devisService = new DevisService(devisRepository, versionRepository, ligneRepository, produitClient);
    }

    private static VersionDevis version(Long id, StatutVersion statut) {
        VersionDevis v = VersionDevis.premiere(DEVIS_ID);
        v.setId(id);
        v.setStatut(statut);
        return v;
    }

    @Test
    void creer_refuseSansClient() {
        StepVerifier.create(devisService.creer(new DevisRequest(null)))
                .expectError(IllegalArgumentException.class)
                .verify();
        verify(devisRepository, never()).save(any());
    }

    @Test
    void ajouterLigne_copieLePrixDuProduitDansLaVersionBrouillon() {
        when(versionRepository.findFirstByDevisIdOrderByNumeroVersionDesc(DEVIS_ID))
                .thenReturn(Mono.just(version(1L, StatutVersion.BROUILLON)));
        when(ligneRepository.findByVersionId(1L)).thenReturn(Flux.empty());
        when(produitClient.getProduit(3L)).thenReturn(Mono.just(miel));
        when(ligneRepository.save(any(LigneDevis.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(devisRepository.findById(DEVIS_ID)).thenReturn(Mono.empty());

        StepVerifier.create(devisService.ajouterLigne(DEVIS_ID, new LigneDevisRequest(3L, 2)))
                .verifyComplete();

        ArgumentCaptor<LigneDevis> captor = ArgumentCaptor.forClass(LigneDevis.class);
        verify(ligneRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getVersionId());
        assertEquals(new BigDecimal("8.50"), captor.getValue().getPrixUnitaireHT());
        assertEquals(2, captor.getValue().getQuantite());
    }

    @Test
    void ajouterLigne_refuseSurUneVersionEnvoyee() {
        when(versionRepository.findFirstByDevisIdOrderByNumeroVersionDesc(DEVIS_ID))
                .thenReturn(Mono.just(version(1L, StatutVersion.ENVOYEE)));
        when(ligneRepository.findByVersionId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(devisService.ajouterLigne(DEVIS_ID, new LigneDevisRequest(3L, 2)))
                .expectError(IllegalStateException.class)
                .verify();
        verify(produitClient, never()).getProduit(any());
    }

    @Test
    void ajouterLigne_videSiDevisInconnu() {
        when(versionRepository.findFirstByDevisIdOrderByNumeroVersionDesc(99L)).thenReturn(Mono.empty());

        StepVerifier.create(devisService.ajouterLigne(99L, new LigneDevisRequest(3L, 2)))
                .verifyComplete();
    }

    @Test
    void nouvelleVersion_remplaceLaPrecedenteEtCopieSesLignes() {
        VersionDevis v1 = version(1L, StatutVersion.ENVOYEE);
        LigneDevis ligne = new LigneDevis(miel, 2);
        ligne.setId(10L);
        ligne.setVersionId(1L);

        when(versionRepository.findFirstByDevisIdOrderByNumeroVersionDesc(DEVIS_ID)).thenReturn(Mono.just(v1));
        when(ligneRepository.findByVersionId(1L)).thenReturn(Flux.just(ligne));
        when(versionRepository.save(any(VersionDevis.class))).thenAnswer(inv -> {
            VersionDevis v = inv.getArgument(0);
            if (v.getId() == null) v.setId(2L);
            return Mono.just(v);
        });
        when(ligneRepository.save(any(LigneDevis.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(devisRepository.findById(DEVIS_ID)).thenReturn(Mono.empty());

        StepVerifier.create(devisService.nouvelleVersion(DEVIS_ID)).verifyComplete();

        assertEquals(StatutVersion.REMPLACEE, v1.getStatut());
        ArgumentCaptor<LigneDevis> captor = ArgumentCaptor.forClass(LigneDevis.class);
        verify(ligneRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getVersionId(), "la ligne copiée appartient à la v2");
        assertEquals(3L, captor.getValue().getProduitId());
    }
}
