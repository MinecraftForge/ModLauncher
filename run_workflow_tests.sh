clear 

#Remove last run just in case
rm -rf artifacts/

act --artifact-server-path ./artifacts

# Uncompress all artifacts 
find ./artifacts/ -name *.gz__ | while read filename; do gunzip --suffix=.gz__ "$filename"; done;

# Add them to a archive in root dir
cd artifacts/1/; zip -r ../../test_artifacts.zip *; cd -

# Grab jmh results
mv artifacts/1/aggregate-results/jmh_results.md .

# Build JUnit Tests
./gradlew --continue test collectTests