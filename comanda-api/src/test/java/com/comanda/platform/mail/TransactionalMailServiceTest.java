package com.comanda.platform.mail;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * deploy-vps 6.3: a down SMTP provider must never crash the caller (PRD 4.5, "nenhum estado
 * silencioso de falha" — the exception is caught and logged, not silently swallowed nor rethrown).
 */
class TransactionalMailServiceTest {

    @Test
    void doesNotThrowWhenSmtpIsUnreachable() throws IOException {
        // Porta reservada e fechada em seguida: nada escuta nela, então a conexão SMTP falha,
        // simulando um provedor indisponível sem depender de rede externa.
        ServerSocket socket = new ServerSocket(0);
        int unreachablePort = socket.getLocalPort();
        socket.close();

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(unreachablePort);
        TransactionalMailService service = new TransactionalMailService(sender);

        assertThatCode(() -> service.send("cliente@example.com", "Assunto", "Corpo"))
                .doesNotThrowAnyException();
    }
}
