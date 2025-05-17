package org.batukhtin.transcontbot.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.mail.Folder;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batukhtin.transcontbot.properties.MailProperties;
import org.batukhtin.transcontbot.adapter.CustomMessageListener;
import org.batukhtin.transcontbot.service.MailReaderService;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailReaderServiceImpl implements MailReaderService {

    private final MailProperties mailProperties;
    private final CustomMessageListener customMessageListener;

    @PostConstruct
    public void startListening() {
        new Thread(this::listenForEmails, "imap-idle-listener").start();
    }
    @Override
    public void listenForEmails() {
        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");

            Session session = Session.getInstance(props);
            Store store = session.getStore();
            store.connect(
                    mailProperties.getHost(),
                    mailProperties.getUsername(),
                    mailProperties.getPassword()
            );
            Folder folder = store.getFolder(mailProperties.getFolder());
            folder.open(Folder.READ_ONLY);

            folder.addMessageCountListener(customMessageListener);

            log.info("IMAP IDLE слушает папку: {}", folder.getFullName());

            while (true) {
                if (folder instanceof IMAPFolder imapFolder) {
                    imapFolder.idle();
                } else {
                    Thread.sleep(60000);
                }
            }
        } catch (FolderClosedException e) {
            //log.error("Перезапуск подключения");
            listenForEmails();
        } catch (MailConnectException e){
            try {
                TimeUnit.MINUTES.sleep(5);
                listenForEmails();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }catch (Exception e) {
            log.error("Ошибка при подключении к IMAP", e);
        }
    }
}
