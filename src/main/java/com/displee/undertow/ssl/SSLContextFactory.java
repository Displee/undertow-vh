package com.displee.undertow.ssl;

import com.displee.undertow.host.VirtualHost;
import com.displee.undertow.host.VirtualHostManager;
import com.displee.undertow.util.ConstantsKt;
import org.apache.commons.io.FileUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nick Hartskeerl
 */
public class SSLContextFactory {

	public static final Logger LOGGER = Logger.getLogger(SSLContextFactory.class.getName());

	public static SSLContext create(VirtualHostManager manager) {
		
		try {

			SSLContext sslContext = SSLContext.getInstance("TLS");
			String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
			Iterator<VirtualHost> iterator = manager.getHosts().values().iterator();
			MultiKeyStoreManager multiKeyStoreManager = new MultiKeyStoreManager();
			
			for(; iterator.hasNext();) {
				
				VirtualHost virtualHost = iterator.next();
				
				for(String host : virtualHost.getHosts()) {
				
					KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
					ExternalKeyStore externalKeyStore = generateKeyStore(host, virtualHost);
					
					if(externalKeyStore == null) {
						continue;
					}
					
					keyFactory.init(externalKeyStore.getKeyStore(), externalKeyStore.getPassword().toCharArray());
					
					X509KeyManager keyManager = getX509KeyManager(defaultAlgorithm, keyFactory);
					
					multiKeyStoreManager.add(host, keyManager);
				
				}
				
			}
			
			sslContext.init(new KeyManager[] { multiKeyStoreManager }, null, null);
			SSLContext.setDefault(sslContext);
			
			Set<String> aliases = multiKeyStoreManager.getKeyManagers().keySet();
			
			for(Iterator<String> it = aliases.iterator(); it.hasNext();) {
				LOGGER.log(Level.INFO, "A SSL context has been created for the virtual host '"+it.next()+"'.");
			}
			
			return sslContext;
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
	}

	private static X509KeyManager getX509KeyManager(String algorithm, KeyManagerFactory factory) throws NoSuchAlgorithmException {
		
		KeyManager[] keyManagers = factory.getKeyManagers();

		if(keyManagers == null || keyManagers.length == 0) {
			throw new NoSuchAlgorithmException("The default algorithm :"+algorithm+" produced no key managers");
		}

		X509KeyManager x509KeyManager = null;

		for(int i = 0; i < keyManagers.length; i++) {
			if(keyManagers[i] instanceof X509KeyManager) {
				x509KeyManager = (X509KeyManager) keyManagers[i];
				break;
			}
		}

		if(x509KeyManager == null) {
			throw new NoSuchAlgorithmException("The default algorithm :"+algorithm+" did not produce a X509 Key manager");
		}
		
		return x509KeyManager;
		
	}

	public static ExternalKeyStore generateKeyStore(String host, VirtualHost virtualHost) {
		return generateKeyStore(host, virtualHost, true);
	}

	public static ExternalKeyStore generateKeyStore(String host, VirtualHost virtualHost, boolean remove) {

		Path sslConfig = virtualHost.sslConfig();
		File keyStore = sslConfig.resolve("certificate.jks").toFile();
		
		if(keyStore.exists()) {
			
			File file = sslConfig.resolve("password.key").toFile();
			
			if(file != null) {
				
				try {
					
					String password = FileUtils.readFileToString(file, ConstantsKt.getCHARSET());
					ExternalKeyStore externalKeyStore = new ExternalKeyStore(keyStore.toPath(), password);
					
					externalKeyStore.load();
					
					return externalKeyStore;
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		
		File privateKey = sslConfig.resolve("privkey.pem").toFile();
		File certificate = sslConfig.resolve("cert.pem").toFile();
		File intermediate = sslConfig.resolve("chain.pem").toFile();
		
		if(!privateKey.exists() || !certificate.exists() || !intermediate.exists()) {
			return null;
		}
		
		try {
			
			File output = File.createTempFile("output_cert", ".pfx");
			File workDirectory = sslConfig.toFile();
			String password = "test123";
			
			keyStore = File.createTempFile("certificate", ".jks");
			
			if(keyStore.exists()) {
				keyStore.delete();
			}
			
			String[] string = { "openssl", "pkcs12", "-export", "-out", output.getAbsolutePath(), "-inkey", privateKey.getAbsolutePath(), "-in", certificate.getAbsolutePath(), "-certfile", intermediate.getAbsolutePath(), "-password", "pass:"+password, "-name", host };
			TerminalExecutor.process(string, Runtime.getRuntime(), workDirectory);
			
			string = new String[]{ TerminalExecutor.findJDKPath() + "bin" + File.separator + "keytool", "-alias", host, "-v", "-importkeystore", "-srckeystore", output.getAbsolutePath(), "-srcstoretype", "PKCS12", "-destkeystore", keyStore.getAbsolutePath(), "-deststoretype", "JKS", "-storepass", password, "-keypass", password, "-srcstorepass", password };
			TerminalExecutor.process(string, Runtime.getRuntime(), workDirectory);
			
			ExternalKeyStore externalKeyStore = new ExternalKeyStore(keyStore.toPath(), password);
			
			externalKeyStore.load();
			
			if(remove) {
				
				if(keyStore.exists()) {
					keyStore.delete();
				}
				
				if(output.exists()) {
					output.delete();
				}
				
			}

			return externalKeyStore;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
}
