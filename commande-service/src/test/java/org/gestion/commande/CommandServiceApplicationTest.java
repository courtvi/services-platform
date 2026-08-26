package org.gestion.commande;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "MAIL_USERNAME=test@test.com",
        "MAIL_PASSWORD=test-password",
        "MAIL_HOST=sandbox.smtp.mailtrap.io",
        "MAIL_PORT=587",
        "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:30090/realms/lorrconnect",
        "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://localhost:30090/realms/lorrconnect/protocol/openid-connect/certs"
})
public class CommandServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
