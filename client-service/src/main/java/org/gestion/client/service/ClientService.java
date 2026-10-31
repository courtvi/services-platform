package org.gestion.client.service;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.dto.ClientResponse;
import org.gestion.client.model.Client;
import org.gestion.client.repository.ClientRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.List;


@Service
public class ClientService {

   private final ClientRepository clientRepository;


    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public void createClient(ClientRequest clientRequest) {
        Client client = new Client();
        client.setFirstName(clientRequest.getFirstName());
        client.setLastName(clientRequest.getLastName());


        clientRepository.save(client);
    }


    private ClientResponse mapToClientResponse(Client client) {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setId(client.getId());
        clientResponse.setFirstName(client.getFirstName());
        clientResponse.setLastName(client.getLastName());


        return clientResponse;
    }

    public Flux<Client> getAllClients() {
        return clientRepository.findAll();
             
    }
}