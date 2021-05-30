package platform.binance.response;

import lombok.Data;

@Data
public class PriceResponse {
    private String symbol;
    private String price;
}
