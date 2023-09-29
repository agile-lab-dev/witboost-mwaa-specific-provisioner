# Changelog

All notable changes to this project will be documented in this file.

## v1.0.0

### Commits

- **[WIT-249] Some SPs provisioning status endpoint incorrectly reports completed for non existent tokens**
  > 
  > ##### New features and improvements
  > 
  > - Aligns the interface specification version to version 2.2.0
  > - Includes a Postman collections with some examples
  > 
  > ##### Related issue
  > 
  > Closes WIT-249
  > 
  > 

- **[WIT-249] Some SPs provisioning status endpoint incorrectly reports completed for non existent tokens**

- **Resolve WIT-287 "Update mwaa deployment"**

- **[WIT-204] HLD documentation for MWAA SP**

- **[WIT-195] API version field in OpenAPI spec is set to implementing server version**

- **[WIT-199] Documentation**

- **Resolve WIT-50 "Update mwaa helm chart"**

- **[WIP] feature: custom-changes-mwaa**

- **[#8] Resolve "Add file name prefix in the unprovision method"**

- **Resolve "Minor improvements"**
  > 
  > ##### New features and improvements
  > 
  > - Inserted a prefix folder dags and source inside the mwaa manager `executeProvision` function call
  > - Prefix also for the dag file name that ends up in the airflow so it looks like $componentIdentifier(without componentName).$dag_name
  > 
  > ##### Related issues
  > 
  > Closes #7

- **[#6] Resolve "Deploy MWAA sp"**
  > 
  > ##### New features and improvements
  > 
  > - Implemented k8s deployment
  > - Changed s3Client initialization in order to include WebIdentity
  > 
  > ##### Related issues
  > 
  > Closes #6

- **[#5] Resolve "Fix a little bug inside provision and unprovision"**
  > 
  > ##### Bugs fixed
  > 
  > - Fixed a bug for subfolders handling
  > 
  > ##### Related issues
  > 
  > Closes #5

- **[#2] Resolve "Import aws sdk the right way"**
  > 
  > ##### New features and improvements
  > 
  > - Added provision API
  > - Added unprovision API
  > - Imported aws sdk library
  > - Added an example of descriptor to pass to the provision and unprovision API (it is called pr_descriptor_mwaasp.yaml)
  > 
  > ##### Related issues
  > 
  > Closes #2

- **[#3] Resolve "Make akka http request to airflow"**
  > 
  > ##### New features and improvements
  > 
  > - Added `/provision` API
  > - Added docker-compose to start airflow locally
  > - Set up the logs
  > - Updated README with how to run airflow locally
  > 
  > ##### Related issues
  > 
  > Closes #3

- **Init**
