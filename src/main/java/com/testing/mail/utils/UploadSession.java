package com.testing.mail.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.apache.commons.lang.exception.ExceptionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class UploadSession {

    // Upload in chunks of 4MB as per MS recommendation
    private static final int CHUNK_SIZE = 4 * 1024 * 1024;
    private final File file;
    //private final InputStream inputStream;
    private final String uploadUrl;
    private final RandomAccessFile raf;
    private String parent;
    private Range[] ranges;
    private long totalUploaded;
    private long lastUploaded;
    private boolean  item;

    public UploadSession(String parent, File file, String uploadUrl, String[] ranges) throws IOException {
        this.parent = parent;
        this.file = file;
        this.uploadUrl = uploadUrl;
        this.raf = new RandomAccessFile(file, "r");
        setRanges(ranges);
    }

    
    public void setRanges(String[] stringRanges) {

    	this.ranges = new Range[stringRanges.length];
    	if(stringRanges[0].indexOf("-")==-1 && !stringRanges[0].equals("0")) {
    		ranges[0] = new Range(Long.parseLong(stringRanges[0]), (Long.parseLong(stringRanges[0])+CHUNK_SIZE));
    		
    	}else if(stringRanges[0].indexOf("-")==-1 && stringRanges[0].equals("0")){
    		ranges[0] = new Range(Long.parseLong(stringRanges[0]), (Long.parseLong(stringRanges[0])+CHUNK_SIZE));
    	}
    	else {
    		for (int i = 0; i < stringRanges.length; i++) {
    			long start = Long.parseLong(stringRanges[i].substring(0, stringRanges[i].indexOf('-')));

    			String s = stringRanges[i].substring(stringRanges[i].indexOf('-') + 1);

    			long end = 0;
    			if (!s.isEmpty()) {
    				end = Long.parseLong(s);
    			}

    			ranges[i] = new Range(end, end);
    		}
    	}
    	if (ranges.length > 0) {
    		lastUploaded = ranges[0].start - totalUploaded;
    		totalUploaded = ranges[0].start;
    	}
    }

    public byte[] getChunk() throws IOException {

    	byte[] bytes = new byte[CHUNK_SIZE];

        raf.seek(totalUploaded);
        int read = raf.read(bytes);

        if (read < CHUNK_SIZE) {
        	if(read != -1){
        		bytes = Arrays.copyOf(bytes, read);
        	}
        }

        return bytes;
    }
    
    public boolean isComplete() {
        return item;
    }

    public void setComplete(boolean item) {
    	this.item = item;
        lastUploaded = file.length() - totalUploaded;
        totalUploaded = file.length();
        closeFile();
    }

   
   
   

    private static class Range {
        public long start;
        public long end;

        private Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
    public void closeFile(){
    	try{
    		this.raf.close();
    	}catch(Exception e){
    		log.error("Exception while closing RAF File:"+ExceptionUtils.getFullStackTrace(e));
    	}
    }
}



