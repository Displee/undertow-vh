package com.displee.undertow.ssl;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nick Hartskeerl
 */
public class TerminalExecutor {

	private static final Pattern JDK_REGEX = Pattern.compile("jdk[0-9].[0-9].[0-9]_(.*?)" + StringEscapeUtils.escapeJava(File.separator));

    public static void process(String[] args, Runtime runtime, File workDir) throws IOException, InterruptedException {
    	process(args, runtime, workDir, null, null);
    }

    public static void process(String[] args, Runtime runtime, File workDir, OutputStream out, OutputStream err) throws IOException, InterruptedException {
        
    	Process process = runtime.exec(args, null, workDir);

        if(out != null) {
        	new Thread(new StreamReader(process.getInputStream(), out)).start();
        }
        
        if(err != null) {
        	new Thread(new StreamReader(process.getErrorStream(), err)).start();
        }

        int exitCode = process.waitFor();
        
        if(exitCode != 0) {
        	
            StringBuilder builder = new StringBuilder();
            
            for(String arg : args) {
                builder.append(arg).append(' ');
            }
            
            throw new RuntimeException("Process execution failed. Return code: " + exitCode + "\ncommand: " + builder);
            
        }
        
    }
    
    /**
     * @author Nick Hartskeerl
     */
    private static class StreamReader implements Runnable {

	    private final InputStream in;
	    private final OutputStream out;

	    public StreamReader(InputStream in, OutputStream out) {
	        this.in = in;
	        this.out = out;
	    }

	    @Override
	    public void run() {
	    	
	        int next;
	        
	        try {
	        	
	            while((next = in.read()) != -1) {
	                out.write(next);
	            }
	            
	            out.flush();
	            
	        } catch(IOException e) {
	            e.printStackTrace();
	        }
	        
	    }
	}

	public static String findJDKPath() {
    	Enumeration<Object> properties = System.getProperties().elements();
    	List<String> possibilities = new ArrayList<>();
    	String separator = System.getProperty("path.separator");
    	while(properties.hasMoreElements()) {
    		Object element = properties.nextElement();
    		if (!(element instanceof String)) {
    			continue;
			}
    		String string = (String) element;
    		String[] split = string.split(separator);
    		for(String s : split) {
				if (s.contains("jdk") && s.contains(File.separator)) {
					possibilities.add(s);
				}
			}
		}
    	for(String s : possibilities) {
			Matcher matcher = JDK_REGEX.matcher(s);
			if (matcher.find()) {
				String group = matcher.group();
				return s.substring(0, s.indexOf(group) + group.length());
			}
		}
    	return null;
	}
	
}
