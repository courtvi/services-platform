package org.gestion.client.service;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.model.Client;
import org.gestion.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    ClientRepository clientRepository;

    private ClientService clientService;
    private ClientRequest request;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository);
        request = new ClientRequest("Ben", "Weber", "+352334568",
                "ben.weber@gmail.com", LocalDate.of(1986, 10, 30), "Haller");
    }

    private static Client client(Long id, String prenom) {
        return new Client(id, prenom, "Weber", "+352334568",
                "ben.weber@gmail.com", LocalDate.of(1986, 10, 30), "Haller");
    }

    @Test
    void createClient_enregistreEtRenvoieLeClient() {
        when(clientRepository.save(any(Client.class)))
                .thenAnswer(inv -> {
                    Client c = inv.getArgument(0);
                    c.setId(1L);
                    return Mono.just(c);
                });

        StepVerifier.create(clientService.createClient(request))
                .assertNext(response -> {
                    assertEquals(1L, response.getId());
                    assertEquals("Ben", response.getFirstName());
                    assertEquals("ben.weber@gmail.com", response.getMail());
                })
                .verifyComplete();

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("Weber", captor.getValue().getLastName());
    }

    @Test
    void createClient_refuseUnNomVide() {
        request.setLastName("  ");

        StepVerifier.create(clientService.createClient(request))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(clientRepository, never()).save(any());
    }

    @Test
    void getAllClients_renvoieTousLesClients() {
        when(clientRepository.findAll()).thenReturn(Flux.just(client(1L, "Ben"), client(2L, "Anna")));

        StepVerifier.create(clientService.getAllClients())
                .assertNext(r -> assertEquals("Ben", r.getFirstName()))
                .assertNext(r -> assertEquals("Anna", r.getFirstName()))
                .verifyComplete();
    }

    @Test
    void getClientById_videSiInconnu() {
        when(clientRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(clientService.getClientById(99L)).verifyComplete();
    }

    @Test
    void updateClient_modifieLeClientExistant() {
        when(clientRepository.findById(1L)).thenReturn(Mono.just(client(1L, "Ancien")));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(clientService.updateClient(1L, request))
                .assertNext(r -> {
                    assertEquals(1L, r.getId());
                    assertEquals("Ben", r.getFirstName());
                })
                .verifyComplete();
    }

    @Test
    void updateClient_videSiInconnu() {
        when(clientRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(clientService.updateClient(99L, request)).verifyComplete();
        verify(clientRepository, never()).save(any());
    }

    @Test
    void deleteClient_trueSiSupprime() {
        Client existant = client(1L, "Ben");
        when(clientRepository.findById(1L)).thenReturn(Mono.just(existant));
        when(clientRepository.delete(existant)).thenReturn(Mono.empty());

        StepVerifier.create(clientService.deleteClient(1L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void deleteClient_falseSiInconnu() {
        when(clientRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(clientService.deleteClient(99L))
                .expectNext(false)
                .verifyComplete();
    }
}
