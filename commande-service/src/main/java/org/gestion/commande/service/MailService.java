package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.gestion.commande.model.LigneCommande;

import java.time.format.DateTimeFormatter;
import java.util.List;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
    // Ajoute cette constante en haut de la classe (après les champs existants)
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd à HH:mm");

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public Mono<Void> sendConfirmationCommande(String toEmail, CommandeResponse commande, List<LigneCommande> lignes) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("✅ Confirmation de commande - " + commande.reference());
                helper.setText(buildEmailBody(commande, lignes), true);
                mailSender.send(message);
                System.out.println("✅ Email envoyé à " + toEmail);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Erreur envoi email: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String buildEmailBody(CommandeResponse commande, List<LigneCommande> lignes) {

        String statusBadge = "<span style=\"background: #EAF3DE; color: #27500A; font-size: 12px; " +
                "padding: 2px 8px; border-radius: 4px; font-weight: 500;\">" +
                commande.statut() + "</span>";



// Remplace la ligne 53
        String dateCommande = commande.dateCommande() != null
                ? commande.dateCommande().format(DATE_FORMATTER)
                : "-";

        String dateLivraison = commande.dateLivraison() != null
                ? commande.dateLivraison().toLocalDate().toString()
                : "-";

        double totalHT = commande.total() / 1.03;
        double totalTVA = commande.total() - totalHT;

        StringBuilder lignesHtml = new StringBuilder();
        if (lignes != null && !lignes.isEmpty()) {
            lignesHtml.append(
                    "<p style=\"font-size: 12px; color: #888; margin: 0 0 0.75rem; font-weight: 500; text-transform: uppercase;\">Articles commandés</p>" +
                            "<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 1.5rem; border: 1px solid #e0e0e0;\">" +
                            "<thead><tr style=\"background: #f9f9f9;\">" +
                            "<th style=\"padding: 10px 12px; text-align: left; font-size: 12px; color: #888;\">Article</th>" +
                            "<th style=\"padding: 10px 12px; text-align: center; font-size: 12px; color: #888;\">Qté</th>" +
                            "<th style=\"padding: 10px 12px; text-align: right; font-size: 12px; color: #888;\">Prix unit.</th>" +
                            "<th style=\"padding: 10px 12px; text-align: right; font-size: 12px; color: #888;\">Total</th>" +
                            "</tr></thead><tbody>"
            );
            for (LigneCommande ligne : lignes) {
                lignesHtml.append(
                        "<tr style=\"border-top: 1px solid #e0e0e0;\">" +
                                "<td style=\"padding: 10px 12px; font-size: 13px;\">" + ligne.getArticle() + "</td>" +
                                "<td style=\"padding: 10px 12px; text-align: center; font-size: 13px;\">" + ligne.getQuantite() + "</td>" +
                                "<td style=\"padding: 10px 12px; text-align: right; font-size: 13px;\">" + String.format("%.2f €", ligne.getPrixUnitaire()) + "</td>" +
                                "<td style=\"padding: 10px 12px; text-align: right; font-size: 13px;\">" + String.format("%.2f €", ligne.getTotal()) + "</td>" +
                                "</tr>"
                );
            }
            lignesHtml.append("</tbody></table>");
        }
        return "<html><body style=\"font-family: Arial, sans-serif; background: #f5f5f5; padding: 2rem; margin: 0;\">" +

                "<div style=\"max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; border: 1px solid #e0e0e0;\">" +

                // Header
                "<div style=\"background: #1a1a1a; padding: 2rem; text-align: center;\">" +
                "<p style=\"font-size: 22px; font-weight: 500; color: white; margin: 0 0 4px;\">🏕 Camping Haller</p>" +
                "<p style=\"color: #999; font-size: 13px; margin: 0;\">Service boulangerie</p>" +
                "</div>" +

                // Success banner
                "<div style=\"background: #EAF3DE; padding: 1.5rem; text-align: center; border-bottom: 1px solid #e0e0e0;\">" +
                "<p style=\"font-size: 32px; margin: 0;\">✅</p>" +
                "<h2 style=\"margin: 0.5rem 0 0.25rem; font-size: 18px; font-weight: 500; color: #27500A;\">Commande confirmée</h2>" +
                "<p style=\"margin: 0; font-size: 14px; color: #3B6D11;\">Votre commande a bien été enregistrée</p>" +
                "</div>" +

                // Body
                "<div style=\"padding: 1.5rem;\">" +

                // Order details
                "<div style=\"background: #f9f9f9; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem;\">" +
                "<p style=\"font-size: 12px; color: #888; margin: 0 0 0.75rem; font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em;\">Détails de la commande</p>" +
                "<table style=\"width: 100%; border-collapse: collapse;\">" +
                "<tr>" +
                "<td style=\"padding: 4px 0; font-size: 12px; color: #888; width: 50%;\">Référence</td>" +
                "<td style=\"padding: 4px 0; font-size: 12px; color: #888;\">Statut</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding: 0 0 12px; font-size: 14px; font-weight: 500; font-family: monospace;\">" + commande.reference() + "</td>" +
                "<td style=\"padding: 0 0 12px;\">" + statusBadge + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding: 4px 0; font-size: 12px; color: #888;\">Date de commande</td>" +
                "<td style=\"padding: 4px 0; font-size: 12px; color: #888;\">Date de livraison</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"font-size: 14px;\">" + dateCommande + "</td>" +
                "<td style=\"font-size: 14px; font-weight: 500; color: #185FA5;\">" + dateLivraison + "</td>" +
                "</tr>" +
                "</table>" +
                "</div>" +
                lignesHtml +
                // Info box
                "<div style=\"background: #E6F1FB; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem;\">" +
                "<p style=\"font-size: 13px; color: #0C447C; margin: 0; line-height: 1.6;\">" +
                "ℹ️ Votre commande sera prête pour le <strong>" + dateLivraison + "</strong>. " +
                "Vous pouvez la récupérer à la boulangerie dès l'ouverture à 7h00." +
                "</p>" +
                "</div>" +

                // Total
                "<div style=\"background: #f9f9f9; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem;\">" +
                "<table style=\"width: 100%; border-collapse: collapse;\">" +
                "<tr>" +
                "<td style=\"padding: 6px 0; font-size: 13px; color: #888;\">Total HT</td>" +
                "<td style=\"padding: 6px 0; font-size: 13px; text-align: right; color: #333;\">" + String.format("%.2f €", totalHT).replace(".", ",") + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding: 6px 0; font-size: 13px; color: #888;\">TVA (3%)</td>" +
                "<td style=\"padding: 6px 0; font-size: 13px; text-align: right; color: #333;\">" + String.format("%.2f €", totalTVA).replace(".", ",") + "</td>" +
                "</tr>" +
                "<tr style=\"border-top: 1px solid #e0e0e0;\">" +
                "<td style=\"padding: 8px 0 0; font-size: 14px; font-weight: 500;\">Total TTC</td>" +
                "<td style=\"padding: 8px 0 0; font-size: 18px; font-weight: 500; text-align: right;\">" + String.format("%.2f €", commande.total()).replace(".", ",") + "</td>" +
                "</tr>" +
                "</table>" +
                "</div>" +

                // Footer
                "<div style=\"border-top: 1px solid #e0e0e0; padding: 1.25rem 1.5rem; text-align: center;\">" +
                "<p style=\"font-size: 12px; color: #888; margin: 0 0 4px;\">Camping Haller — Boulangerie artisanale</p>" +
                "<p style=\"font-size: 12px; color: #aaa; margin: 0;\">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
                "</div>" +

                "</div>" +
                "</body></html>";
    }
}