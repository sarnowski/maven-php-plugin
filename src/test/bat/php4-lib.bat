call versions.bat
call php4.bat
call mvn archetype:generate -DarchetypeGroupId=org.phpmaven -DarchetypeArtifactId=php4-lib-archetype -DarchetypeVersion=%PHPMAVENVERSION% -DgroupId=org.sample -DartifactId=my-app
call testit