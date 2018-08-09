package com.sap.rest.utils;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.*;
import org.apache.olingo.server.api.uri.UriParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public class DummyDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummyDataProvider.class);

    private final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private final String protocol = "jdbc:derby:";
    private final String dbName;
    private Connection connection;

    public DummyDataProvider() throws SQLException {
        this.dbName = "derbyDB";

        initializeConnection();
        createCarsTable();
        createCars();
        this.connection.commit();
    }

    private void initializeConnection() throws SQLException {
        LOGGER.info("Create derby connection");
        Properties properties = new Properties();
        properties.setProperty("user", "user1");
        properties.setProperty("password", "user1");

        this.connection = DriverManager.getConnection(protocol + this.dbName + ";create=true", properties);

        try (Statement statement = this.connection.createStatement()) {
            statement.execute("drop table Cars");
        }
    }

    private void createCarsTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("create table Cars(id int, model varchar(40))");
        }
    }

    private void createCars() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into Cars values(?, ?)")) {
            preparedStatement.setInt(1, 1);
            preparedStatement.setString(2, "Maruti");
            preparedStatement.executeUpdate();

            preparedStatement.setInt(1, 2);
            preparedStatement.setString(2, "Hyundai");
            preparedStatement.executeUpdate();
        }
    }

    private Property createPrimitive(String name, Object value) {
        return new Property(null, name, ValueType.PRIMITIVE, value);
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public final EntityCollection readAll(EdmEntitySet edmEntitySet) throws SQLException {
        EntityCollection entityCollection;
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT id, model FROM Cars ORDER BY id")) {
            entityCollection = new EntityCollection();
            while (rs.next()) {
                Entity entity = new Entity().addProperty(createPrimitive("Id", rs.getInt(1))).addProperty(createPrimitive("Model", rs.getString(2)));
                entity.setId(createId("Cars", rs.getInt(1)));
                entityCollection.getEntities().add(entity);
            }
        }
        return entityCollection;
    }

    public Entity read(EdmEntitySet edmEntitySet, List<UriParameter> parameters) throws SQLException {
        EdmEntityType entityType = edmEntitySet.getEntityType();

        EntityCollection entitySet = readAll(edmEntitySet);

        try {
            for (Entity entity : entitySet.getEntities()) {
                boolean found = true;

                for (UriParameter parameter : parameters) {
                    EdmProperty property = (EdmProperty) entityType.getProperty(parameter.getName());
                    EdmPrimitiveType type = (EdmPrimitiveType) property.getType();

                    if (type.valueToString(entity.getProperty(parameter.getName()).getValue(), property.isNullable(), property.getMaxLength(), property.getPrecision(),
                            property.getScale(), property.isUnicode()).equals(parameter.getText())) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return entity;
                }
            }
            return null;
        } catch (EdmPrimitiveTypeException e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    @Override
    public String toString() {
        return "DummyDataProvider{" + "driver=" + driver + ", protocol=" + protocol + ", dbName=" + dbName + ", connection=" + connection + '}';
    }
}
