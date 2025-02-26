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

package io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao;

import io.entgra.device.mgt.core.device.mgt.common.DeviceManagementConstants;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.IllegalTransactionStateException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.TransactionManagementException;
import io.entgra.device.mgt.core.device.mgt.common.exceptions.UnsupportedDatabaseEngineException;
import io.entgra.device.mgt.core.device.mgt.core.config.datasource.DataSourceConfig;
import io.entgra.device.mgt.core.device.mgt.core.config.datasource.JNDILookupDefinition;
import io.entgra.device.mgt.core.device.mgt.core.dao.util.DeviceManagementDAOUtil;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.impl.GenericMetadataDAOImpl;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.impl.OracleMetadataDAOImpl;
import io.entgra.device.mgt.core.device.mgt.core.metadata.mgt.dao.impl.SQLServerMetadataDAOImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;

/**
 * DAO factory class to be used in MetadataManagement related functionalities.
 */
public class MetadataManagementDAOFactory {

    private static DataSource dataSource;
    private static String databaseEngine;
    private static final Log log = LogFactory.getLog(MetadataManagementDAOFactory.class);
    private static ThreadLocal<Connection> currentConnection = new ThreadLocal<>();

    public static MetadataDAO getMetadataDAO() {
        if (databaseEngine != null) {
            switch (databaseEngine) {
                case DeviceManagementConstants.DataBaseTypes.DB_TYPE_ORACLE:
                    return new OracleMetadataDAOImpl();
                case DeviceManagementConstants.DataBaseTypes.DB_TYPE_MSSQL:
                    return new SQLServerMetadataDAOImpl();
                case DeviceManagementConstants.DataBaseTypes.DB_TYPE_POSTGRESQL:
                case DeviceManagementConstants.DataBaseTypes.DB_TYPE_H2:
                case DeviceManagementConstants.DataBaseTypes.DB_TYPE_MYSQL:
                    return new GenericMetadataDAOImpl();
                default:
                    throw new UnsupportedDatabaseEngineException("Unsupported database engine : " + databaseEngine);
            }
        }
        throw new IllegalStateException("Database engine has not initialized properly.");
    }

    public static void init(DataSourceConfig config) {
        dataSource = resolveDataSource(config);
        try {
            databaseEngine = dataSource.getConnection().getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            log.error("Error occurred while retrieving config.datasource connection", e);
        }
    }

    public static void init(DataSource dtSource) {
        dataSource = dtSource;
        try {
            databaseEngine = dataSource.getConnection().getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            log.error("Error occurred while retrieving config.datasource connection", e);
        }
    }


    public static void beginTransaction() throws TransactionManagementException {
        Connection conn = currentConnection.get();
        if (conn != null) {
            throw new IllegalTransactionStateException("A transaction is already active within the context of " +
                    "this particular thread. Therefore, calling 'beginTransaction/openConnection' while another " +
                    "transaction is already active is a sign of improper transaction handling");
        }
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            currentConnection.set(conn);
        } catch (SQLException e) {
            throw new TransactionManagementException("Error occurred while retrieving config.datasource connection", e);
        }
    }

    public static void openConnection() throws SQLException {
        Connection conn = currentConnection.get();
        if (conn != null) {
            throw new IllegalTransactionStateException("A transaction is already active within the context of " +
                    "this particular thread. Therefore, calling 'beginTransaction/openConnection' while another " +
                    "transaction is already active is a sign of improper transaction handling");
        }
        conn = dataSource.getConnection();
        currentConnection.set(conn);
    }

    public static Connection getConnection() throws IllegalTransactionStateException {
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw new IllegalTransactionStateException("No connection is associated with the current transaction. " +
                    "This might have ideally been caused by not properly initiating the transaction via " +
                    "'beginTransaction'/'openConnection' methods");
        }
        return conn;
    }

    public static void commitTransaction() {
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw new IllegalTransactionStateException("No connection is associated with the current transaction. " +
                    "This might have ideally been caused by not properly initiating the transaction via " +
                    "'beginTransaction'/'openConnection' methods");
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            log.error("Error occurred while committing the transaction", e);
        }
    }

    public static void rollbackTransaction() {
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw new IllegalTransactionStateException("No connection is associated with the current transaction. " +
                    "This might have ideally been caused by not properly initiating the transaction via " +
                    "'beginTransaction'/'openConnection' methods");
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.warn("Error occurred while roll-backing the transaction", e);
        }
    }

    public static void closeConnection() {
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw new IllegalTransactionStateException("No connection is associated with the current transaction. " +
                    "This might have ideally been caused by not properly initiating the transaction via " +
                    "'beginTransaction'/'openConnection' methods");
        }
        try {
            conn.close();
        } catch (SQLException e) {
            log.warn("Error occurred while close the connection");
        }
        currentConnection.remove();
    }


    /**
     * Resolve data source from the data source definition.
     *
     * @param config data source configuration
     * @return data source resolved from the data source definition
     */
    private static DataSource resolveDataSource(DataSourceConfig config) {
        DataSource dataSource = null;
        if (config == null) {
            throw new RuntimeException(
                    "Device Management Repository data source configuration " + "is null and " +
                            "thus, is not initialized");
        }
        JNDILookupDefinition jndiConfig = config.getJndiLookupDefinition();
        if (jndiConfig != null) {
            if (log.isDebugEnabled()) {
                log.debug("Initializing Device Management Repository data source using the JNDI " +
                        "Lookup Definition");
            }
            List<JNDILookupDefinition.JNDIProperty> jndiPropertyList =
                    jndiConfig.getJndiProperties();
            if (jndiPropertyList != null) {
                Hashtable<Object, Object> jndiProperties = new Hashtable<>();
                for (JNDILookupDefinition.JNDIProperty prop : jndiPropertyList) {
                    jndiProperties.put(prop.getName(), prop.getValue());
                }
                dataSource = DeviceManagementDAOUtil.lookupDataSource(jndiConfig.getJndiName(), jndiProperties);
            } else {
                dataSource = DeviceManagementDAOUtil.lookupDataSource(jndiConfig.getJndiName(), null);
            }
        }
        return dataSource;
    }

}
