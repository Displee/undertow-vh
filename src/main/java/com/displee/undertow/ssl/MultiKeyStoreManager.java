package com.displee.undertow.ssl;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nick Hartskeerl
 */
public class MultiKeyStoreManager extends SunX509KeyManagerImpl {

	private final Map<String, X509KeyManager> keyManagers = new HashMap<>();

	public void add(String alias, X509KeyManager keyManager) {
		getKeyManagers().put(alias, keyManager);
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		
		if (keyType == null) {
            return null;
        }
		
		for(X509KeyManager keyManager : getKeyManagers().values()) {
			
			if(keyManager == null) {
				continue;
			}
			
			String alias = keyManager.chooseClientAlias(keyType, issuers, socket);
			
			if(alias != null) {
				System.out.println("Choose client alias: "+alias+" - "+keyType+" - "+Arrays.toString(issuers)+" - "+socket);
				return alias;
			}
			
		}
		
		return null;
		
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return null;
	}
	
	@Override
	public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
		
		ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
		String hostname = null;
		for (SNIServerName name : session.getRequestedServerNames()) {
			if (name.getType() == StandardConstants.SNI_HOST_NAME) {
				hostname = ((SNIHostName) name).getAsciiName();
				break;
			}
		}
		
		if (hostname != null && (getCertificateChain(hostname) != null && getPrivateKey(hostname) != null)) {
			return hostname;
		}
		
		return null;
		
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		
		X509KeyManager keyManager = getKeyManagers().get(alias);
		
		if(keyManager != null) {
			
			X509Certificate[] chain = keyManager.getCertificateChain(alias);
			
			if(chain != null) {
				return chain;
			}
			
		}
		
		return null;
		
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		
		X509KeyManager keyManager = getKeyManagers().get(alias);
		
		if(keyManager != null) {
			PrivateKey key = keyManager.getPrivateKey(alias);
			return key;
		}
		
		return null;
		
	}
	
	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
        
		if (keyType == null) {
            return null;
        }
		
		for(X509KeyManager keyManager : getKeyManagers().values()) {
			
			if(keyManager == null) {
				continue;
			}
			
			String[] aliases = keyManager.getServerAliases(keyType, issuers);
			
			if(aliases != null) {
				return aliases;
			}
			
		}
		
		return null;
		
    }

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		
		if (keyType == null) {
            return null;
        }
		
		for(X509KeyManager keyManager : getKeyManagers().values()) {
			
			if(keyManager == null) {
				continue;
			}
			
			String[] aliases = keyManager.getServerAliases(keyType, issuers);
			
			if(aliases != null) {
				return aliases;
			}
			
		}
		
		return null;
		
	}

	public Map<String, X509KeyManager> getKeyManagers() {
		return keyManagers;
	}

}