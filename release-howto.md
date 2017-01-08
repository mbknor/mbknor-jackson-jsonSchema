Change version in 

* version.sbt

Clean and test cross scala version

    sbt clean +test

Commit
    
tag with version

    
Deploy to maven central:

    sbt +publish-signed
    

Check and publish artifact
    
    https://oss.sonatype.org/
    
change version to next snapshot in version.sbt
commit

    
push
push --tag  
