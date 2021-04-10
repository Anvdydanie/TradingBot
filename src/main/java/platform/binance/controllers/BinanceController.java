package platform.binance.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import platform.binance.services.BinanceService;

@RestController
@RequestMapping("/binance")
public class BinanceController {

    @GetMapping(value = "/data", produces = "application/json")
    public String getData() {
        BinanceService binanceService = new BinanceService();
        return binanceService.getData();
    }

}