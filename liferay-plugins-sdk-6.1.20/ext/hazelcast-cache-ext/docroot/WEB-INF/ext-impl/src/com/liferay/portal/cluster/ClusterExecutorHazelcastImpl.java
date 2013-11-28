package com.liferay.portal.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.liferay.portal.kernel.cluster.Address;
import com.liferay.portal.kernel.cluster.ClusterEventListener;
import com.liferay.portal.kernel.cluster.ClusterException;
import com.liferay.portal.kernel.cluster.ClusterExecutor;
import com.liferay.portal.kernel.cluster.ClusterMessageType;
import com.liferay.portal.kernel.cluster.ClusterNode;
import com.liferay.portal.kernel.cluster.ClusterNodeResponse;
import com.liferay.portal.kernel.cluster.ClusterNodeResponses;
import com.liferay.portal.kernel.cluster.ClusterRequest;
import com.liferay.portal.kernel.cluster.ClusterResponseCallback;
import com.liferay.portal.kernel.cluster.FutureClusterResponses;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.executor.PortalExecutorManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.MethodHandler;
import com.liferay.portal.kernel.util.WeakValueConcurrentHashMap;
import com.liferay.portal.util.PortalPortEventListener;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class ClusterExecutorHazelcastImpl implements ClusterExecutor, PortalPortEventListener  {

	
	public static final String CLUSTER_EXECUTOR_CALLBACK_THREAD_POOL =
			"CLUSTER_EXECUTOR_CALLBACK_THREAD_POOL";

		public void addClusterEventListener(
			ClusterEventListener clusterEventListener) {

			if (!isEnabled()) {
				return;
			}

			_clusterEventListeners.addIfAbsent(clusterEventListener);
		}
//
//		@Override
//		public void afterPropertiesSet() {
//			if (PropsValues.CLUSTER_EXECUTOR_DEBUG_ENABLED) {
//				addClusterEventListener(new DebuggingClusterEventListenerImpl());
//			}
//
//			if (PropsValues.LIVE_USERS_ENABLED) {
//				addClusterEventListener(new LiveUsersClusterEventListenerImpl());
//			}
//
//			super.afterPropertiesSet();
//		}
//
		@Override
		public void destroy() {
			if (!isEnabled()) {
				return;
			}

			PortalExecutorManagerUtil.shutdown(
				CLUSTER_EXECUTOR_CALLBACK_THREAD_POOL, true);

		}

	
	protected ClusterNodeResponse runLocalMethod(MethodHandler methodHandler) {
		ClusterNodeResponse clusterNodeResponse = new ClusterNodeResponse();

		ClusterNode localClusterNode = getLocalClusterNode();

		clusterNodeResponse.setAddress(getLocalClusterNodeAddress());
		clusterNodeResponse.setClusterNode(localClusterNode);
		clusterNodeResponse.setClusterMessageType(ClusterMessageType.EXECUTE);

		if (methodHandler == null) {
			clusterNodeResponse.setException(
				new ClusterException(
					"Payload is not of type " + MethodHandler.class.getName()));

			return clusterNodeResponse;
		}

		try {
			Object returnValue = methodHandler.invoke(true);

			if (returnValue instanceof Serializable) {
				clusterNodeResponse.setResult(returnValue);
			}
			else if (returnValue != null) {
				clusterNodeResponse.setException(
					new ClusterException("Return value is not serializable"));
			}
		}
		catch (Exception e) {
			clusterNodeResponse.setException(e);
		}

		return clusterNodeResponse;
	}
		public FutureClusterResponses execute(ClusterRequest clusterRequest)
			throws SystemException {

			if (!isEnabled()) {
				return null;
			}

			List<Address> addresses = getAddresses();

			FutureClusterResponses futureClusterResponses =
				new FutureClusterResponses(addresses);

			
			
			if (!clusterRequest.isFireAndForget()) {
				String uuid = clusterRequest.getUuid();

				_futureClusterResponses.put(uuid, futureClusterResponses);
			}


			if (!clusterRequest.isSkipLocal() && _shortcutLocalMethod &&
				addresses.remove(getLocalClusterNodeAddress())) {

				ClusterNodeResponse clusterNodeResponse = runLocalMethod(
					clusterRequest.getMethodHandler());

				clusterNodeResponse.setMulticast(clusterRequest.isMulticast());
				clusterNodeResponse.setUuid(clusterRequest.getUuid());

				futureClusterResponses.addClusterNodeResponse(clusterNodeResponse);
			}

			_hazelcastInstance.getQueue("myQueue").add(clusterRequest);

			return futureClusterResponses;
		}

		public void execute(
				ClusterRequest clusterRequest,
				ClusterResponseCallback clusterResponseCallback)
			throws SystemException {
			FutureClusterResponses futureClusterResponses = execute(clusterRequest);
			ClusterResponseCallbackJob clusterResponseCallbackJob =
					new ClusterResponseCallbackJob(
						clusterResponseCallback, futureClusterResponses);

			_hazelcastInstance.getExecutorService().execute(clusterResponseCallbackJob);
		}

		public void execute(
				ClusterRequest clusterRequest,
				ClusterResponseCallback clusterResponseCallback, long timeout,
				TimeUnit timeUnit)
			throws SystemException {

			FutureClusterResponses futureClusterResponses = execute(clusterRequest);

			ClusterResponseCallbackJob clusterResponseCallbackJob =
				new ClusterResponseCallbackJob(
					clusterResponseCallback, futureClusterResponses, timeout,
					timeUnit);

			_executorService.execute(clusterResponseCallbackJob);
		}

		public List<ClusterEventListener> getClusterEventListeners() {
			if (!isEnabled()) {
				return Collections.emptyList();
			}

			return Collections.unmodifiableList(_clusterEventListeners);
		}

		public List<Address> getClusterNodeAddresses() {
			if (!isEnabled()) {
				return Collections.emptyList();
			}

			return getAddresses();
		}

		public List<ClusterNode> getClusterNodes() {
			if (!isEnabled()) {
				return Collections.emptyList();
			}
		    return getClusterNodesFromHazelcast();
		}

		private List<ClusterNode> getClusterNodesFromHazelcast() {
			
			List<ClusterNode> clusterNodes = new ArrayList<ClusterNode>(_hazelcastInstance.getCluster().getMembers().size());
			Set<Member> members = _hazelcastInstance.getCluster().getMembers();
			for (Member member : members) {
				ClusterNode clusterNode = new ClusterNode(member.getUuid());
				clusterNode.setHostName(member.getInetSocketAddress().getHostName());
				clusterNode.setInetAddress(member.getInetSocketAddress().getAddress());
				clusterNode.setPort(member.getInetSocketAddress().getPort());
				
				clusterNodes.add(clusterNode);
			}
			return clusterNodes;
		}

		public ClusterNode getLocalClusterNode() {
			if (!isEnabled()) {
				return null;
			}

			return _localClusterNode;
		}

		public Address getLocalClusterNodeAddress() {
			if (!isEnabled()) {
				return null;
			}
			return _localAddress;
		}

		public void initialize() {
			if (!isEnabled()) {
				return;
			}
			
			_executorService = PortalExecutorManagerUtil.getPortalExecutor(
					CLUSTER_EXECUTOR_CALLBACK_THREAD_POOL);

			PortalUtil.addPortalPortEventListener(this);
				
			try {
				initLocalClusterNode();
			}
			catch (SystemException se) {
				_log.error("Unable to determine local network address", se);
			}
			
			Config cfg = new Config();
			
			_hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
			
			_localAddress = new AddressHazelcastImpl(_hazelcastInstance.getCluster().getLocalMember());
		}

		public boolean isClusterNodeAlive(Address address) {
			if (!isEnabled()) {
				return false;
			}

			List<Address> addresses = getAddresses();
			
			return addresses.contains(address);
		}
		
		protected List<Address> getAddresses() {
			
			Set<Member> members = _hazelcastInstance.getCluster().getMembers();

			if (members == null) {
				return Collections.emptyList();
			}

			List<Address> addresses = new ArrayList<Address>(
					members.size());

			for (Member member : members) {
				addresses.add(new AddressHazelcastImpl(member));
			}

			return addresses;
		}

		public boolean isClusterNodeAlive(String clusterNodeId) {
			if (!isEnabled()) {
				return false;
			}

			return _clusterNodeAddresses.containsKey(clusterNodeId);
		}

		@Override
		public boolean isEnabled() {
			return PropsValues.CLUSTER_LINK_ENABLED;
		}

		public void portalPortConfigured(int port) {
			if (!isEnabled() ||
				_localClusterNode.getPort() ==
					PropsValues.PORTAL_INSTANCE_HTTP_PORT) {

				return;
			}

			try {
				
				//TODO
				_localClusterNode.setPort(port);


			}
			catch (Exception e) {
				_log.error("Unable to determine configure node port", e);
			}
		}

		public void removeClusterEventListener(
			ClusterEventListener clusterEventListener) {

			if (!isEnabled()) {
				return;
			}

			_clusterEventListeners.remove(clusterEventListener);
		}

		public void setClusterEventListeners(
			List<ClusterEventListener> clusterEventListeners) {

			if (!isEnabled()) {
				return;
			}

			_clusterEventListeners.addAllAbsent(clusterEventListeners);
		}

		public void setShortcutLocalMethod(boolean shortcutLocalMethod) {
			if (!isEnabled()) {
				return;
			}

			_shortcutLocalMethod = shortcutLocalMethod;
		}

		protected void initLocalClusterNode() throws SystemException {
			Member member = _hazelcastInstance.getCluster().getLocalMember();
			_localClusterNode = new ClusterNode(member.getUuid());

			try {
			
				_localClusterNode.setInetAddress(member.getInetSocketAddress().getAddress());

				_localClusterNode.setHostName(member.getInetSocketAddress().getHostName());
			}
			catch (Exception e) {
				throw new SystemException(
					"Unable to determine local network address", e);
			}
		}

		private static final String _DEFAULT_CLUSTER_NAME =
			"LIFERAY-CONTROL-CHANNEL";

		private static Log _log = LogFactoryUtil.getLog(ClusterExecutorImpl.class);

		private CopyOnWriteArrayList<ClusterEventListener> _clusterEventListeners =
			new CopyOnWriteArrayList<ClusterEventListener>();
		private Map<String, Address> _clusterNodeAddresses =
			new ConcurrentHashMap<String, Address>();
		private ExecutorService _executorService;
		private Map<String, FutureClusterResponses> _futureClusterResponses =
			new WeakValueConcurrentHashMap<String, FutureClusterResponses>();
		
		private Address _localAddress;
		private ClusterNode _localClusterNode;
		private boolean _shortcutLocalMethod;

		private  HazelcastInstance _hazelcastInstance;
		
		private class ClusterResponseCallbackJob implements Runnable {

			public ClusterResponseCallbackJob(
				ClusterResponseCallback clusterResponseCallback,
				FutureClusterResponses futureClusterResponses) {

				_clusterResponseCallback = clusterResponseCallback;
				_futureClusterResponses = futureClusterResponses;
				_timeout = -1;
				_timeoutGet = false;
				_timeUnit = TimeUnit.SECONDS;
			}

			public ClusterResponseCallbackJob(
				ClusterResponseCallback clusterResponseCallback,
				FutureClusterResponses futureClusterResponses, long timeout,
				TimeUnit timeUnit) {

				_clusterResponseCallback = clusterResponseCallback;
				_futureClusterResponses = futureClusterResponses;
				_timeout = timeout;
				_timeoutGet = true;
				_timeUnit = timeUnit;
			}

			public void run() {
				BlockingQueue<ClusterNodeResponse> blockingQueue =
					_futureClusterResponses.getPartialResults();

				_clusterResponseCallback.callback(blockingQueue);

				ClusterNodeResponses clusterNodeResponses = null;

				try {
					if (_timeoutGet) {
						clusterNodeResponses = _futureClusterResponses.get(
							_timeout, _timeUnit);
					}
					else {
						clusterNodeResponses = _futureClusterResponses.get();
					}

					_clusterResponseCallback.callback(clusterNodeResponses);
				}
				catch (InterruptedException ie) {
					_clusterResponseCallback.processInterruptedException(ie);
				}
				catch (TimeoutException te) {
					_clusterResponseCallback.processTimeoutException(te);
				}
			}

			private final ClusterResponseCallback _clusterResponseCallback;
			private final FutureClusterResponses _futureClusterResponses;
			
			private final long _timeout;
			private final boolean _timeoutGet;
			private final TimeUnit _timeUnit;
		}

		
		
		
		
		

}
