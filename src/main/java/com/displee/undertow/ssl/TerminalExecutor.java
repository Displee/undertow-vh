package com.displee.undertow.ssl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Nick Hartskeerl
 */
public class TerminalExecutor {

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
	
}
