package platform.binance.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;

public interface Request {

    /**
     * Метод преобразовывает объект в MultiValueMap
     * @return MultiValueMap
     */
    default MultiValueMap<String, String> toMultiValueMap() {
        ObjectMapper objectMapper = new ObjectMapper();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        Map<String, String> maps = objectMapper.convertValue(this, new TypeReference<>() {});
        parameters.setAll(maps);
        return parameters;
    }

}
