package platform.binance.request;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class BalanceRequest implements Request {
    private final long timestamp;
    private String signature;

    public BalanceRequest() {
        // -1000 - коррекция времени (я хз, где сервак у бинанса)
        timestamp = ZonedDateTime.now().toInstant().toEpochMilli() - 1000;
    }

    public String stringToSign() {
        return "timestamp=".concat(String.valueOf(timestamp));
    }
}
