package com.neueda.trade.injector;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.trade.server.model.DtoFactory;
import com.neueda.trade.server.model.Trade;
import com.neueda.trade.server.model.TradeState;

@Component
@Profile("!injector-activemq")
public class RestClient implements InjectorClient {

    @Autowired
    private DtoFactory factory;
    
    @Autowired
    @Value("${trade.server.url:http://localhost:8080/trades}")
    private String tradeUrl;
    
	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);
	
    private RestTemplate restTemplate = new RestTemplate();
    
    public RestClient() {
    	restTemplate.setErrorHandler(new RestErrorHandler());
    	restTemplate.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter()));
	}

	public Trade findTrade(int id) {
        Trade trade = restTemplate.getForObject("{tradeUrl}/find/{id}", factory.tradeClass(), tradeUrl, id);
		logger.info("Found trade: {}", trade);	
		return trade;
	}

	
	public void place(Trade trade) {
        Trade response = restTemplate.postForObject("{tradeUrl}/place", trade, factory.tradeClass(), tradeUrl);
		logger.info("Place trade: {}", response);	
	}
	
	public void modify(String transid, double price, int volume) {
		Trade trade = factory.createTrade(transid, null, null, price, volume, null);
        Trade response = restTemplate.postForObject("{tradeUrl}/modify/{transid}",  
        											trade, factory.tradeClass(), tradeUrl, transid);
		logger.info("Modify trade: {}", response);	
	}

	public void updateState(String transid, TradeState state) {
        Trade response = restTemplate.getForObject("{tradeUrl}/{state}/{transid}", 
        		                                   factory.tradeClass(), tradeUrl, state.name().toLowerCase(), transid);
		logger.info("{} trade: {}", state, response);	
	}

	@SuppressWarnings("unchecked")
	private static class RestErrorHandler extends DefaultResponseErrorHandler {
		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			logger.info("Http error: {}", response);	
			Map<String, Map<String,String>> map = new ObjectMapper().readValue(response.getBody(), HashMap.class);
			throw new InjectException("REST error: %s", map.get("error").get("message"));
		}
	}

}
