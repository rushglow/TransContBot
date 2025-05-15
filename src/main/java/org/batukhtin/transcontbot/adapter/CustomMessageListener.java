package org.batukhtin.transcontbot.adapter;

import jakarta.mail.*;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batukhtin.transcontbot.telegram.BotProducer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomMessageListener implements MessageCountListener {
    private final BotProducer botProducer;

    @Override
    public void messagesAdded(MessageCountEvent e) {
        for (Message message : e.getMessages()) {
            try {
                MimeMessage mimeMessage = new MimeMessage((MimeMessage) message);
                botProducer.setNotificationText(mimeMessage.getSubject(), getRequestLink(mimeMessage));
                botProducer.sendMessage();
                log.info("Новое письмо от {}: {}", mimeMessage.getFrom()[0], mimeMessage.getSubject());
            } catch (MessagingException ex) {
                log.error(ex.getMessage(), ex);
                botProducer.setNotificationText("Пришел новый запрос", "http://sd.trcont.ru");
                botProducer.sendMessage();
            } catch (Exception ex) {
                log.error("Ошибка при обработке письма", ex);
            }
        }
    }

    @Override
    public void messagesRemoved(MessageCountEvent e) {

    }

    private String getRequestLink(Message message) throws Exception {
        log.info("Тип письма: " + message.getContentType().toString());
        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                log.info("Тип вложенной части: " + part.getContentType().toString());
                if (part.isMimeType("multipart/*")) {
                    Multipart bodyMultipart = (Multipart) part.getContent();
                    for (int j = 0; j < bodyMultipart.getCount(); j++) {
                        log.info("Тип вложенной дважды части: " + part.getContentType().toString());
                        BodyPart bodyPart = bodyMultipart.getBodyPart(j);
                        if (bodyPart.isMimeType("text/html")) {
                            String html = (String) part.getContent();
                            log.info("HTML " + extractLinkFromHtml(html));
                            return extractLinkFromHtml(html);
                        }
                    }
                }

            }
        }

        return "http://sd.trcont.ru/sd/operator";
    }

    private String extractLinkFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String linkText = link.text().toLowerCase();
            String href = link.attr("href");

            if (href.startsWith("http://sd.trcont.ru/sd/operator")) {
                return href;
            }
        }

        return "http://sd.trcont.ru/sd/operator";
    }
}
