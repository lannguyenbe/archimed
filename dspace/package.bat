# DSpace-5.0
mvn -U clean package -Dmirage2.on=true -Dmirage2.deps.included=false -Ddb.name=oracle -Denv=ptg_dspace2

# DSpace-4.2 with Mirage2
mvn clean package -Ddb.name=oracle -Denv=ptg_dspace2 -Dmirage2.on=true
@REM mvn package -rf :xmlui-mirage2 -Ddb.name=oracle -Denv=ptg_dspace2 -Dmirage2.on=true