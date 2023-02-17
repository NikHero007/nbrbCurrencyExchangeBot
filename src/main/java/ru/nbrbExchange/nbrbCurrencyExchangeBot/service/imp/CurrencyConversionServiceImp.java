package ru.nbrbExchange.nbrbCurrencyExchangeBot.service.imp;

import org.json.JSONException;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.entity.Currency;
import ru.nbrbExchange.nbrbCurrencyExchangeBot.service.CurrencyConversionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.json.JSONObject;

public class CurrencyConversionServiceImp implements CurrencyConversionService {

    public CurrencyConversionServiceImp() {
    }

    @Override
    public double getConversionRatio(Currency from, Currency to) {
        double fromRate = 0;
        double toRate = 0;
        try {
            fromRate = getRate(from);
            toRate = getRate(to);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        return fromRate / toRate;
    }

    private double getRate(Currency currency) throws IOException, JSONException {
        if (currency == Currency.BYN) {
            return 1;
        }
        URL url = new URL("https://www.nbrb.by/api/exrates/rates/" + currency.getId());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject json = new JSONObject(response.toString());
        double rate = json.getDouble("Cur_OfficialRate");
        double scale = json.getDouble("Cur_Scale");
        return rate / scale;
    }


}
