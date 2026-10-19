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
 * Consumidor de Notificaciones con confirmación manual.
 */
@Component
public class NotificacionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacionListener.class);

    @RabbitListener(queues = "${vidasalud.rabbitmq.queues.notificaciones.name}")
    public void procesarMensaje(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("[NotificacionListener] Mensaje recibido. Tag: {}", tag);
        try {
            // Simulación de procesamiento
            String content = new String(message.getBody());
            log.info("[NotificacionListener] Procesando: {}", content);
            
            // Éxito -> ACK
            channel.basicAck(tag, false);
            log.info("[NotificacionListener] ACK enviado para Tag: {}", tag);
        } catch (Exception e) {
            log.error("[NotificacionListener] Error al procesar mensaje. Enviando a DLQ. Tag: {}, Error: {}", tag, e.getMessage());
            // Error -> NACK (requeue=false para enviar a DLQ)
            channel.basicNack(tag, false, false);
        }
    }
}
