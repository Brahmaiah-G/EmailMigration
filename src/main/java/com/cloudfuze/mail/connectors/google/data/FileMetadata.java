package com.cloudfuze.mail.connectors.google.data;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileMetadata {

    private String kind;
    private String id;
    private String etag;
    private String selfLink;
    private String webContentLink;
    private String alternateLink;
    private String embedLink;
    private String iconLink;
    private String thumbnailLink;
    private String title;
    private String mimeType;
    private Label labels;
    private String createdDate;
    private String modifiedDate;
    private String modifiedByMeDate;
    private String lastViewedByMeDate;
    private String markedViewedByMeDate;
    private String version;
    private String downloadUrl;
    private String originalFilename;
    private String fileExtension;
    private String md5Checksum;
    private long fileSize;
    private String quotaBytesUsed;
    private List<String> ownerNames = null;
    private String lastModifyingUserName;
   
    private Boolean editable;
    private Boolean copyable;
    private Boolean writersCanShare;
    private Boolean shared;
    private Boolean explicitlyTrashed;
    private Boolean appDataContents;
    private String headRevisionId;
    private List<String> spaces = null;

    private Map<String, String> exportLinks = new HashMap<String, String>();

	
}

