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

import java.util.ArrayList;
import java.util.List;

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
                Multipart multipart = (Multipart) mimeMessage.getContent();
                botProducer.setNotificationText(mimeMessage.getSubject(), getRequestLink(multipart, mimeMessage.getSubject()));
                botProducer.sendMessage();
                //log.info("Новое письмо от {}: {}", mimeMessage.getFrom()[0], mimeMessage.getSubject());
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

    private String getRequestLink(Multipart originMultipart, String subject) throws Exception {
        List<String> parentHtmlList = new ArrayList<>();

        for (int i = 0; i < originMultipart.getCount(); i++) {
            //log.info("Название мультипарта: " + originMultipart.getContentType());
            if (originMultipart.getBodyPart(i).isMimeType("multipart/*")) {
                Multipart bodyMultipart = (Multipart) originMultipart.getBodyPart(i).getContent();
                parentHtmlList.addAll(getRequestLink(bodyMultipart));
            }
            if (originMultipart.getBodyPart(i).isMimeType("text/*")) {
                String html = (String) originMultipart.getBodyPart(i).getContent();
                parentHtmlList.add(html);
            }
        }

        return extractLinkFromHtml(parentHtmlList, subject);
    }

    private List<String> getRequestLink(Multipart originMultipart) throws Exception {
        List<String> htmlList = new ArrayList<>();
        for (int i = 0; i < originMultipart.getCount(); i++) {
            if (originMultipart.getBodyPart(i).isMimeType("multipart/*")) {
                Multipart bodyMultipart = (Multipart) originMultipart.getBodyPart(i).getContent();
                htmlList.addAll(getRequestLink(bodyMultipart));
            }
            if (originMultipart.getBodyPart(i).isMimeType("text/*")) {
                String html = (String) originMultipart.getBodyPart(i).getContent();
                htmlList.add(html);
            }
        }
        return htmlList;
    }

    private String extractLinkFromHtml(List<String> html, String subject) {
        for (String htmlElement : html){
            Document doc = Jsoup.parse(htmlElement);
            Elements links = doc.select("a");

            for (Element link : links) {
                String linkText = link.text().toLowerCase();
                //log.info("Найденная ссылка: " + linkText);
                if (linkText.contains(subject.replaceAll(".*?(№\\d+).*", "$1"))) {
                    return link.attr("href");
                }
            }
        }

        //log.info("Ссылка не найдена в линках");
        return "http://sd.trcont.ru/sd/operator";
    }
}
