package ru.nbrbExchange.nbrbCurrencyExchangeBot.service.imp;

import ru.nbrbExchange.nbrbCurrencyExchangeBot.entity.Currency;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.service.SelectedCurrencyService;

import java.util.HashMap;
import java.util.Map;

public class SelectedCurrencyServiceImp implements SelectedCurrencyService {
    private final Map<Long, Currency> fromCurrency = new HashMap<>();

    private final Map<Long, Currency> toCurrency = new HashMap<>();

    public SelectedCurrencyServiceImp() {
    }

    @Override
    public Currency getFromCurrency(long chatId) {
        return fromCurrency.getOrDefault(chatId, Currency.USD);
    }

    @Override
    public Currency getToCurrency(long chatId) {
        return toCurrency.getOrDefault(chatId, Currency.USD);
    }

    @Override
    public void setFromCurrency(long chatId, Currency currency) {
        fromCurrency.put(chatId, currency);
    }

    @Override
    public void setToCurrency(long chatId, Currency currency) {
        toCurrency.put(chatId, currency);
    }
}
