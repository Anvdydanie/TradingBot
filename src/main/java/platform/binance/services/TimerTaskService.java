package platform.binance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import platform.binance.configs.BinanceConfig;
import platform.binance.response.PriceResponse;
import platform.binance.strategies.MurrayLevelsStrategy;
import java.math.BigDecimal;
import java.util.TimerTask;

@Service
public class TimerTaskService extends TimerTask {

    @Autowired
    private BinanceConfig config;

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private MurrayLevelsStrategy murrayLevelsStrategy;

    public void run() {
        // TODO параллельно идти по списку
        config.getCryptoList().forEach(cryptoCurrency -> {
            PriceResponse response = binanceService.currentPrice(cryptoCurrency);
            if (response.getPrice() == null || response.getPrice().isEmpty()) {
                // TODO писать в лог о проблеме
                return;
            }

            var currencyPriceList = binanceService.getPriceByToolHourly().get(cryptoCurrency);
            // если количество точек не накопилось
            if (currencyPriceList.size() >= BinanceConfig.MaxPriceElementsInList.HOURLY.maxValue) {
                currencyPriceList.remove(0);
            }
            currencyPriceList.add( new BigDecimal( response.getPrice() ) );

            // ведем расчет стратегии
            if (currencyPriceList.size() == BinanceConfig.MaxPriceElementsInList.HOURLY.maxValue) {
                var i = 1;
                // находим предыдущую цену различную с текущей
                var currentPrice = new BigDecimal(response.getPrice());
                var previousPrice = new BigDecimal(response.getPrice());
                while (currentPrice.compareTo(previousPrice) == 0) {
                    previousPrice = currencyPriceList.get(currencyPriceList.size() - i);
                    if (currencyPriceList.size() == i) {
                        break;
                    }
                    i++;
                }

                if (currentPrice.compareTo(previousPrice) != 0) {
                    return;
                }

                murrayLevelsStrategy.strategyForHour(
                    cryptoCurrency,
                    currencyPriceList.get(currencyPriceList.size() - 1),
                    currencyPriceList.get(currencyPriceList.size() - 2)
                );
            }
        });
    }

}
