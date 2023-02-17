package ru.nbrbExchange.nbrbCurrencyExchangeBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.config.BotConfig;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.entity.Currency;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class TelegramBot extends TelegramLongPollingBot {


    private final SelectedCurrencyService selectedCurrencyService = SelectedCurrencyService.getInstance();

    private final CurrencyConversionService currencyConversionService = CurrencyConversionService.getInstance();


    private final String HELP_MESSAGE = ", данный бот предназначен для того, " +
            "чтобы сконверитровать из одной валюты в другую по курсу НБРБ.\n" +
            "Для получения требуемой информации, выбери из левого столбца ту валюту, " +
            "из которой хочешь конвертировать, а в правом столбце выбери ту валюту, " +
            "в которую хочешь конвертировать введенную сумму.\n\n" +
            "Сумму, которую хочешь конвертировать нужно написать в чат боту, после чего он произведет конвертацию.\n\n" +
            "Чтобы установить необходимые валюты используй комманду \n/make_exchange \n" +
            "или воспользуйся меню возле клавиатуры"; // TODO

    @Autowired
    final BotConfig botConfig;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand>  listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начинает взаимодействие с ботом"));
        listOfCommands.add(new BotCommand("/help", "данная команда вызывает подстазку по фуркционалу бота"));
        listOfCommands.add(new BotCommand("/make_exchange", "выбери валюты, чтобы узнать курс НБРБ"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if(update.hasMessage()) {
            handleMessage(update.getMessage());
        }
    }


    private void handleMessage(Message message) {
        if (message.hasText() && message.hasEntities()) {
            String messageText = message.getText();

            long chatId = message.getChatId();
            String userName = message.getChat().getFirstName();

            switch (messageText) {
                case "/start":
                    startCommandRecived(chatId, userName);
                    break;
                case "/help":
                    sendMessage(chatId, userName + HELP_MESSAGE);
                    break;
                case "/make_exchange":
                    makeExchange(chatId);
                    break;
                default:
                    sendMessage(chatId, "Sorry, unsupported command");

            }
        }

        if(message.hasText()) {
            String messageText = message.getText();
            Optional<Double> value = parseDouble(messageText);

            Currency fromCurrency = selectedCurrencyService.getFromCurrency(message.getChatId());
            Currency toCurrency = selectedCurrencyService.getToCurrency(message.getChatId());
            double ratio = currencyConversionService.getConversionRatio(fromCurrency, toCurrency);

            if(value.isPresent()){

                String messageToSend = String.format("%4.2f %s конвертируется в %4.2f %s",
                        value.get(), fromCurrency, (value.get() * ratio), toCurrency);
                sendMessage(message.getChatId(), messageToSend);
            }
        }
    }

    private Optional<Double> parseDouble(String message) {
        try{
            Optional<Double> parsedDouble = Optional.of(Double.parseDouble(message));
            return parsedDouble;
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        String[] param = callbackQuery.getData().split(":");
        String action = param[0];
        Currency newCurrency = Currency.valueOf(param[1]);

        switch (action) {
            case "FROM":
                selectedCurrencyService.setFromCurrency(message.getChatId(), newCurrency);
                break;
            case  "TO" :
                selectedCurrencyService.setToCurrency(message.getChatId(), newCurrency);
                break;
        }

        // TODO ЗАРЕФАКТОРИТЬ В ОТДЕЛЬНЫЙ МЕТОД

        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        Currency fromCurrency = selectedCurrencyService.getFromCurrency(message.getChatId());
        Currency toCurrency = selectedCurrencyService.getToCurrency(message.getChatId());

        for(Currency currency : Currency.values()) {
            buttons.add(
                    Arrays.asList(
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(fromCurrency, currency))
                                    .callbackData("FROM:" + currency)
                                    .build(),
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(toCurrency, currency))
                                    .callbackData("TO:" + currency)
                                    .build()));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);

        EditMessageReplyMarkup editedMarkup = new EditMessageReplyMarkup();
        editedMarkup.setChatId(message.getChatId());
        editedMarkup.setMessageId(message.getMessageId());
        editedMarkup.setReplyMarkup(markup);

        try {
            execute(editedMarkup);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void makeExchange(long chatId) {
        // устанавливаем подстрочные кнопки на каждую валюту
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        Currency fromCurrency = selectedCurrencyService.getFromCurrency(chatId);
        Currency toCurrency = selectedCurrencyService.getToCurrency(chatId);

        for(Currency currency : Currency.values()) {
            buttons.add(
                    Arrays.asList(
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(fromCurrency, currency))
                                    .callbackData("FROM:" + currency)
                                    .build(),
                            InlineKeyboardButton.builder()
                                    .text(getCurrencyButton(toCurrency, currency))
                                    .callbackData("TO:" + currency)
                                    .build()));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);

        // настраиваем само сообщение
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Из левого столбца выбери ту валюту, которую хочешь поменять, " +
                "а из правого - на которую хочешь поменять (по курсу НБРБ)");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }



    private void startCommandRecived(long chatId, String name) {

        String answer = "Привет " + name + " напиши команду /help для получения информации о Боте";

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }

    public String getCurrencyButton(Currency savedCurrency, Currency currentCurrency) {
        return savedCurrency == currentCurrency ? currentCurrency.name() + " \uD83D\uDC48" : currentCurrency.name();
    }


    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }
}


/////////////////////ТАК ВСТАВЛЯЮТСЯ КНОПКИ КЛАВИАТУРНЫЕ

  /*  ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

                List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("another");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("1 row2");
        row.add("2 row2");
        row.add("3 row2");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);*/




