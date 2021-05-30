package platform.binance.strategies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import platform.binance.configs.BinanceConfig;
import platform.binance.services.BinanceService;
import platform.binance.services.TelegramService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MurrayLevelsStrategy {

    private List<BigDecimal> levelsList = new ArrayList<>();

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private TelegramService telegramService;

    /*private enum LevelsInfo {
        LEVEL_1("8/8 0/8", "Эти уровни выступают очень сильным препятствием для дальнейшего движения через них. Как правило, пробить данные уровни может сильная новость. Чаще трейдеры ждут разворот от данной области, либо, как минимум, серьезную коррекцию. Также есть мнение, что отскок вверх от уровня [0/8] имеет более высокую вероятность, нежели отскок вниз от уровня [8/8]. Чаще именно уровень [8/8] пробивается ценами вверх и тренд продолжается."),
        LEVEL_2("7/8 1/8", "Эти уровни дают понимание того, ждать ли начало нового импульса или движение рынка было случайным и цены уйдут в коррекцию. если цена сильно оттолкнулась от уровня [4/8] вверх и достигла уровня [7/8], однако дальнейшего движения вверх не происходит, и цена всего лишь тестирует [7/8], тогда стоит ожидать падение обратно к уровню [4/8]. Если же цена уверенно пробивает [7/8], в этом случае стоит говорить о сильном тренде на рынке, и ожидать высокую вероятность продолжения подъёма цены до уровня [8/8] и выше. Для уровня [1/8] все с точностью до наоборот."),
        LEVEL_3("6/8 2/8", "Достаточно сильные уровни, трейдеры сравнивают их с уровнями [8/8], [0/8] и линией баланса [4/8]. Уровни будут очень сильными, если после отскока от них цена не может пробить эти уровни ценами закрытия"),
        LEVEL_4("5/8 3/8", "Эти уровни обозначают боковое движение на рынке, в котором он пребывает порядка большую часть времени. Если удается вытолкнуть цену за пределы этих уровней, то стоит ожидать продолжение движения в сторону пробоя. Ближайшей целью роста в случае пробоя уровня [5/8] выступает уровень [8/8]. Если же цены пробили линию [3/8], тогда ожидаем падение до уровня [0/8] с высокой вероятностью"),
        LEVEL_5("4/8", "Если цена располагается над уровнем [4/8], то эта область выступает хорошей поддержкой. Если ниже [4/8],линия выступает сильным уровнем сопротивления."),
        LEVEL_6("-1/8 +1/8", "Эти уровни используются для попытки поймать разворот тренда. Уровень [-1/8] представляет собой экстремальную поддержку в момент медвежьего тренда. Уровень [+1/8] указывает, где располагается экстремальное сопротивление в момент бычьего тренда. Тест этих уровней говорит об ослаблении текущего тренда, как правило, сильных разворотов не происходит, а рынок переходит в стадию коррекции к уровням [0/8] и [8/8]"),
        LEVEL_7("-2/8 +2/8", "Пробой этих уровней указывает на очень сильный тренд на рынке. Уровень [-2/8] представляет собой конечную поддержку при нисходящем тренде. Уровень [+2/8] указывает на конечное сопротивления во время восходящей тенденции. ");
    }*/

    /**
     * Описание уровней Мюррея
     */
    private enum MurrayLevels {
        EXTREMELY_OVERBOUGHT("+2/8", "ПРОДАВАТЬ! (Перестроение линий)"),
        OVERBOUGHT("+1/8", "Лучше продавать"),
        ULTIMATE_RESISTANCE("8/8", "Может произойти коррекция или падение"),
        WEAK_RESISTANCE("7/8", "Пробитие уровня говорит о восходящем тренде. В противном случае должна произойти коррекция к 4/8"),
        PIVOT_SELL("6/8", "Опорная продажа, если уровень не удается пробить вверх"),
        RANGE_TOP("5/8", "В случае пробоя можно ожидать повышение до уровня 8/8"),
        MIDDLE_LINE("4/8", "Хорошая поддержка или сопротивление"),
        RANGE_BOTTOM("3/8", "Можно ожидать падение до уровня 0/8"),
        PIVOT_BUY("2/8", "Опорная покупка, если уровень не удается пробить вниз"),
        WEAK_SUPPORT("1/8", "Пробитие уровня говорит о нисходящем тренде. В противном случае должна произойти коррекция к 4/8"),
        ULTIMATE_SUPPORT("0/8", "отскок вверх от уровня имеет высокую вероятность"),
        OVERSOLD("-1/8", "Лучше покупать"),
        EXTREMELY_OVERSOLD("-2/8", "ПОКУПАТЬ! (Перестроение линий)");

        public final String level;
        public final String description;
        MurrayLevels(String level, String description) {
            this.level = level;
            this.description = description;
        }
    }

    /**
     * Стратегия для 1 часа строится на 64 свечах за час
     */
    public void strategyForHour(String cryptoCurrency, BigDecimal currentPrice, BigDecimal previousPrice) {
        // рассчитываем где средняя линию
        var middleLevelIndex = (int) Math.round( (double) MurrayLevels.values().length / (double) 2 );
        // Строим уровни мюррея
        if (levelsList.isEmpty() && binanceService.priceByToolHourly.get(cryptoCurrency).size() >= BinanceConfig.MaxPriceElementsInList.HOURLY.maxValue) {
            var maxPrice = Collections.max(binanceService.priceByToolHourly.get(cryptoCurrency));
            var minPrice = Collections.min(binanceService.priceByToolHourly.get(cryptoCurrency));
            var step = maxPrice.subtract(minPrice).divide(BigDecimal.valueOf(9), 4, RoundingMode.UP);

            for (var i = 0; i < MurrayLevels.values().length; i++) {
                levelsList.add(maxPrice.add(step.multiply(BigDecimal.valueOf(2))).subtract(step.multiply(BigDecimal.valueOf(i))));
            }
        } else {
            return;
        }

        var nearestLevelIndex = getNearestIndex(levelsList, currentPrice);
        // если уровень был пробит вниз до 4/8 или ниже
        if (
            currentPrice.compareTo(levelsList.get(nearestLevelIndex)) <= 0
            && previousPrice.compareTo(levelsList.get(nearestLevelIndex)) > 0
            && nearestLevelIndex >= middleLevelIndex
        ) {
            telegramService.sendMessage(
                "По " + cryptoCurrency + ": \n"
                + "был пробит уровень " + MurrayLevels.values()[nearestLevelIndex].level + " вниз. \n"
                + "Совет: " + MurrayLevels.values()[nearestLevelIndex].description + ". \n"
                + "Цена уровня: " + levelsList.get(nearestLevelIndex) + " рублей. \n"
                + "Текущая цена: " + currentPrice + " рублей."
            );
        }

        // если уровень был пробит вверх до 4/8 или выше
        if (
            currentPrice.compareTo(levelsList.get(nearestLevelIndex)) >= 0
            && previousPrice.compareTo(levelsList.get(nearestLevelIndex)) < 0
            && nearestLevelIndex <= middleLevelIndex
        ) {
            telegramService.sendMessage(
                "По " + cryptoCurrency + ": \n"
                + "был пробит уровень " + MurrayLevels.values()[nearestLevelIndex].level + " вверх. \n"
                + "Совет: " + MurrayLevels.values()[nearestLevelIndex].description + ". \n"
                + "Цена уровня: " + levelsList.get(nearestLevelIndex) + " рублей. \n"
                + "Текущая цена: " + currentPrice + " рублей."
            );
        }

        // если пробиты критические уровни +2 или -2, то корректируем уровни
        if (
            nearestLevelIndex == 0
            && currentPrice.compareTo(levelsList.get(nearestLevelIndex)) > 0
            || nearestLevelIndex == levelsList.size() - 1
            && currentPrice.compareTo(levelsList.get(nearestLevelIndex)) < 0
        ) {
            for (var i = 0; i < MurrayLevels.values().length; i++) {
                levelsList = new ArrayList<>();
            }
        }
    }

    /**
     * Метод возвращает индекс ближайшего уровня
     * @param levelsList ArrayList<BigDecimal>
     * @param currentPrice BigDecimal
     * @return int
     */
    private int getNearestIndex(List<BigDecimal> levelsList, BigDecimal currentPrice) {
        var index = 0;
        var difference = currentPrice.subtract(levelsList.get(0)).abs();
        for (var i = 0; i < levelsList.size(); i++) {
            var currentDifference = currentPrice.subtract( levelsList.get(i) ).abs();
            if (difference.compareTo(currentDifference) > 0) {
                difference = currentDifference;
                index = i;
            }
        }
        return index;
    }

}
