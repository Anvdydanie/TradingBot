package platform.binance.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import platform.binance.services.BinanceService;
import platform.binance.services.TelegramService;
import java.lang.reflect.Method;
import java.util.*;

@RestController
@RequestMapping("/binance")
public class BinanceController {

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private TelegramService telegramService;

    @GetMapping(value = "/help", produces = "application/json")
    public Object getHelp() {
        Map<String, String[]> methodsMap = new HashMap<>();

        Class<BinanceController> clazz = BinanceController.class;
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                methodsMap.put(method.getName(), method.getAnnotation(GetMapping.class).value());
            }
        }

        return methodsMap;
    }

    @GetMapping(value = "/get-api-status", produces = "application/json")
    public Object getApiStatus() {
        return binanceService.apiStatus();
    }

    @GetMapping(value = "/get-account-balance", produces = "application/json")
    public Object getAccountBalance() {
        return binanceService.accountBalance();
    }

    @GetMapping(value = "/get-price", produces = "application/json")
    public Object getPrice(
        @RequestParam("symbol") String symbol
    ) {
        return binanceService.currentPrice(symbol);
    }

    @GetMapping(value = "/send-test-message", produces = "application/json")
    public void telegramTestMessage() {
        telegramService.sendMessage("test");
    }

}