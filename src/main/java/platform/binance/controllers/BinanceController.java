package platform.binance.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import platform.binance.services.BinanceService;

@RestController
@RequestMapping("/binance")
public class BinanceController {

    @Autowired
    private BinanceService binanceService;

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

}