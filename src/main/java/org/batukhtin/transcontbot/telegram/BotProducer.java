package org.batukhtin.transcontbot.telegram;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.batukhtin.transcontbot.config.BotConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Setter
@Getter
public class BotProducer extends TelegramLongPollingBot {
    private final BotConfig botConfig;

    private String currentWorker;
    private String notificationText;
    private boolean userHaveSeenMessage;

    Deque<Integer> messageIds = new ArrayDeque<>();

    private static final Map<String, String> BUTTONS = new HashMap<>();

    static {

        BUTTONS.put("/worker", "Выйти на смену");
        BUTTONS.put("/notification", "Прочитано");
        BUTTONS.put("/after_notification", "Прочитано ✅");
        BUTTONS.put("/leave", "Уйти со смены");

    }

    public void setNotificationText(String notificationText, String link) {

        this.notificationText = "<a href=\"" + link + "\">"+ notificationText + "</a>";
        this.userHaveSeenMessage = false;

    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            switch (data) {
                case "/worker" -> {

                    currentWorker = update.getCallbackQuery().getFrom().getUserName();

                    List<InlineKeyboardButton> buttons = new ArrayList<>();
                    buttons.add(createButton(BUTTONS.get("/leave"), "/leave"));

                    sendMessage("На смену вышел @" + currentWorker, buttons);

                }
                case "/notification" -> {
                    if (currentWorker != null && currentWorker.equals(update.getCallbackQuery().getFrom().getUserName())) {

                        userHaveSeenMessage = true;

                        List<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(createButton(BUTTONS.get("/after_notification"), "/after_notification"));

                        editMarkup(buttons);
                    }
                }
                case "/leave" -> {
                    if (currentWorker != null && currentWorker.equals(update.getCallbackQuery().getFrom().getUserName())) {

                        List<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(createButton(BUTTONS.get("/worker"), "/worker"));

                        deleteMessages();

                        sendMessage("@" + currentWorker + " вышел со смены", buttons);
                        currentWorker = null;
                    }
                }
            }
        }

        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getChatId().equals(botConfig.getChatId())) {
            String messageText = update.getMessage().getText();

            switch (messageText) {
                case "/start" -> {

                        List<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(createButton(BUTTONS.get("/worker"), "/worker"));
                        buttons.add(createButton(BUTTONS.get("/leave"), "/leave"));

                        sendMessage("Привет, я буду помогать тебе следить за сообщениями 1 линии поддержки", buttons);
                        deleteMessage(update.getMessage().getMessageId());

                }
                default -> {

                        List<InlineKeyboardButton> buttons = new ArrayList<>();
                        buttons.add(createButton(BUTTONS.get("/worker"), "/worker"));
                        buttons.add(createButton(BUTTONS.get("/leave"), "/leave"));

                        sendMessage("Список команд", buttons);
                        deleteMessage(update.getMessage().getMessageId());

                }
            }
        }
    }

    public void sendMessage(String textToSend, List<InlineKeyboardButton> keyboardButtons){

        List<List<InlineKeyboardButton>> keyboard = List.of(keyboardButtons);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(botConfig.getChatId());
        message.setText(textToSend);
        message.setParseMode("HTML");
        message.setReplyMarkup(markup);

        try {
            addMessageId(execute(message).getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void deleteMessage(Integer messageId) {
        try {
            if (messageId != null) {

                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(botConfig.getChatId());
                deleteMessage.setMessageId(messageId);
                execute(deleteMessage);

            }
        } catch (Exception e) {

            e.printStackTrace();

        }
    }

    public void deleteMessages() {
        DeleteMessages deleteMessages = new DeleteMessages();
        deleteMessages.setChatId(botConfig.getChatId());
        deleteMessages.setMessageIds(messageIds.stream().toList());
        messageIds.clear();
        try {
            execute(deleteMessages);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMessageId(int id) {
        if (messageIds.size() >= 15) {
            deleteMessage(messageIds.removeFirst());
        }
        messageIds.addLast(id);
    }

    private InlineKeyboardButton createButton(String text, String callBack) {

        var button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callBack);
        return button;

    }

    @Scheduled(fixedRate = 30000)
    public void sendMessage(){
        if (!userHaveSeenMessage && currentWorker != null && notificationText != null) {

            List<InlineKeyboardButton> buttons = new ArrayList<>();
            buttons.add(createButton(BUTTONS.get("/notification"), "/notification"));

            sendMessage("@"+ currentWorker + "\n\n" + notificationText, buttons);

        }
    }

    public void editMarkup(List<InlineKeyboardButton> keyboardButtons){
        List<List<InlineKeyboardButton>> keyboard = List.of(keyboardButtons);


        InlineKeyboardMarkup updatedMarkup = new InlineKeyboardMarkup();
        updatedMarkup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setReplyMarkup(updatedMarkup);
        editMarkup.setChatId(botConfig.getChatId());
        editMarkup.setMessageId(messageIds.getLast());

        try {
            execute(editMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
