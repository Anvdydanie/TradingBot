package platform.binance.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SystemStatusResponse {
    public static final String SUCCESS_RESPONSE = "0";

    @JsonProperty("status")
    private String status;
    @JsonProperty("msg")
    private String message;
}
