package com.corp.flume.sink;

import java.util.HashMap;
import java.util.Map;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.bson.Document;

import com.google.common.base.Throwables;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoSink extends AbstractSink implements Configurable {
	private String CONF_URL = "connectUrl";
	private String CONF_DB = "db";
	private String CONF_COL = "collection";
	private String CONF_SELECTION = "selector";
	private String CONF_BATCH = "batch";
	
	private String KEY_DEFAULT = "default";
	
	private Context context;
	
	private String defaultDb;
	private String defaultCol;
	private String selection = "source";
	private int batch = 1;
	
	private MongoClient mongoClient;
	private Map<String, IDocumentConverter> converters = new HashMap<>();

	@Override
	public synchronized void start() {
		super.start();
		String connectUrl = context.getString(CONF_URL);
		loadDefault();
		loadConverters();
		
		mongoClient = new MongoClient(new MongoClientURI(connectUrl));
	}
	
	private void loadDefault() {
		defaultDb = context.getString(CONF_DB);
		defaultCol = context.getString(CONF_COL);
		batch = context.getInteger(CONF_BATCH, batch);
		selection = context.getString(CONF_SELECTION, selection);
		
		System.out.println(">>>>>>> MongoSink");
		System.out.println(">>>>>>> DB=" + defaultDb);
		System.out.println(">>>>>>> COL=" + defaultCol);
		System.out.println(">>>>>>> BATCH=" + batch);
		System.out.println(">>>>>>> SELECTOR=" + selection);
	}
	
	private void loadConverters() {
		Map<String, String> map = context.getParameters();
		String type;
		String className;
		for (String key : map.keySet()) {
			if (key.startsWith("converter.")) {
				type = key.substring("converter.".length());
				className = map.get(key);
				try
				{
					Class<IDocumentConverter> clazz = (Class<IDocumentConverter>)Class.forName(className);
					converters.put(type, clazz.newInstance());
				}
				catch (Exception ex)
				{
					throw new RuntimeException(ex);
				}
			}
		}
		
		if (!converters.containsKey(KEY_DEFAULT)) {
			converters.put(KEY_DEFAULT, new JsonDocumentConverter());
		}
	}

	@Override
	public synchronized void stop() {
		mongoClient.close();
		super.stop();
	}

	@Override
	public Status process() throws EventDeliveryException {
		Status result = Status.READY;
		Channel channel = getChannel();
		Transaction transaction = null;
		Event event = null;
		try {
			long processedEvents = 0;
			transaction = channel.getTransaction();
			transaction.begin();

			for (; processedEvents < batch; processedEvents += 1) {
				event = channel.take();

				if (event == null) {
					// no events available in channel
					if (processedEvents == 0) {
						result = Status.BACKOFF;
					}
					break;
				}

				Map<String, String> headers = event.getHeaders();
				String db = defaultDb;
				String col = defaultCol;
				IDocumentConverter converter = null;
				if (headers.containsKey(CONF_DB))
				{
					db = headers.get(CONF_DB);
				}
				if (headers.containsKey(CONF_COL))
				{
					col = headers.get(CONF_COL);
				}
				if (headers.containsKey(selection)) {
					converter = converters.get(headers.get(selection));
				}

				if (null == converter) {
					converter = converters.get(KEY_DEFAULT);
				}
				MongoDatabase dbObj = mongoClient.getDatabase(db);
				MongoCollection colObj = dbObj.getCollection(col);
				Map<String, Object> map = converter.convert(event);
				Document doc = new Document();
				doc.putAll(map);
				colObj.insertOne(doc);
			}
			transaction.commit();

		} catch (Exception ex) {
			result = Status.BACKOFF;
			if (transaction != null) {
				try {
					transaction.rollback();
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
			throw new EventDeliveryException("Error in Processing Mongo", ex);
		} finally {
			if (transaction != null) {
				transaction.close();
			}
		}
		return result;
	}

	@Override
	public void configure(Context context) {
		this.context = context;
	}
}
