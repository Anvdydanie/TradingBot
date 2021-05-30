package platform.binance.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TelegramResponse {
    @JsonProperty(value = "ok")
    private boolean ok;
    @JsonProperty(value = "description")
    private String description;
}
