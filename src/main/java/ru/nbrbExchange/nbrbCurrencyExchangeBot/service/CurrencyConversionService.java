package ru.nbrbExchange.nbrbCurrencyExchangeBot.service;

import ru.nbrbExchange.nbrbCurrencyExchangeBot.entity.Currency;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.service.imp.CurrencyConversionServiceImp;

public interface CurrencyConversionService {

    static CurrencyConversionService getInstance() {
        return new CurrencyConversionServiceImp();
    }

    double getConversionRatio(Currency from, Currency to);

}
