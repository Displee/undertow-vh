package com.displee.undertow.ssl;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

/**
 * @author Nick Hartskeerl
 */
public class InternalKeyStore {

	private KeyStore keyStore;

	private String password;

	public InternalKeyStore(String password) {
		this.password = password;
	}

	public void load() {
		
		try {
			
			setKeyStore(KeyStore.getInstance("JKS"));
			getKeyStore().load(null, getPassword().toCharArray());
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public void add(ExternalKeyStore keyStore, String alias) {
		
		try {
			
			Key key = keyStore.getKeyStore().getKey(alias, keyStore.getPassword().toCharArray());
			Certificate[] certificates = keyStore.getKeyStore().getCertificateChain(alias);
			
			if(key == null) {
				throw new IOException("No key found for alias '"+alias+"'");
			}
			if(certificates == null || certificates.length == 0) {
				throw new IOException("No certificate found for alias '"+alias+"'");
			}
			
			getKeyStore().setKeyEntry(alias, key, keyStore.getPassword().toCharArray(), certificates);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}