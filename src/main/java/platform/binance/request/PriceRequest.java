package platform.binance.request;

import lombok.Data;

@Data
public class PriceRequest implements Request {
    private String symbol;
}
