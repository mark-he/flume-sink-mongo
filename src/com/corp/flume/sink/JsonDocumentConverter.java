package com.corp.flume.sink;

import java.util.Map;

import org.apache.flume.Event;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JsonDocumentConverter implements IDocumentConverter {
	private ObjectMapper mapper = new ObjectMapper();
	@Override
	public Map<String, Object> convert(Event event) throws Exception {
		return mapper.readValue(event.getBody(), new TypeReference<Map<String, Object>>(){});
	}
}
