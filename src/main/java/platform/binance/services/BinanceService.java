package platform.binance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import platform.binance.configs.BinanceConfig;

@Service
public class BinanceService {

    @Autowired
    private BinanceConfig configuration;

    public String getData() {
        return configuration.getKey() + configuration.getSecret();
    }

}
