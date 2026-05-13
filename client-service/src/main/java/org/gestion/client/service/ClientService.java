package org.gestion.client.service;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.dto.ClientResponse;
import org.gestion.client.model.Client;
import org.gestion.client.repository.ClientRepository;

import org.springframework.stereotype.Service;


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
        client.setReservation(client.getReservation());

        clientRepository.save(client);
    }


    private ClientResponse mapToClientResponse(Client client) {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setId(client.getId());
        clientResponse.setFirstName(client.getFirstName());
        clientResponse.setLastName(client.getLastName());
        clientResponse.setReservation(client.getReservation());

        return clientResponse;
    }
}