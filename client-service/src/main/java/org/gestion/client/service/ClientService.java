package org.gestion.client.service;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.dto.ClientResponse;
import org.gestion.client.model.Client;
import org.gestion.client.repository.ClientRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Mono<ClientResponse> createClient(ClientRequest request) {
        return valider(request)
                .map(r -> {
                    Client client = new Client();
                    copier(r, client);
                    return client;
                })
                .flatMap(clientRepository::save)
                .map(this::mapToClientResponse);
    }

    public Flux<ClientResponse> getAllClients() {
        return clientRepository.findAll().map(this::mapToClientResponse);
    }

    /** Mono vide si le client n'existe pas (le handler renvoie alors 404). */
    public Mono<ClientResponse> getClientById(Long id) {
        return clientRepository.findById(id).map(this::mapToClientResponse);
    }

    /** Mono vide si le client n'existe pas. */
    public Mono<ClientResponse> updateClient(Long id, ClientRequest request) {
        return valider(request)
                .flatMap(r -> clientRepository.findById(id)
                        .map(client -> {
                            copier(r, client);
                            return client;
                        }))
                .flatMap(clientRepository::save)
                .map(this::mapToClientResponse);
    }

    /** Renvoie true si le client existait et a été supprimé, false sinon. */
    public Mono<Boolean> deleteClient(Long id) {
        return clientRepository.findById(id)
                .flatMap(client -> clientRepository.delete(client).thenReturn(true))
                .defaultIfEmpty(false);
    }

    private Mono<ClientRequest> valider(ClientRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Corps de requête manquant"));
        }
        if (estVide(request.getFirstName()) || estVide(request.getLastName())) {
            return Mono.error(new IllegalArgumentException("Prénom et nom obligatoires"));
        }
        return Mono.just(request);
    }

    private static boolean estVide(String s) {
        return s == null || s.isBlank();
    }

    private static void copier(ClientRequest source, Client cible) {
        cible.setFirstName(source.getFirstName());
        cible.setLastName(source.getLastName());
        cible.setTelephone(source.getTelephone());
        cible.setMail(source.getMail());
        cible.setDateNaissance(source.getDateNaissance());
        cible.setAdresse(source.getAdresse());
    }

    private ClientResponse mapToClientResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getTelephone(),
                client.getMail(),
                client.getDateNaissance(),
                client.getAdresse());
    }
}
