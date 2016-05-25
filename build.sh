#!/bin/sh
javac src/*.java
rm -f CSGF.jar
cd src
find . -name .DS_Store -type f-delete
jar cvfe ../CSGF.jar CrossSpeciesGeneFinder `find . -type f`
cd ..
find . -name '*.class' -type f -delete
find . -name '*\~' -type f -delete