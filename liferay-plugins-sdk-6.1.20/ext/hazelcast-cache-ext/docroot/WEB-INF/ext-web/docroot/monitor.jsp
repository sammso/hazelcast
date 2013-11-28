 
 <%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%
 java.util.Collection<com.hazelcast.core.Instance> colInstances = com.hazelcast.core.Hazelcast.getInstances();
	for(com.hazelcast.core.Instance instance : colInstances) {
       if (instance.getInstanceType()== com.hazelcast.core.Instance.InstanceType.MAP) {
     	  com.hazelcast.core.IMap imap = (com.hazelcast.core.IMap) instance;
     	  com.hazelcast.monitor.LocalMapStats localMapStats = imap.getLocalMapStats();
     	  long creationTime = localMapStats.getCreationTime();
           long ownedEntryCount = localMapStats.getOwnedEntryCount();
           long backupEntryCount = localMapStats.getBackupEntryCount();
           long hits = localMapStats.getHits();
           long lastUpdateTime = localMapStats.getLastUpdateTime();
           //... and more
           com.hazelcast.monitor.LocalMapOperationStats operationStats =
           localMapStats.getOperationStats();
           long periodStart = operationStats.getPeriodStart();
           long periodEnd = operationStats.getPeriodEnd();
           long numberOfGets = operationStats.getNumberOfGets();
           long numberOfPuts = operationStats.getNumberOfPuts();
           long numberOfEvents = operationStats.getNumberOfEvents();
     	  DateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
     	  
     	 
     	 %>
     	<%=instance.getId() %>
     	<table>
     	<tr>
     	<td>
     	Creation time: 
     	</td>
     	<td>
     	 <%=format.format(new Date(creationTime)) %>
     	 </td>
     	 </tr>
     	 <tr>
     	<td>
     	Owned Entry Count 
     	</td>
     	<td>
     	 <%=ownedEntryCount %>
     	 </td>
     	 </tr>
     	 <tr>
     	<td>
     	Backup Entry Count: 
     	</td>
     	<td>
     	 <%=backupEntryCount %>
     	 </td>
     	 </tr>
     	 <tr>
     	<td>
     	Hits: 
     	</td>
     	<td>
     	 <%=hits %>
     	 </td>
     	 </tr>
     	 <tr>
     	<td>
     	Last Update Time: 
     	</td>
     	<td>
     	 <%=format.format(new Date(lastUpdateTime)) %>
     	 </td>
     	 </tr>
     	 
     	  <tr>
     	<td>
     	Period Start: 
     	</td>
     	<td>
     	 <%=format.format(new Date(periodStart)) %>
     	 </td>
     	 </tr>
     	 <tr>
     	<td>
     	Period End: 
     	</td>
     	<td>
     	 <%=format.format(new Date(periodEnd)) %>
     	 </td>
     	 </tr>
     	 	 <tr>
     	<td>
     	Number of gets: 
     	</td>
     	<td>
     	 <%=numberOfGets %>
     	 </td>
     	 </tr>
     	 	 <tr>
     	<td>
     	Number of puts: 
     	</td>
     	<td>
     	 <%=numberOfPuts %>
     	 </td>
     	 </tr>
     	 
     	  <tr>
     	<td>
     	Number of events: 
     	</td>
     	<td>
     	 <%=numberOfEvents %>
     	 </td>
     	 </tr>
     	 
     	 </table>
     	 <%  
       }
   }
	%>
         