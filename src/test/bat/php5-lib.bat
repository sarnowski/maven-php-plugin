del my-app
call mvn archetype:generate -DarchetypeGroupId=org.phpmaven -DarchetypeArtifactId=php5-lib-archetype -DarchetypeVersion=1.0-ALPHA-2 -DgroupId=org.sample -DartifactId=my-app
cd my-app
call mvn package -X
