package io.entgra.device.mgt.core.notification.mgt.common.service;

import io.entgra.device.mgt.core.notification.mgt.common.exception.NotificationConfigurationServiceException;
import io.entgra.device.mgt.core.notification.mgt.common.beans.NotificationConfig;
import io.entgra.device.mgt.core.notification.mgt.common.beans.NotificationConfigurationList;
import java.util.List;
public interface NotificationConfigService {
    /**
     * Retrieve the  notification configurations for a tenant.
     *
     * @return {@link List < Notification >}
     * @throws NotificationConfigurationServiceException Throws when error occurred while retrieving notifications.
     */
        void addNotificationConfigContext(NotificationConfigurationList configurations) throws NotificationConfigurationServiceException;


        void deleteNotificationConfigContext(String configID) throws NotificationConfigurationServiceException;

        void updateNotificationConfigContext(NotificationConfig updatedConfig) throws NotificationConfigurationServiceException;

        void deleteNotificationConfigurations() throws NotificationConfigurationServiceException;

        NotificationConfigurationList getNotificationConfigurations() throws NotificationConfigurationServiceException;

        NotificationConfig getNotificationConfigByID(String configID) throws NotificationConfigurationServiceException;


}
