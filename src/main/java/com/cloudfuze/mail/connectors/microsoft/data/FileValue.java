//package com.cloudfuze.mail.connectors.microsoft.data;
//
//import java.util.HashMap;
//import java.util.Map;
//import com.fasterxml.jackson.annotation.JsonAnyGetter;
//import com.fasterxml.jackson.annotation.JsonAnySetter;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonPropertyOrder;
//
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@JsonPropertyOrder({
//    "createdBy",
//    "createdDateTime",
//    "cTag",
//    "eTag",
//    "id",
//    "lastModifiedBy",
//    "lastModifiedDateTime",
//    "name",
//    "parentReference",
//    "size",
//    "webUrl",
//    "fileSystemInfo",
//    "folder",
//    "@microsoft.graph.downloadUrl",
//    "file"
//})
//public class FileValue {
//
//    @JsonProperty("createdBy")
//    private CreatedBy createdBy;
//    @JsonProperty("createdDateTime")
//    private String createdDateTime;
//    @JsonProperty("cTag")
//    private String cTag;
//    @JsonProperty("eTag")
//    private String eTag;
//    @JsonProperty("id")
//    private String id;
//    @JsonProperty("lastModifiedBy")
//    private LastModifiedBy lastModifiedBy;
//    @JsonProperty("lastModifiedDateTime")
//    private String lastModifiedDateTime;
//    @JsonProperty("name")
//    private String name;
//    @JsonProperty("parentReference")
//    private ParentReference parentReference;
//    @JsonProperty("size")
//    private long size;
//    @JsonProperty("webUrl")
//    private String webUrl;
//    @JsonProperty("fileSystemInfo")
//    private FileSystemInfo fileSystemInfo;
//    @JsonProperty("folder")
//    private Folder folder;
//    @JsonProperty("@microsoft.graph.downloadUrl")
//    private String microsoftGraphDownloadUrl;
//    @JsonProperty("file")
//    private File file;
//    
//    @JsonProperty("deleted")
//    private Deleted deleted;
//    @JsonIgnore
//    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
//
//    @JsonProperty("createdBy")
//    public CreatedBy getCreatedBy() {
//        return createdBy;
//    }
//
//    @JsonProperty("createdBy")
//    public void setCreatedBy(CreatedBy createdBy) {
//        this.createdBy = createdBy;
//    }
//
//    @JsonProperty("createdDateTime")
//    public String getCreatedDateTime() {
//        return createdDateTime;
//    }
//
//    @JsonProperty("createdDateTime")
//    public void setCreatedDateTime(String createdDateTime) {
//        this.createdDateTime = createdDateTime;
//    }
//
//    @JsonProperty("cTag")
//    public String getCTag() {
//        return cTag;
//    }
//
//    @JsonProperty("cTag")
//    public void setCTag(String cTag) {
//        this.cTag = cTag;
//    }
//
//    @JsonProperty("eTag")
//    public String getETag() {
//        return eTag;
//    }
//
//    @JsonProperty("eTag")
//    public void setETag(String eTag) {
//        this.eTag = eTag;
//    }
//
//    @JsonProperty("id")
//    public String getId() {
//        return id;
//    }
//
//    @JsonProperty("id")
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    @JsonProperty("lastModifiedBy")
//    public LastModifiedBy getLastModifiedBy() {
//        return lastModifiedBy;
//    }
//
//    @JsonProperty("lastModifiedBy")
//    public void setLastModifiedBy(LastModifiedBy lastModifiedBy) {
//        this.lastModifiedBy = lastModifiedBy;
//    }
//
//    @JsonProperty("lastModifiedDateTime")
//    public String getLastModifiedDateTime() {
//        return lastModifiedDateTime;
//    }
//
//    @JsonProperty("lastModifiedDateTime")
//    public void setLastModifiedDateTime(String lastModifiedDateTime) {
//        this.lastModifiedDateTime = lastModifiedDateTime;
//    }
//
//    @JsonProperty("name")
//    public String getName() {
//        return name;
//    }
//
//    @JsonProperty("name")
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @JsonProperty("parentReference")
//    public ParentReference getParentReference() {
//        return parentReference;
//    }
//
//    @JsonProperty("parentReference")
//    public void setParentReference(ParentReference parentReference) {
//        this.parentReference = parentReference;
//    }
//
//    @JsonProperty("size")
//    public long getSize() {
//        return size;
//    }
//
//    @JsonProperty("size")
//    public void setSize(long size) {
//        this.size = size;
//    }
//
//    @JsonProperty("webUrl")
//    public String getWebUrl() {
//        return webUrl;
//    }
//
//    @JsonProperty("webUrl")
//    public void setWebUrl(String webUrl) {
//        this.webUrl = webUrl;
//    }
//
//    @JsonProperty("fileSystemInfo")
//    public FileSystemInfo getFileSystemInfo() {
//        return fileSystemInfo;
//    }
//
//    @JsonProperty("fileSystemInfo")
//    public void setFileSystemInfo(FileSystemInfo fileSystemInfo) {
//        this.fileSystemInfo = fileSystemInfo;
//    }
//
//    @JsonProperty("folder")
//    public Folder getFolder() {
//        return folder;
//    }
//
//    @JsonProperty("folder")
//    public void setFolder(Folder folder) {
//        this.folder = folder;
//    }
//
//    @JsonProperty("@microsoft.graph.downloadUrl")
//    public String getMicrosoftGraphDownloadUrl() {
//        return microsoftGraphDownloadUrl;
//    }
//
//    @JsonProperty("@microsoft.graph.downloadUrl")
//    public void setMicrosoftGraphDownloadUrl(String microsoftGraphDownloadUrl) {
//        this.microsoftGraphDownloadUrl = microsoftGraphDownloadUrl;
//    }
//
//    @JsonProperty("file")
//    public File getFile() {
//        return file;
//    }
//
//    @JsonProperty("file")
//    public void setFile(File file) {
//        this.file = file;
//    }
//
//    @JsonAnyGetter
//    public Map<String, Object> getAdditionalProperties() {
//        return this.additionalProperties;
//    }
//
//    @JsonAnySetter
//    public void setAdditionalProperty(String name, Object value) {
//        this.additionalProperties.put(name, value);
//    }
//    @JsonProperty("deleted")
//    public Deleted getDeleted() {
//    return deleted;
//    }
//
//    @JsonProperty("deleted")
//    public void setDeleted(Deleted deleted) {
//    this.deleted = deleted;
//    }
//}
