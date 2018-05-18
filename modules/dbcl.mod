[description]
Enables webapplication deployment from a database.

[depend]
deploy
webapp

[lib]
lib/dbcl-jetty-deploy-PA1.jar

[files]
webapps/

[xml]
etc/dbcl-jetty-deploy.xml

[ini-template]
# Monitored table name (fully qualified)
# dbcl.deploy.monitoredTable=DBCLASSLOAD.DEPLOYED

# Defaults Descriptor for all deployed webapps
# jetty.deploy.defaultsDescriptorPath=${jetty.base}/etc/webdefault.xml

# Monitored directory scan period (seconds)
dbcl.deploy.scanInterval=0

# Whether to extract *.war files
# dbcl.jetty.extractWars=true

#
dbcl.jetty.driver.url=jar:file
dbcl.jetty.jdbc.url=jdbc:postgresql://192.168.1.12:5432/postgres
dbcl.jetty.jdbc.user=DBCLASSLOAD
dbcl.jetty.jdbc.passwd=Tr1ss
