package platform.binance.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("binance")
public class BinanceConfig {

    private String key;

    private String secret;

}