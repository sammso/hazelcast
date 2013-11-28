package com.liferay.portal.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.liferay.portal.cache.memcached.MemcachePortalCache;
import com.liferay.portal.kernel.cache.CacheListener;
import com.liferay.portal.kernel.cache.CacheListenerScope;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.impl.UserImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class HazelcastPortalCache implements PortalCache{

	public HazelcastPortalCache(
			String name, HazelcastInstance hazelcastInstance, int timeout,
			TimeUnit timeoutTimeUnit) {

			_name = name;
			_hazelcastInstance = hazelcastInstance;
			_cacheManager = _hazelcastInstance.getMap(name);
			_timeout = timeout;
			_timeoutTimeUnit = timeoutTimeUnit;
			 
			
		      
			
		}

		public void destroy() {
          
			_hazelcastInstance.getLifecycleService().shutdown();			
		
		}

		public Collection<Object> get(Collection<Serializable> keys) {
			

			Set<Future<Object>> futureObjects = new HashSet<Future<Object>>(); 
			for (Serializable serializable : keys) {
				
			
			try {
				futureObjects.add( _cacheManager.getAsync((String)serializable));
				
			}
			catch (IllegalArgumentException iae) {
				if (_log.isWarnEnabled()) {
					_log.warn("Error retrieving with keys " + keys, iae);
				}

				return null;
			}
			}
			Set<Object> values = new HashSet<Object>();
            for (Future<Object> futureObject : futureObjects) {
				
			
			try {
				
				values.add( futureObject.get(_timeout, _timeoutTimeUnit));
			}
			catch (Throwable t) {
				if (_log.isWarnEnabled()) {
					_log.warn("Memcache operation error", t);
				}

				futureObject.cancel(true);
			}
            }
			if (values != null && !values.isEmpty()) {
				return values;
			}

			return null;
		}

		public Object get(Serializable key) {
			
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(UserImpl.class.getClassLoader());
			Object obj = null;
			
			try{
			
			 obj = _cacheManager.get(String.valueOf(key));
			
			} catch(Throwable t){
					if (_log.isWarnEnabled()) {
						_log.warn("Error storing value with key " + key);
					}
			}
			  finally{
				
				  Thread.currentThread().setContextClassLoader(cl);
			  }
			
			return obj;
		
		}

		public String getName() {
			return _name;
		}

		public void put(Serializable key, Object value) {
			put(key, value, _timeToLive);
		}

		public void put(Serializable key, Object value, int timeToLive) {
			if(!(value instanceof Serializable )){
				_log.warn("Object not serializable, cannot be added to the cache");
				return;
			}
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				
				Thread.currentThread().setContextClassLoader(UserImpl.class.getClassLoader());
			
				_cacheManager.put(String.valueOf(key),  value, timeToLive,TimeUnit.SECONDS);
				
				
			}
			catch (IllegalArgumentException iae) {
				if (_log.isWarnEnabled()) {
					_log.warn("Error storing value with key " + key);
				}
			} catch(Throwable t){
				if (_log.isWarnEnabled()) {
					_log.warn("Error storing value with key " + key);
				}
			}finally{
				Thread.currentThread().setContextClassLoader(cl);
			}
		}

		public void put(Serializable key, Serializable value) {
			put(key, value, _timeToLive);
		}

		public void put(Serializable key, Serializable value, int timeToLive) {

			try {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(UserImpl.class.getClassLoader());
				_cacheManager.put(String.valueOf(key),  value, timeToLive,_timeoutTimeUnit);
				Thread.currentThread().setContextClassLoader(cl);
			}
			catch (IllegalArgumentException iae) {
				if (_log.isWarnEnabled()) {
					_log.warn("Error storing value with key " + key);
				}
			}
			catch(Throwable t){
				if (_log.isWarnEnabled()) {
					_log.warn("Error storing value with key " + key);
				}
			}
		}

		public void registerCacheListener(CacheListener cacheListener) {
			registerCacheListener(cacheListener, CacheListenerScope.ALL);
		}

		public void registerCacheListener(
			CacheListener cacheListener, CacheListenerScope cacheListenerScope) {
			_log.warn("This method is not implemented by hazelcast");
		}

		public void remove(Serializable key) {

			try {
				_cacheManager.remove(String.valueOf(key));
			}
			catch (IllegalArgumentException iae) {
				if (_log.isWarnEnabled()) {
					_log.warn("Error removing value with key " + key, iae);
				}
			}catch(Throwable t){
				if (_log.isWarnEnabled()) {
					_log.warn("Error storing value with key " + key, t);
				}
			}
		}

		public void removeAll() {
			_cacheManager.flush();
		}

		public void setTimeToLive(int timeToLive) {
			_timeToLive = timeToLive;
		}

		public void unregisterCacheListener(CacheListener cacheListener) {
		}

		public void unregisterCacheListeners() {
		}

		private static Log _log = LogFactoryUtil.getLog(MemcachePortalCache.class);
        private IMap<String, Object> _cacheManager = null;
		private HazelcastInstance _hazelcastInstance;
		private String _name;
		private int _timeout;
		private TimeUnit _timeoutTimeUnit;
		private int _timeToLive;

}
