package platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import platform.binance.configs.BinanceConfig;

@SpringBootApplication
@EnableConfigurationProperties(BinanceConfig.class)
public class ServiceApplication {

    private final BinanceConfig configuration;

    public ServiceApplication(BinanceConfig configuration) {
        this.configuration = configuration;
    }

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
