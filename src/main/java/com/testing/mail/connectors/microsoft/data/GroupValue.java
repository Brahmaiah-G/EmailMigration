package com.testing.mail.connectors.microsoft.data;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GroupValue {
	@SerializedName("@odata.context")
	@Expose
	private String odataContext;
	@SerializedName("@odata.id")
	@Expose
	private String odataId;
	@SerializedName("id")
	@Expose
	private String id;
	@SerializedName("deletedDateTime")
	@Expose
	private Object deletedDateTime;
	@SerializedName("classification")
	@Expose
	private Object classification;
	@SerializedName("createdDateTime")
	@Expose
	private String createdDateTime;
	@SerializedName("createdByAppId")
	@Expose
	private String createdByAppId;
	@SerializedName("organizationId")
	@Expose
	private String organizationId;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("displayName")
	@Expose
	private String displayName;
	@SerializedName("expirationDateTime")
	@Expose
	private Object expirationDateTime;
	@SerializedName("groupTypes")
	@Expose
	private List<String> groupTypes;
	@SerializedName("infoCatalogs")
	@Expose
	private List<Object> infoCatalogs;
	@SerializedName("isAssignableToRole")
	@Expose
	private Boolean isAssignableToRole;
	@SerializedName("isManagementRestricted")
	@Expose
	private Object isManagementRestricted;
	@SerializedName("mail")
	@Expose
	private String mail;
	@SerializedName("mailEnabled")
	@Expose
	private Boolean mailEnabled;
	@SerializedName("mailNickname")
	@Expose
	private String mailNickname;
	@SerializedName("membershipRule")
	@Expose
	private Object membershipRule;
	@SerializedName("membershipRuleProcessingState")
	@Expose
	private Object membershipRuleProcessingState;
	@SerializedName("onPremisesDomainName")
	@Expose
	private Object onPremisesDomainName;
	@SerializedName("onPremisesLastSyncDateTime")
	@Expose
	private Object onPremisesLastSyncDateTime;
	@SerializedName("onPremisesNetBiosName")
	@Expose
	private Object onPremisesNetBiosName;
	@SerializedName("onPremisesSamAccountName")
	@Expose
	private Object onPremisesSamAccountName;
	@SerializedName("onPremisesSecurityIdentifier")
	@Expose
	private Object onPremisesSecurityIdentifier;
	@SerializedName("onPremisesSyncEnabled")
	@Expose
	private Object onPremisesSyncEnabled;
	@SerializedName("preferredDataLocation")
	@Expose
	private String preferredDataLocation;
	@SerializedName("preferredLanguage")
	@Expose
	private Object preferredLanguage;
	@SerializedName("proxyAddresses")
	@Expose
	private List<String> proxyAddresses;
	@SerializedName("renewedDateTime")
	@Expose
	private String renewedDateTime;
	@SerializedName("resourceBehaviorOptions")
	@Expose
	private List<Object> resourceBehaviorOptions;
	@SerializedName("resourceProvisioningOptions")
	@Expose
	private List<Object> resourceProvisioningOptions;
	@SerializedName("securityEnabled")
	@Expose
	private Boolean securityEnabled;
	@SerializedName("securityIdentifier")
	@Expose
	private String securityIdentifier;
	@SerializedName("theme")
	@Expose
	private Object theme;
	@SerializedName("visibility")
	@Expose
	private String visibility;
	@SerializedName("writebackConfiguration")
	@Expose
	private WritebackConfiguration writebackConfiguration;
	@SerializedName("onPremisesProvisioningErrors")
	@Expose
	private List<Object> onPremisesProvisioningErrors;
}
