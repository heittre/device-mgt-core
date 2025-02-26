/*
 * Copyright (c) 2018 - 2023, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
 *
 * Entgra (Pvt) Ltd. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.entgra.device.mgt.core.device.mgt.core.metadata.mgt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.MetadataManagementException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.TransactionManagementException;
import io.entgra.device.mgt.core.device.mgt.common.metadata.mgt.Metadata;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.MetadataDAO;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.MetadataManagementDAOException;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.MetadataManagementDAOFactory;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.util.MetadataConstants;
import io.entgra.device.mgt.core.device.mgt.core.dto.notification.mgt.NotificationConfigDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;


public class NotificationConfigServiceImpl {
    private static final Log log = LogFactory.getLog(NotificationConfigServiceImpl.class);

    private final MetadataDAO metadataDAO;

    public NotificationConfigServiceImpl(MetadataDAO metadataDAO) {
        this.metadataDAO = MetadataManagementDAOFactory.getMetadataDAO();
    }

    public void addNotificationConfigContext(int tenantId, List<NotificationConfigDTO> configurations) throws MetadataManagementException {
        try {
            if (!metadataDAO.isExist(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY)) {
                Metadata configMetadata = constructNotificationConfigContext(configurations);
                metadataDAO.addMetadata(tenantId, configMetadata);
            }else {
                String message = "Notification configurations already exist for tenant: " + tenantId;
                log.error(message);
                throw new IllegalStateException(message);
            }
        } catch (MetadataManagementDAOException e) {
            String message = "Error adding notification configuration context";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        }
    }

    /**
     * Constructs a Metadata object for Notification Configuration using ObjectMapper.
     *
     * @param configurations A list of NotificationConfigDTO objects containing notification configuration details.
     * @return A Metadata object containing the serialized notification configuration.
     */
    public Metadata constructNotificationConfigContext(List<NotificationConfigDTO> configurations) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode notificationConfigObject = objectMapper.createObjectNode();
            ArrayNode configArray = objectMapper.createArrayNode();
            for (NotificationConfigDTO config : configurations) {
                ObjectNode configNode = objectMapper.valueToTree(config);
                configArray.add(configNode);
            }
            notificationConfigObject.set("configurations", configArray);
            Metadata configMetadata = new Metadata();
            configMetadata.setMetaKey(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            configMetadata.setMetaValue(notificationConfigObject.toString());
            return configMetadata;
        } catch (Exception e) {
            throw new RuntimeException("Error constructing Notification Config Context", e);
        }
    }

    /**
     * Deletes a specific notification configuration from the Metadata context for a given tenant.
     *
     * @param tenantId    The tenant ID associated with the notification configurations.
     * @param operationCode The unique identifier (operationCode) of the notification configuration to be deleted.
     * @throws MetadataManagementException If no configuration is found with the specified operationCode, or
     * if any error occurs during the database transaction or processing
     * This method retrieves the existing notification configuration context for the given tenant, removes the
     * configuration matching the provided operationCode, and updates the Metadata context with the remaining configurations.
     */


    public void deleteNotificationConfigContext(int tenantId, String operationCode) throws MetadataManagementDAOException {
        try {
            Metadata existingMetadata = metadataDAO.getMetadata(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (existingMetadata == null) {
                String message = "No notification configuration context found for tenant: " + tenantId;
                throw new NoSuchElementException(message);
            }


            ObjectMapper objectMapper = new ObjectMapper();
            List<NotificationConfigDTO> configurations = objectMapper.readValue(
                    existingMetadata.getMetaValue(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
            );


            boolean isRemoved = configurations.removeIf(config -> config.getOperationCode().equals(operationCode));
            if (!isRemoved) {
                String message = "No configuration found with operationCode: " + operationCode;
                log.error(message);
                throw new MetadataManagementDAOException(message);
            }


            existingMetadata.setMetaValue(objectMapper.writeValueAsString(configurations));
            metadataDAO.updateMetadata(tenantId, existingMetadata);


        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (MetadataManagementDAOException e) {
            String message = "Error accessing database for metadata update";
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error occurred while deleting notification configuration";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }




    /**
     * Updates an existing notification configuration or adds a new configuration to the Metadata context for a given tenant.
     *
     * @param tenantId      The tenant ID associated with the notification configurations.
     * @param updatedConfig The notification configuration to be updated or added.
     *                      If a configuration with the same operationCode exists, it will be updated; otherwise, it will be added as a new entry.
     * @throws MetadataManagementException If any error occurs during the database transaction or processing.
     *
     * This method retrieves the existing notification configuration context for the given tenant. If a configuration with the same
     * operationCode as the provided configuration exists, it updates that configuration with the new details. Otherwise, it appends
     * the provided configuration as a new entry. The updated configurations are then serialized and saved back to the Metadata context.
     */

    public void updateNotificationConfigContext(int tenantId, NotificationConfigDTO updatedConfig) throws MetadataManagementException {
        try {
            Metadata existingMetadata;
            try {
                existingMetadata = metadataDAO.getMetadata(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            } catch (MetadataManagementDAOException e) {
                throw new MetadataManagementException("Error fetching metadata for tenant ID: " + tenantId, e);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            List<NotificationConfigDTO> configurations;

            try {
                if (existingMetadata != null) {
                    configurations = objectMapper.readValue(
                            existingMetadata.getMetaValue(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
                    );

                    boolean isUpdated = false;
                    for (int i = 0; i < configurations.size(); i++) {
                        if (configurations.get(i).getOperationCode().equals(updatedConfig.getOperationCode())) {
                            configurations.set(i, updatedConfig);
                            isUpdated = true;
                            break;
                        }
                    }
                    if (!isUpdated) {
                        configurations.add(updatedConfig);
                    }
                } else {
                    configurations = new ArrayList<>();
                    configurations.add(updatedConfig);
                }
            } catch (IOException e) {
                throw new MetadataManagementException("Error processing JSON while reading or updating configurations", e);
            }

            Metadata updatedMetadata = new Metadata();
            updatedMetadata.setMetaKey(MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            try {
                updatedMetadata.setMetaValue(objectMapper.writeValueAsString(configurations));
            } catch (JsonProcessingException e) {
                throw new MetadataManagementException("Error serializing configurations to JSON", e);
            }

            try {
                if (existingMetadata != null) {
                    metadataDAO.updateMetadata(tenantId, updatedMetadata);
                } else {
                    metadataDAO.addMetadata(tenantId, updatedMetadata);
                }
            } catch (MetadataManagementDAOException e) {
                throw new MetadataManagementException("Error saving metadata for tenant ID: " + tenantId, e);
            }
        } catch (Exception e) {
            MetadataManagementDAOFactory.rollbackTransaction();
            String message = "Unexpected error while processing notification configuration context";
            log.error(message, e);
            throw new MetadataManagementException(message, e);
        }
    }

    public void deleteNotificationConfigurations(int tenantId) throws MetadataManagementDAOException {
        try {
            Metadata existingMetadata = metadataDAO.getMetadata(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (existingMetadata == null) {
                String message = "No notification configuration context found for tenant: " + tenantId;
                throw new NoSuchElementException(message);
            }

            ObjectMapper objectMapper = new ObjectMapper();

            existingMetadata.setMetaValue(objectMapper.writeValueAsString(Collections.emptyList()));
            metadataDAO.updateMetadata(tenantId, existingMetadata);


        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.error("Error clearing configurations");
            throw e;
        } catch (MetadataManagementDAOException e) {
            String message = "Database error while updating metadata for tenant ID: " + tenantId;
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error occurred while deleting notification configurations for tenant ID: " + tenantId;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public List<NotificationConfigDTO> getNotificationConfigurations(int tenantId) throws MetadataManagementDAOException {
        try {
            Metadata existingMetadata = metadataDAO.getMetadata(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY);

            if (existingMetadata == null) {
                log.warn("No notification configurations found for tenant");
                return Collections.emptyList();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(
                    existingMetadata.getMetaValue(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
            );
        } catch (MetadataManagementDAOException e) {
            String message = "Database error occurred while retrieving notification configurations for tenant ID: " + tenantId;
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        } catch (Exception e) {
            String message = "Unexpected error occurred while retrieving notification configurations for tenant ID: " + tenantId;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }




    public NotificationConfigDTO getNotificationConfigById(int tenantId, String id) throws MetadataManagementDAOException {
        try {
            Metadata metadata = metadataDAO.getMetadata(tenantId, MetadataConstants.NOTIFICATION_CONFIG_META_KEY);
            if (metadata == null) {
                log.error("No configurations found for tenant: " + tenantId);
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            List<NotificationConfigDTO> configurations = objectMapper.readValue(
                    metadata.getMetaValue(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NotificationConfigDTO.class)
            );

            for (NotificationConfigDTO config : configurations) {
                if (config.getOperationCode().equals(id)) {
                    return config;
                }
            }

            log.warn("Configuration with ID '" + id + "' not found for tenant: " + tenantId);
            return null;
        } catch (Exception e) {
            String message = "Error retrieving notification configuration by ID.";
            log.error(message, e);
            throw new MetadataManagementDAOException(message, e);
        }
    }


}





