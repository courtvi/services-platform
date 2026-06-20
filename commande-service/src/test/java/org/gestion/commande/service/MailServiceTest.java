package org.gestion.commande.service;

import jakarta.mail.internet.MimeMessage;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.model.LigneCommande;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "test@camping-haller.fr");
    }

    private static final LocalDateTime DATE_COMMANDE  = LocalDateTime.of(2026, 1, 15, 10, 0, 0);
    private static final LocalDateTime DATE_LIVRAISON = LocalDateTime.of(2026, 1, 16, 10, 0, 0);

    @Test
    void sendConfirmationCommande_shouldSendEmail() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        CommandeResponse commande = new CommandeResponse(
                1L,
                "user-alice",
                "CLI-001",
                "CMD-ALICE-001",
                "CREEE",
                DATE_COMMANDE,   // ← date fixe
                DATE_LIVRAISON,  // ← date fixe
                6.70
        );

        LigneCommande ligne1 = new LigneCommande();
        ligne1.setArticle("Baguette");
        ligne1.setQuantite(2);
        ligne1.setPrixUnitaire(1.10);
        ligne1.setTotal(2.20);

        LigneCommande ligne2 = new LigneCommande();
        ligne2.setArticle("Croissant");
        ligne2.setQuantite(3);
        ligne2.setPrixUnitaire(1.50);
        ligne2.setTotal(4.50);

        // Act
        mailService.sendConfirmationCommande(
                "alice@test.com",
                commande,
                List.of(ligne1, ligne2)
        ).block();

        // Assert
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendConfirmationCommande_withEmptyLignes_shouldStillSendEmail() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        CommandeResponse commande = new CommandeResponse(
                2L,
                "user-bob",
                "CLI-002",
                "CMD-BOB-001",
                "CREEE",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                0.0
        );

        // Act
        mailService.sendConfirmationCommande(
                "bob@test.com",
                commande,
                List.of()
        ).block();

        // Assert
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendConfirmationCommande_withNullDates_shouldNotThrow() throws Exception {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        CommandeResponse commande = new CommandeResponse(
                3L,
                "user-alice",
                "CLI-001",
                "CMD-ALICE-003",
                "CREEE",
                null,   // ← dateCommande null
                null,   // ← dateLivraison null
                3.50
        );

        // Act & Assert — ne doit pas lever d'exception
        mailService.sendConfirmationCommande(
                "alice@test.com",
                commande,
                null
        ).block();

        verify(mailSender, times(1)).send(mimeMessage);
    }
}