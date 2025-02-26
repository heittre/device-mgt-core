package io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@ApiModel(value = "NotificationConfig", description = "Notification Configurations")
public class NotificationConfig {

    @ApiModelProperty(name = "id", value = "The ID of the notification configuration.", required = true)
    private String id;

    @ApiModelProperty(name = "name", value = "The name of the notification configuration.", required = true)
    private String name = "Notification Configuration Name";

    @ApiModelProperty(name = "description", value = "The description of the notification configuration.", required = true)
    private String description = "Notifies Description";


    @ApiModelProperty(name = "opcode", value = "The operation or task code associated with the notification.", required = true)
    private String code;

    @ApiModelProperty(name = "priority", value = "The priority of the notification configuration.", required = true)
    private int priority = 1;

    @ApiModelProperty(name = "archiveAfter", value = "The duration after which notifications are archived.", required = true)
    private Duration archiveAfter;

    @ApiModelProperty(name = "recipients", value = "The recipients of the notification.", required = true)
    private NotificationConfigRecipients recipients;

    @ApiModelProperty(name = "configuredBy", value = "Information about the person who configured the notification.", required = true)
    private ConfiguredBy configuredBy;

    @ApiModelProperty(name = "notificationSettings", value = "The notification settings.", required = true)
    private NotificationConfigurationSettings notificationSettings;

    @ApiModelProperty(name = "Critical criteria", value = "The critical criteria for notification configurations", required = false)
    private NotificationConfigCriticalCriteria criticalCriteria;

//    @ApiModelProperty(name = "meta data", value = "Meta data in notification configurations.", required = false)
//    private Metadata metaData;

    // Getters and Setters
    // (Omitted for brevity)

    public static class ConfiguredBy {
        @JsonProperty("user")
        private String user;

        @JsonProperty("lastModifiedAt")
        private LocalDateTime lastModifiedAt;
    }




    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getArchiveAfter() {
        return archiveAfter;
    }

    public void setArchiveAfter(String archiveAfter) {
        this.archiveAfter = archiveAfter;
    }

    public ConfiguredBy getConfiguredBy() {
        return configuredBy;
    }

    public void setConfiguredBy(ConfiguredBy configuredBy) {
        this.configuredBy = configuredBy;
    }


    public enum NotificationType {
        OPERATION_RELATED,
        TASK_RELATED
    }

}
