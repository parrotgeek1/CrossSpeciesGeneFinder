# Cross-Species Gene Finder
Open source, cross-platform tool to search for similar genes across species using the NCBI database. Used in [this research project](https://github.com/parrotgeek1/CrossSpeciesGeneFinder/raw/master/TvedteGradRetreatPosterSp16.pdf) by Eric Tvedte at the University of Iowa.

# NOTE 2018-04-24: It is not working again due to changes in NCBI's API. It will be fixed when I have time. I apologize for the inconvenience.

# [Download (.jar)](https://github.com/parrotgeek1/CrossSpeciesGeneFinder/raw/master/CSGF.jar)
Double-click CSGF.jar to start. You will be given instructions. Java is required.
# [Source Code](https://github.com/parrotgeek1/CrossSpeciesGeneFinder)

CSGF Batch File Format
---

The file extension is always ".txt".

The first line of the file starts with:

```
!CSGFBatchV1
```

After the colon, you can put either:

* A species name, a colon, and a maximum e value: 

        !CSGFBatchV1:Nasonia giraulti:1e-30

* A species name, a colon, a maximum e value, and a custom buffer size on both sides of the gene: 

        !CSGFBatchV1:Nasonia giraulti:1e-30:2000

* A species name, a colon, a maximum e value, and 2 custom buffer sizes on either side of the gene: 

        !CSGFBatchV1:Nasonia giraulti:1e-30:2000,3000

The default buffer size is 1000 bases on both sides, if unspecified.

Extraneous spaces between colons and commas, or at the END of the line, will be ignored. The file must start with EXACTLY !CSGFBatchV1 in that capitalization, with no extra spaces.

The rest of the file is composed of NCBI gene IDs, one per line. Extraneous spaces at the beginning or end of the line will be ignored.

A comment (any text after a #) will cause the rest of the line it is in to be ignored, and can be anywhere in the file except on the first line. Comments can either be at the beginning of a line, or after a valid gene ID, at the end. There can be any number of spaces before or after the #.

Blank lines, or lines consisting only of spaces, are silently ignored.

To Do
---

* add NOT specifying max e value or species on batch file
* can pause and resume?
* expire date show for results
* more code cleanup
