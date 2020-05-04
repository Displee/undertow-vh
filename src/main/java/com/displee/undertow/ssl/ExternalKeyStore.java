package com.displee.undertow.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * @author Nick Hartskeerl
 */
public class ExternalKeyStore {

	private Path path;

	private String password;

	private KeyStore keyStore;

	public ExternalKeyStore(Path path, String password) {
		this.setPath(path);
		this.password = password;
	}
	

	public void load() {
		
		File file = path.toFile();
		
		if(!file.exists()) {
			throw new RuntimeException("The key store '"+file.toString()+"' does not exist");
		}
		
		try {
			setKeyStore(KeyStore.getInstance("jks"));
			getKeyStore().load(new FileInputStream(file), getPassword().toCharArray());
		} catch(Exception e) {
			throw new RuntimeException("The key store '"+file.toString()+"' could not be loaded", e);
		}
		
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public KeyStore getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(KeyStore keyStore) {
		this.keyStore = keyStore;
	}
	
}
