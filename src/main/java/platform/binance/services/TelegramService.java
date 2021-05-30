package platform.binance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import platform.binance.configs.BinanceConfig;
import platform.binance.response.TelegramResponse;
import java.security.ProviderException;

@Service
public class TelegramService {

    @Autowired
    private BinanceConfig config;

    @Autowired
    private WebExchangeService webExchangeService;

    /**
     * Метод отправляет сообщение в телеграм группу
     * @param message String
     */
    public void sendMessage(String message) {
        var uriComponents = UriComponentsBuilder.newInstance()
            .scheme("https")
            .host("api.telegram.org")
            .path("/{token}/sendMessage")
            .queryParam("chat_id", config.getTelegramChatId())
            .queryParam("text", message)
            .buildAndExpand(config.getTelegramBotToken());

        try {
            HttpEntity<TelegramResponse> response = webExchangeService.getRestTemplate().exchange(
                uriComponents.toUriString(),
                HttpMethod.GET,
                null,
                TelegramResponse.class
            );

            if (response.getBody() == null) {
                throw new ProviderException("empty response from telegram");
            }

            if (!response.getBody().isOk()) {
                throw new ProviderException(response.getBody().getDescription());
            }
        } catch (Exception exception) {
            // TODO писать в лог ошибку

        }

    }

}
