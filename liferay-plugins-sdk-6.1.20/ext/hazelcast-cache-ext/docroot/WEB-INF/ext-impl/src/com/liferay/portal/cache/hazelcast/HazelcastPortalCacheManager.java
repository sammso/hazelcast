package com.liferay.portal.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheManager;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PropsUtil;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;





public class HazelcastPortalCacheManager implements PortalCacheManager {

	
	public void afterPropertiesSet() {
		String configurationPath = PropsUtil.get(_configPropertyKey);

		if (Validator.isNull(configurationPath)) {
			configurationPath = _DEFAULT_CLUSTERED_HAZELCAST_CONFIG_FILE;
		}

		_usingDefault = configurationPath.equals(
			_DEFAULT_CLUSTERED_HAZELCAST_CONFIG_FILE);

		Config configuration = new Config();
		if(!_usingDefault){
			configuration.setConfigurationUrl(HazelcastPortalCacheManager.class.getResource(configurationPath));
		}
		_hazelcastInstance = Hazelcast.newHazelcastInstance(configuration);

	}
	
	public void clearAll() {
		try {
			//destroy();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		_hazelcastPortalCaches.clear();
	}

	public void destroy() throws Exception {
		for (HazelcastPortalCache hazelcastPortalCache :
				_hazelcastPortalCaches.values()) {
			
			hazelcastPortalCache.destroy();
		}
		Hazelcast.shutdownAll();
		
	}

	public PortalCache getCache(String name) {
		return getCache(name, false);
	}

	public PortalCache getCache(String name, boolean blocking) {
		HazelcastPortalCache hazelcastPortalCache = _hazelcastPortalCaches.get(
			name);

		if (hazelcastPortalCache == null) {
			synchronized(_hazelcastInstance){
				if (hazelcastPortalCache == null) {
					try {
						hazelcastPortalCache = new HazelcastPortalCache(
							name,_hazelcastInstance, _timeout, _timeoutTimeUnit);
		
						_hazelcastPortalCaches.put(name, hazelcastPortalCache);
					}
					catch (Exception e) {
						throw new IllegalStateException(
							"Unable to initiatlize Memcache connection", e);
					}
				}
			}
		}

		return hazelcastPortalCache;
	}

	public void reconfigureCaches(URL configurationURL) {
	}

	public void removeCache(String name) {
		_hazelcastPortalCaches.remove(name);
	}
	
	public void setConfigPropertyKey(String configPropertyKey) {
		_configPropertyKey = configPropertyKey;
	}

	public void setTimeout(int timeout) {
		_timeout = timeout;
	}

	public void setTimeoutTimeUnit(String timeoutTimeUnit) {
		_timeoutTimeUnit = TimeUnit.valueOf(timeoutTimeUnit);
	}
	private static final String _DEFAULT_CLUSTERED_HAZELCAST_CONFIG_FILE =
			"hazelcast.xml";
	private String _configPropertyKey;
	private Map<String, HazelcastPortalCache> _hazelcastPortalCaches =
		new ConcurrentHashMap<String, HazelcastPortalCache>();
	private HazelcastInstance _hazelcastInstance = null;
	private int _timeout;
	private TimeUnit _timeoutTimeUnit;
	private boolean _usingDefault;
}
