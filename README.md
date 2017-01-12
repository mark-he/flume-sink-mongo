# flume-sink-mongo
A very simple mongo sink.

It supports mongo cluster, dynamic db/collection in event header, customed document converter etc.

# Configuration

	agent.sinks.[sink].type=com.corp.flume.sink.MongoSink
	#Connect URL for Mongo
	agent.sinks.[sink].connectUrl=mongodb://[username]:[password]@localhost:27017/[db]
	
	#Default db/collection to store data. We can also override these setting by adding into event header with key-value of db or collection
	agent.sinks.[sink].db=monitor
	agent.sinks.[sink].collection=test

	#By default we will use JsonDocumentConverter to format the event body. We can also specify a selector key in event header.
	#We can define a Converter with selector value = default to cover the default JsonDocumentConverter.
	#agent.sinks.[sink].selector=[selector key in event header]
	#agent.sinks.[sink].converter.[selector value]=com.corp.flume.sink.JsonDocumentConverter

	agent.sinks.[sink].channel = event
