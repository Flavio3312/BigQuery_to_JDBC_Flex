# BigQuery to JDBC Flex Template
The BigQuery to JDBC template is a batch pipeline that transfers data from an existing BigQuery table into a relational database table using JDBC. This pipeline uses JDBC to connect to the relational database. You can use this template to transfer data from any BigQuery table to any relational database with available JDBC drivers.

For an extra layer of protection, you can also pass in a Cloud KMS key along with Base64-encoded username, password, and connection string parameters encrypted with the Cloud KMS key. See the Cloud KMS API encryption endpoint for additional details on encrypting your username, password, and connection string parameters.

## Overview
This template allows you to transfer data from a BigQuery table to a JDBC-compatible database using Apache Beam on Google Cloud Dataflow.



### Required Parameters

- **driverJars**: The comma-separated list of driver JAR files.  
  **Example**: `gs://your-bucket/driver_jar1.jar,gs://your-bucket/driver_jar2.jar`

- **driverClassName**: The JDBC driver class name.  
  **Example**: `com.mysql.jdbc.Driver`

- **connectionURL**: The JDBC connection URL string.  
  **Example**: `jdbc:mysql://some-host:3306/sampledb`  
  You can pass in this value as a string that's encrypted with a Cloud KMS key and then Base64-encoded. Remove whitespace characters from the Base64-encoded string.

- **outputTable**: The BigQuery output table location.  
  **Example**: `<PROJECT_ID>:<DATASET_NAME>.<TABLE_NAME>`

- **bigQueryLoadingTemporaryDirectory**: The temporary directory for the BigQuery loading process.  
  **Example**: `gs://your-bucket/your-files/temp_dir`



### Optional Parameters

- **connectionProperties**: The properties string to use for the JDBC connection. The format of the string must be `[propertyName=property;]*`.  
  **Example**: `unicode=true;characterEncoding=UTF-8`  
  [More information on MySQL connection properties](https://dev.mysql.com/doc/connector-j/8.1/en/connector-j-reference-configuration-properties.html).

- **username**: The username to use for the JDBC connection. You can pass in this value as a string that's encrypted with a Cloud KMS key and then Base64-encoded.

- **password**: The password to use for the JDBC connection. You can pass in this value as a string that's encrypted with a Cloud KMS key and then Base64-encoded.

- **query**: The query to run on the source to extract the data.  
  **Note**: Some JDBC SQL and BigQuery types, although sharing the same name, have some differences. Type casting may be required if your schemas do not match.  
  **Example**: `SELECT * FROM sampledb.sample_table`

- **KMSEncryptionKey**: The Cloud KMS encryption key to use to decrypt the username, password, and connection string.  
  **Example**: `projects/your-project/locations/global/keyRings/your-keyring/cryptoKeys/your-key`

- **useColumnAlias**: If set to true, the pipeline uses the column alias (AS) instead of the column name to map the rows to BigQuery. Defaults to `false`.

- **isTruncate**: If set to true, the pipeline truncates before loading data into BigQuery. Defaults to `false`, which causes the pipeline to append data.

- **partitionColumn**: The column name for partitioning when reading data in parallel. This parameter supports `Long` partition columns.

- **table**: The table to read from when using partitions. This parameter also accepts a subquery in parentheses.  
  **Example**: `Person as table`

- **numPartitions**: The number of partitions. When the input is less than 1, the number is set to 1.

- **lowerBound**: The lower bound for the partition scheme. If not provided, it is inferred by Apache Beam.

- **upperBound**: The upper bound for the partition scheme. If not provided, it is inferred by Apache Beam.

- **fetchSize**: The number of rows to fetch from the database at a time. Defaults to `50000`.


- **disabledAlgorithms**: Comma-separated list of algorithms to disable.  
  **Example**: `SSLv3, RC4`

- **extraFilesToStage**: Comma-separated Cloud Storage paths or Secret Manager secrets for files to stage in the worker. These files are saved in the `/extra_files` directory in each worker.  
  **Example**: `gs:///file.txt,projects/<PROJECT_ID>/secrets/<SECRET_ID>/versions/<VERSION_ID>`

- **useStorageWriteApi**: If true, the pipeline uses the BigQuery Storage Write API. Defaults to `false`.

- **useStorageWriteApiAtLeastOnce**: Specifies the write semantics for the Storage Write API. Defaults to `false`.


## Requirements

- **Java 11**
- **Maven**
- **gcloud CLI**: Ensure that you have the gcloud CLI installed and configured. Execute the following commands:

  ```bash
  gcloud auth login
  gcloud auth application-default login
  
    ```


## Building the Template

To create the JAR file for the template, follow these steps:

1. **Clean the project**: This step ensures that any previous builds or cached files are removed.
   ```bash
   mvn clean
   
    ```
2. **Build the JAR file**: This step compiles the code and creates a JAR file.
   ```bash
   mvn package
   ```

## Creating and Pushing the Docker Image

Once the JAR file is created, you need to build a Docker image and push it to the Artifact Registry. Follow these steps:

1. **Build the Docker image**: Use the following command to build your Docker image. Replace the `ARTIFACT_REGISTRY_LOCATION` variable with your specific Artifact Registry location.
   ```bash
   docker build -t $ARTIFACT_REGISTRY_LOCATION/bq-to-jdbc .
   
    ```

For example, if your Artifact Registry location is us-east1-docker.pkg.dev/my-id-project, the command would be:


    ```bash
    docker build -t us-east1-docker.pkg.dev/my-id-project/bq-to-jdbc .
    
     ```
2. **Push the Docker image**: Use the following command to push your Docker image to the Artifact Registry. Replace the `ARTIFACT_REGISTRY_LOCATION` variable with your specific Artifact Registry location.
3. **Push the Docker image**: Use the following command to push your Docker image to the Artifact Registry. Replace the `ARTIFACT_REGISTRY_LOCATION` variable with your specific Artifact Registry location.
   ```bash
   docker push $ARTIFACT_REGISTRY_LOCATION/bq-to-jdbc
   
    ```
For more details on managing Docker images with Artifact Registry, including pushing and pulling images, refer to the official documentation [Artifact ](https://cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling?hl=es-419)
## Staging the Template

After creating your Docker image, you need to stage the template specification file to Google Cloud Storage. Follow these steps:

1. **Upload the template file**: Use the following command to upload the `bq-to-jdbc-image-spec.json` file to your preferred location in Google Cloud Storage. Replace `YOUR_BUCKET_NAME` with the name of your bucket.

   ```bash
   gsutil cp bq-to-jdbc-image-spec.json gs://YOUR_BUCKET_NAME/template/bq-to-jdbc-image-spec.json
       
    ```


## Running the Template



The `gcloud` command supports using flags files to simplify the management of complex parameter configurations. Flags files are YAML files where you define parameters and their values, which are then passed to the `gcloud` command. This approach helps to keep your command line calls concise and manageable.
For more information on using flags files, visit the [gcloud flags file documentation](https://cloud.google.com/sdk/gcloud/reference/topic/flags-file).


To execute the Dataflow template, use the following command. This command will run the job using the parameters specified in the `gcloud-flags-file.yaml` file.
```bash
gcloud dataflow flex-template run "YOUR_JOB_ID_NAME" --flags-file=gcloud-flags-file.yaml
```

### Connecting with JDBC Wallet

This example demonstrates how to run a Dataflow job using a JDBC wallet for secure database connections. The wallet files are placed in the /extra_files directory within the worker nodes. If you are not using a wallet, you can directly assign your database host without the wallet configuration. For more details on connecting with a JDBC wallet, refer to the [Oracle JDBC wallet documentation](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/connect-jdbc-thin-wallet.html#GUID-BE543CFD-6FB4-4C5B-A2EA-9638EC30900D).

## Connecting with JDBC Wallet

When connecting to a database using JDBC, you can use a wallet for secure and simplified database connections. A wallet contains the necessary credentials and configuration for secure database access.

### Using JDBC Wallet

1. **Include Wallet Files**: Upload your wallet files (e.g., `tnsnames.ora`, `cwallet.sso`, `ewallet.p12`, etc.) to Google Cloud Storage. Ensure these files are accessible to your Dataflow workers.

2. **Specify Wallet Location**: In your Dataflow template parameters, set the `connectionURL` to include the wallet configuration. For example:

    ```yaml
    connectionURL: jdbc:oracle:thin:@your_db_medium?TNS_ADMIN=/extra_files
    ```
    In this example, the `TNS_ADMIN` parameter specifies the location of the wallet files in the `/extra_files` directory.



## Testing Locally

This template supports local testing with the Apache Beam DirectRunner. This is useful for debugging and verifying your pipeline before deploying it to Google Cloud Dataflow. When testing locally with the DirectRunner, ensure that your wallet files are accessible in a local directory.

### Testing with DirectRunner

To run your pipeline locally using the DirectRunner, follow these steps:

1. **Ensure Wallet Files are Accessible**: Place your wallet files (e.g., `tnsnames.ora`, `cwallet.sso`, `ewallet.p12`, etc.) in a local directory, e.g., `./extra_files`.

2. **Run the Pipeline Locally**: Use the following Maven command to execute the pipeline with DirectRunner:

    ```bash
    mvn compile exec:java \
    -Pdirect-runner \
    -Dexec.mainClass=org.sigma.BqToJdbc \
    -Dexec.cleanupDaemonThreads=false \
    -Dexec.args="--runner=DirectRunner \
    --project=YOUR_PROJECT_ID \
    --region=YOUR_REGION \
    --serviceAccount=YOUR_SERVICE_ACCOUNT_EMAIL \
    --driverJars=gs://YOUR_BUCKET_NAME/jars/ojdbc8.jar,gs://YOUR_BUCKET_NAME/jars/ucp.jar,gs://YOUR_BUCKET_NAME/jars/oraclepki.jar,gs://YOUR_BUCKET_NAME/jars/osdt_cert.jar,gs://YOUR_BUCKET_NAME/jars/osdt_core.jar \
    --driverClassName=oracle.jdbc.driver.OracleDriver \
    --connectionURL=jdbc:oracle:thin:@YOUR_DB_HOST?TNS_ADMIN=./extra_files \
    --username=YOUR_JDBC_USERNAME \
    --password=YOUR_JDBC_PASSWORD \
    --bigQueryLoadingTemporaryDirectory=gs://YOUR_BUCKET_NAME/tmp/ \
    --query=\"SELECT * FROM YOUR_PROJECT_ID.YOUR_DATASET.YOUR_TABLE\" \
    --subnetwork=https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_ID/regions/YOUR_REGION/subnetworks/YOUR_SUBNETWORK \
    --usePublicIps=false \
    --table=YOUR_TABLE \
    --tempLocation=gs://YOUR_BUCKET_NAME/tmp \
    --connectionProperties=isTruncate=true;oracle.net.tns_adsmin=./extra_files;oracle.net.wallet_location=./extra_files"
    ```

   Replace placeholders such as `YOUR_PROJECT_ID`, `YOUR_REGION`, `YOUR_SERVICE_ACCOUNT_EMAIL`, `YOUR_BUCKET_NAME`, `YOUR_DB_HOST`, `YOUR_JDBC_USERNAME`, `YOUR_JDBC_PASSWORD`, `YOUR_DATASET`, `YOUR_TABLE`, and `YOUR_SUBNETWORK` with appropriate values for your environment.

### Notes

- **Local Directory for Wallet**: Ensure that the `extra_files` directory contains all necessary wallet files and is in the same location as specified in the `--connectionURL` and `--connectionProperties` parameters.
- **DirectRunner**: The DirectRunner is suitable for local development and testing but is not designed for large-scale data processing. For production runs, use Google Cloud Dataflow.

For more details about using the DirectRunner and local testing, refer to the [Apache Beam DirectRunner documentation](https://beam.apache.org/documentation/runners/direct/).

