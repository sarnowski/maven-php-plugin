call versions.bat
call mvn archetype:generate -DarchetypeGroupId=org.phpmaven -DarchetypeArtifactId=php5-lib-archetype -DarchetypeVersion=%PHPMAVENVERSION% -DgroupId=org.sample -DartifactId=my-app
call testit
