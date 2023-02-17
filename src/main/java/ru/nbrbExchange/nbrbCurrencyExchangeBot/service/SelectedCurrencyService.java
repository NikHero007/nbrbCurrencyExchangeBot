package ru.nbrbExchange.nbrbCurrencyExchangeBot.service;

import ru.nbrbExchange.nbrbCurrencyExchangeBot.entity.Currency;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.service.imp.SelectedCurrencyServiceImp;

public interface SelectedCurrencyService {

    static SelectedCurrencyService getInstance() {
        return new SelectedCurrencyServiceImp();
    }

    Currency getFromCurrency(long chatId);

    Currency getToCurrency(long chatId);

    void setFromCurrency(long chatId, Currency currency);

    void setToCurrency(long chatId, Currency currency);
}
