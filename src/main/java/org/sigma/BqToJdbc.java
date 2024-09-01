package org.sigma;

/*
 * Copyright (C) 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */




import autovalue.shaded.com.google.common.annotations.VisibleForTesting;
import com.google.api.services.bigquery.model.TableRow;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;


import org.sigma.commons.*;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write;
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.transforms.MapElements;

import org.apache.beam.sdk.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import static org.sigma.commons.KMSUtils.maybeDecrypt;

/**
 * A template that copies data from a BigQuery table to a relational database using JDBC .
 *

 */
@Template(
        name = "BigQuery_to_Jdbc_Flex",
        category = TemplateCategory.BATCH,
        displayName = "BigQuery to JDBC",
        description = {
                "The BigQuery To JDBC template is a batch pipeline that copies data from a BigQuery  into an existing relational database  table. "
                        + "This pipeline uses JDBC to connect to the relational database. You can use this template to copy data from BigQuery to any relational database with available JDBC drivers",
                "For an extra layer of protection, you can also pass in a Cloud KMS key along with a Base64-encoded username, password, and connection string parameters encrypted with the Cloud KMS key. "
                        + "See the <a href=\"https://cloud.google.com/kms/docs/reference/rest/v1/projects.locations.keyRings.cryptoKeys/encrypt\">Cloud KMS API encryption endpoint</a> for additional details on encrypting your username, password, and connection string parameters."
        },
        optionsClass = BigQueryToJdbcOptions.class,
        flexContainerName = "bigquery-to-jdbc",
        documentation =
                "https://cloud.google.com/dataflow/docs/guides/templates/provided/jdbc-to-bigquery",
        contactInformation = "https://cloud.google.com/support",
        preview = true,
        requirements = {
                "The JDBC drivers for the relational database must be available.",
                "The BigQuery table must exist before pipeline execution.",
                "The BigQuery table must have a compatible schema.",
                "The relational database must be accessible from the subnet where Dataflow runs."
        })
public class BqToJdbc {

    /**
     * Main entry point for executing the pipeline. This will run the pipeline asynchronously. If
     * blocking execution is required, use the {@link BqToJdbc#run} method to start the pipeline
     * and invoke {@code result.waitUntilFinish()} on the {@link PipelineResult}.
     *
     * @param args The command-line arguments to the pipeline.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BqToJdbc.class);

    public static void main(String[] args) {

     
        UncaughtExceptionLogger.register();

        // Parse the user options passed from the command-line
        BigQueryToJdbcOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryToJdbcOptions.class);

        run(options, writeToBQTransform(options));
    }

    /**
     * Create the pipeline with the supplied options.
     *
     * @param options The execution parameters to the pipeline.
     * @param writeToBQ the transform that outputs {@link TableRow}s to BigQuery.
     * @return The result of the pipeline execution.
     */
    @VisibleForTesting
    static PipelineResult run(BigQueryToJdbcOptions options, Write<TableRow> writeToBQ) {
        // Validate BQ STORAGE_WRITE_API options
        BigQueryIOUtils.validateBQStorageApiOptionsBatch(options);

        // Create the pipeline
        Pipeline pipeline = Pipeline.create(options);

        /*
         * Steps: 1) Read records via JDBC and convert to TableRow via RowMapper
         *        2) Append TableRow to BigQuery via BigQueryIO
         */
        JdbcIO.DataSourceConfiguration dataSourceConfiguration =
                JdbcIO.DataSourceConfiguration.create(
                                StaticValueProvider.of(options.getDriverClassName()),
                                maybeDecrypt(options.getConnectionURL(), options.getKMSEncryptionKey()))
                        .withUsername(maybeDecrypt(options.getUsername(), options.getKMSEncryptionKey()))
                        .withPassword(maybeDecrypt(options.getPassword(), options.getKMSEncryptionKey()));

        if (options.getDriverJars() != null) {
            dataSourceConfiguration = dataSourceConfiguration.withDriverJars(options.getDriverJars());
        }

        if (options.getConnectionProperties() != null) {
            dataSourceConfiguration =
                    dataSourceConfiguration.withConnectionProperties(options.getConnectionProperties());

        }

        LOG.info("DataSourceConfiguration: {}", dataSourceConfiguration);



        // Step 1: Leer registros de una tabla en BigQuery
        PCollection<TableRow> bigQueryRows_3 = pipeline.apply("Read from BigQuery",
                BigQueryIO.readTableRowsWithSchema()
                        .fromQuery(options.getQuery())
                        .withCoder(TableRowJsonCoder.of())
                        .withoutValidation().usingStandardSql().withMethod(BigQueryIO.TypedRead.Method.DIRECT_READ));


        Schema schema = bigQueryRows_3.getSchema();

        String inputFields = "";
        String columnFields = "";

        for (int i = 0; i < schema.getFieldCount(); i++) {
            if (i < schema.getFieldCount() - 1) {
                inputFields = inputFields.concat("?,");
            } else {
                inputFields = inputFields.concat("?");
            }
        }
        for (int i = 0; i < schema.getFieldCount(); i++) {
            System.out.println(schema.getField(i).getType());
            if (i < schema.getFieldCount() - 1) {

                columnFields = columnFields.concat(schema.nameOf(i) + ",");
            } else {
                columnFields = columnFields.concat(schema.nameOf(i));
            }
        }

        String tableName = options.getTable();
        String insertquery = "INSERT INTO " + tableName + " (" + columnFields + ") values(" + inputFields + ")";


        bigQueryRows_3
                .apply(MapElements.into(TypeDescriptor.of(TableRow.class))
                        // Use TableRow to access individual fields in the row.
                        .via((TableRow row) -> {
                            return row;
                        }))
                .apply(JdbcIO.<TableRow>write().withDataSourceConfiguration(dataSourceConfiguration)
                        .withStatement(insertquery)
                        .withAutoSharding()
                        .withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<TableRow>() {
                            public void setParameters(TableRow element, PreparedStatement query) throws SQLException {


                                for (int i = 0; i < schema.getFieldCount(); i++) {

                                    String type = schema.getField(i).getType().toString();

                                    switch (type) {
                                        case "INT64":
                                            if (element.get(schema.nameOf(i)) != null) {
                                                query.setInt(i + 1, Integer.parseInt(element.get(schema.nameOf(i)).toString()));
                                            } else {
                                                query.setString(i + 1, null);
                                            }
                                            break;
                                        case "STRING":
                                            query.setString(i + 1, (String) element.get(schema.nameOf(i)));
                                            break;
                                        case "TIMESTAMP":
                                            query.setTimestamp(i + 1, (Timestamp) element.get(schema.nameOf(i)));
                                            break;
                                        case "BOOLEAN":
                                            if (element.get(schema.nameOf(i)) != null) {
                                                query.setBoolean(i + 1,
                                                        Boolean.parseBoolean(element.get(schema.nameOf(i)).toString()));
                                            } else {
                                                query.setString(i + 1, null);
                                            }
                                            break;
                                        default:
                                            query.setString(i + 1, (String) element.get(schema.nameOf(i)));
                                    }

                                }
                            }
                        }));
        return pipeline.run();

    }

    /**
     * Create the {@link Write} transform that outputs the collection to BigQuery as per input option.
     */

    @VisibleForTesting
    static Write<TableRow> writeToBQTransform(BigQueryToJdbcOptions options) {
        // Needed for loading GCS filesystem before Pipeline.Create call
        FileSystems.setDefaultPipelineOptions(options);
        Write<TableRow> write =
                BigQueryIO.writeTableRows()
                        .withoutValidation()
                        .withCreateDisposition(Write.CreateDisposition.valueOf(options.getCreateDisposition()))
                        .withWriteDisposition(
                                options.getIsTruncate()
                                        ? Write.WriteDisposition.WRITE_TRUNCATE
                                        : Write.WriteDisposition.WRITE_APPEND)
                        .withCustomGcsTempLocation(
                                StaticValueProvider.of(options.getBigQueryLoadingTemporaryDirectory()))
                        .to(options.getOutputTable());

        if (Write.CreateDisposition.valueOf(options.getCreateDisposition())
                != Write.CreateDisposition.CREATE_NEVER) {
            write = write.withJsonSchema(GCSUtils.getGcsFileAsString(options.getBigQuerySchemaPath()));
        }

        return write;
    }


}
