package com.corp.flume.sink;

import java.util.Map;

import org.apache.flume.Event;

public interface IDocumentConverter {
	Map<String, Object> convert(Event event) throws Exception;
}
