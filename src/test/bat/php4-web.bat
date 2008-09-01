del my-app
call mvn archetype:generate -DarchetypeGroupId=org.phpmaven -DarchetypeArtifactId=php4-web-archetype -DarchetypeVersion=1.0-ALPHA -DgroupId=org.sample -DartifactId=my-app
cd my-app
call mvn package
