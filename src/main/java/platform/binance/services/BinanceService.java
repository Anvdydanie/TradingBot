package platform.binance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import platform.binance.configs.BinanceConfig;
import platform.binance.request.BalanceRequest;
import platform.binance.request.PriceRequest;
import platform.binance.response.BalanceResponse;
import platform.binance.response.PriceResponse;
import platform.binance.response.SystemStatusResponse;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

@Service
public class BinanceService {

    @Autowired
    private BinanceConfig config;

    @Autowired
    private TimerTaskService timerTaskService;

    @Autowired
    private WebExchangeService exchangeService;

    /**
     * переменная хранит данные по цене крипты каждые n секунд за час
      */
    public Map<String, List<BigDecimal>> priceByToolHourly = new HashMap<>();

    /**
     * переменная хранит данные по цене крипты каждые 2 минуты за сутки
     */
    public Map<String, List<BigDecimal>> priceByToolDaily = new HashMap<>();

    /**
     * переменная хранит данные по цене крипты каждый час за неделю
     */
    public Map<String, List<BigDecimal>> priceByToolWeekly = new HashMap<>();

    /**
     * переменная хранит данные по цене крипты каждый день за месяц
     */
    public Map<String, List<BigDecimal>> priceByToolMonthly = new HashMap<>();

    /**
     * Метод возвращает статус работоспособности системы
     * @return SystemStatusResponse
     */
    public SystemStatusResponse apiStatus() {
        return (SystemStatusResponse) exchangeService.sendHttpRequest(
            WebExchangeService.ApiMethods.SYSTEM_STATUS,
            null,
            new SystemStatusResponse()
        );
    }

    /**
     * Метод возвращает баланс аккаунта
     * @return BalanceResponse
     */
    public BalanceResponse accountBalance() {
        var balanceRequest = new BalanceRequest();
        balanceRequest.setSignature(
            config.signRequest(
                balanceRequest.stringToSign()
            )
        );

        var balanceResponse = (BalanceResponse) exchangeService.sendHttpRequest(
            WebExchangeService.ApiMethods.GET_BALANCE,
            balanceRequest.toMultiValueMap(),
            new BalanceResponse()
        );

        if (balanceResponse.getBalances().isEmpty()) {
            return balanceResponse;
        }

        balanceResponse.getBalances().removeIf(
            balanceData ->
                new BigDecimal(balanceData.getFree()).intValue() == 0
                && new BigDecimal(balanceData.getLocked()).intValue() == 0
        );

        return balanceResponse;
    }

    /**
     * Метод получения актуальной цены криптовалюты
     * @param symbol String
     * @return PriceResponse
     */
    public PriceResponse currentPrice(String symbol) {
        var priceRequest = new PriceRequest();
        priceRequest.setSymbol(symbol);

        return (PriceResponse) exchangeService.sendHttpRequest(
            WebExchangeService.ApiMethods.GET_PRICE,
            priceRequest.toMultiValueMap(),
            new PriceResponse()
        );
    }

    @PostConstruct
    private void init() {
        // TODO подключить sql и избавиться от этой схемы
        for (String cryptoCurrency : config.getCryptoList()) {
            priceByToolHourly.put(cryptoCurrency, new ArrayList<>());
            priceByToolDaily.put(cryptoCurrency, new ArrayList<>());
            priceByToolWeekly.put(cryptoCurrency, new ArrayList<>());
            priceByToolMonthly.put(cryptoCurrency, new ArrayList<>());
        }

        var timer = new Timer();
        timer.schedule(timerTaskService, 0, BinanceConfig.MaxPriceElementsInList.HOURLY.intervalInMilliSeconds);
    }

}
