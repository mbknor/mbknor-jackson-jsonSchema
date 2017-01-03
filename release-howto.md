Change version in 

* build.sbt
* and multiple places in README.md

Clean and test cross scala version

    sbt clean +test

Commit
    
tag with version

    
Deploy to maven central:

    sbt +publish-signed
    

Check and publish artifact
    
    https://oss.sonatype.org/
    
change version to next snapshot in build.sbt
commit

    
push
push --tag  
