package com.liferay.portal.cluster;

import com.hazelcast.core.Member;
import com.liferay.portal.kernel.cluster.Address;

public class AddressHazelcastImpl implements Address {

	public AddressHazelcastImpl(Member address) {
		_address = address;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		AddressHazelcastImpl addressImpl = null;

		try {
			addressImpl = (AddressHazelcastImpl)obj;
		}
		catch (ClassCastException cce) {
			return false;
		}

		if (_address.equals(addressImpl._address)) {
			return true;
		}
		else {
			return false;
		}
	}

	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return _address.toString();
	}

	@Override
	public Object getRealAddress() {
		// TODO Auto-generated method stub
		return _address.getInetSocketAddress();
	}
	
	@Override
	public int hashCode() {
		return _address.hashCode();
	}

	@Override
	public String toString() {
		return _address.toString();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5154046516647521257L;

	private Member _address;
}
