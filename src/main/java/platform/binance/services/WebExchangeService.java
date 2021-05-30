package platform.binance.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import platform.binance.configs.BinanceConfig;
import platform.binance.response.SystemStatusResponse;
import javax.annotation.PostConstruct;
import java.security.ProviderException;

@Service
public class WebExchangeService {
    private String activeApiUrl;
    private RestTemplate restTemplate;
    private boolean stopAllRequests = false;
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    @Autowired
    private BinanceConfig config;

    @Autowired
    private TelegramService telegramService;

    /**
     * Методы в API
     */
    public enum ApiMethods {
        // получение статуса работоспособности системы бинанс
        SYSTEM_STATUS(HttpMethod.GET, "/sapi/v1/system/status"),
        // получение текущего баланса
        GET_BALANCE(HttpMethod.GET, "/api/v3/account"),
        // получение текущей цены криптовалюты
        GET_PRICE(HttpMethod.GET, "/api/v3/ticker/price");

        public final HttpMethod requestType;
        public final String methodUrl;
        ApiMethods(HttpMethod requestType, String methodUrl) {
            this.requestType = requestType;
            this.methodUrl = methodUrl;
        }
    }

    /**
     * Возможные коды ошибок от платформы Binance
     */
    private enum ErrorCodes {
        HTTP_4XX("4", "the issue is on the sender's side"),
        HTTP_403("403", "the WAF Limit (Web Application Firewall) has been violated"),
        HTTP_429("429", "breaking a request rate limit. Stop all requests!"),
        HTTP_418("418", "an IP has been auto-banned for continuing to send requests after receiving 429 codes"),
        HTTP_5XX("5", "the issue is on Binance's side"),
        HTTP_XXX("", "unknown error code");

        public String errorCode;
        public final String errorMessage;
        ErrorCodes(String errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        static ErrorCodes getByErrorCode(String errorCode) {
            for (ErrorCodes errorCodeData : ErrorCodes.values()) {
                if (errorCodeData.errorCode.equals(errorCode)) {
                    return errorCodeData;
                }
            }
            HTTP_XXX.errorCode = errorCode;
            return HTTP_XXX;
        }
    }

    /**
     * Все адреса API платформы Binance
     */
    private enum ApiUrls {
        MAIN_URL("https://api.binance.com"),
        // if there are performance issues with the endpoint above
        RESERVE_URL_1("https://api1.binance.com"),
        RESERVE_URL_2("https://api2.binance.com"),
        RESERVE_URL_3("https://api3.binance.com");

        public final String urlString;
        ApiUrls(String urlString) {
            this.urlString = urlString;
        }
    }

    /**
     * Метод ищет активный урл
     */
    private void findActiveApiUrl() {
        for (ApiUrls apiUrl : ApiUrls.values()) {
            activeApiUrl = apiUrl.urlString;
            SystemStatusResponse systemStatus = (SystemStatusResponse) sendHttpRequest(
                WebExchangeService.ApiMethods.SYSTEM_STATUS,
                null,
                new SystemStatusResponse()
            );

            if (
                systemStatus.getStatus() != null
                && systemStatus.getStatus().equals(SystemStatusResponse.SUCCESS_RESPONSE)
            ) {
                return;
            }
        }

        telegramService.sendMessage("все урлы Binance нерабочие");
    }

    /**
     * Метод настраивает RestTemplate клиента
     * @return RestTemplate
     */
    public RestTemplate getRestTemplate() {
        if (restTemplate != null) {
            return restTemplate;
        }

        class MyErrorHandler implements ResponseErrorHandler {
            @Override
            public void handleError(ClientHttpResponse response) {}
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        }
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new MyErrorHandler());
        return restTemplate;
    }

    // Метод для тестирования
    public void sendHttpRequest2(ApiMethods methodName, MultiValueMap<String, String> requestBodyMap) {
        try {
            HttpEntity<String> entityRequest;
            var headers = new HttpHeaders();
            headers.add("Content-Type", CONTENT_TYPE);
            headers.add("X-MBX-APIKEY", config.getApiKey());

            var uriBuilder = UriComponentsBuilder.fromHttpUrl(activeApiUrl + methodName.methodUrl);
            if (methodName.requestType == HttpMethod.GET) {
                requestBodyMap.forEach(uriBuilder::queryParam);
                entityRequest = new HttpEntity(null, headers);
            } else {
                entityRequest = new HttpEntity(requestBodyMap, headers);
            }
            System.out.println(uriBuilder.toUriString());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                uriBuilder.toUriString(),
                methodName.requestType,
                entityRequest,
                String.class
            );
            System.out.println(responseEntity.getBody());
        } catch (Exception exception) {
            System.out.println(exception.toString());
        }
    }


    /**
     * Метод осуществляет запрос методов API и первичную обработку ответа
     * @param methodName урл метода
     * @param requestBodyMap тело запроса MultiValueMap типа
     * @param responseObject ожидаемый ответ
     * @param <T> принимает любой класс ожидаемого ответа
     * @return Object
     */
    public <T> Object sendHttpRequest(ApiMethods methodName, MultiValueMap<String, String> requestBodyMap, T responseObject) {
        if (stopAllRequests) {
            return null;
        }

        HttpEntity<String> entityRequest;
        var headers = new HttpHeaders();
        headers.add("Content-Type", CONTENT_TYPE);
        headers.add("X-MBX-APIKEY", config.getApiKey());

        try {
            var uriBuilder = UriComponentsBuilder.fromHttpUrl(activeApiUrl + methodName.methodUrl);
            if (methodName.requestType == HttpMethod.GET && requestBodyMap != null) {
                requestBodyMap.forEach(uriBuilder::queryParam);
                entityRequest = new HttpEntity(null, headers);
            } else {
                entityRequest = new HttpEntity(requestBodyMap, headers);
            }

            ResponseEntity<?> responseEntity = restTemplate.exchange(
                uriBuilder.toUriString(),
                methodName.requestType,
                entityRequest,
                responseObject.getClass()
            );

            // обработка кода ответа
            var statusCode = String.valueOf(responseEntity.getStatusCodeValue());

            if (
                statusCode.equals(ErrorCodes.HTTP_403.errorCode)
                || statusCode.equals(ErrorCodes.HTTP_429.errorCode)
                || statusCode.equals(ErrorCodes.HTTP_418.errorCode)
            ) {
                stopAllRequests = true;

                // TODO A Retry-After header is sent with a 418 or 429 responses and will give the number of seconds required to wait,
                //  in the case of a 429, to prevent a ban, or, in the case of a 418, until the ban is over.

                throw new ProviderException(
                    ErrorCodes.getByErrorCode(statusCode).errorCode
                    + ErrorCodes.getByErrorCode(statusCode).errorMessage
                );
            }

            if (statusCode.substring(0,1).equals(ErrorCodes.HTTP_4XX.errorCode)) {
                throw new ProviderException(ErrorCodes.HTTP_4XX.errorMessage);
            }

            if (statusCode.substring(0,1).equals(ErrorCodes.HTTP_5XX.errorCode)) {
                findActiveApiUrl();
                throw new ProviderException(ErrorCodes.HTTP_5XX.errorMessage);
            }

            if (responseEntity.getBody() == null) {
                throw new ProviderException(ErrorCodes.HTTP_XXX.errorMessage);
            }

            return responseEntity.getBody();
        } catch (Exception exception) {
            telegramService.sendMessage(
                "При отправке запроса " + methodName.methodUrl + " получена ошибка: " + exception.getMessage()
            );
            return null;
        }
    }

    @PostConstruct
    private void init() {
        restTemplate = getRestTemplate();
        findActiveApiUrl();
    }

}
