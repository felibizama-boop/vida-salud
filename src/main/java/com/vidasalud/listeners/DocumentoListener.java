package com.vidasalud.listeners;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor de Documentos con confirmación manual.
 */
@Component
public class DocumentoListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentoListener.class);

    @RabbitListener(queues = "${vidasalud.rabbitmq.queues.documentos.name}")
    public void procesarMensaje(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("[DocumentoListener] Mensaje recibido. Tag: {}", tag);
        try {
            String content = new String(message.getBody());
            log.info("[DocumentoListener] Procesando Documento: {}", content);
            
            channel.basicAck(tag, false);
            log.info("[DocumentoListener] ACK enviado para Tag: {}", tag);
        } catch (Exception e) {
            log.error("[DocumentoListener] Error en Documento. A la DLQ. Tag: {}, Error: {}", tag, e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
