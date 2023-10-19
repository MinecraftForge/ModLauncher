import groovy.json.JsonSlurper
import net.steppschuh.markdowngenerator.table.Table

import java.nio.file.Files
import java.nio.file.Path
import java.math.RoundingMode

@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
    @Grab('org.apache.groovy:groovy-json:4.0.13'),
    @Grab('com.github.Steppschuh:Java-Markdown-Generator:1.3.2')
])

final versions = [] as SortedSet
final javas = [:] as TreeMap
final results = [:] as TreeMap

final resultsPath = Path.of('build/test_artifacts')
for (def dir : Files.list(Path.of('build/test_artifacts'))) {
    def dirName = dir.fileName.toString()
    def file = dir.resolve('jmh_results.json')
    if (!dirName.startsWith('jmh-') || !Files.exists(file))
        continue
    (javaName,javaVersion) = dirName.substring('jmh-'.length()).split('-')
    javas.computeIfAbsent(javaName, { [] }).add(javaVersion)
    versions.add(javaVersion)
    
    def json = new JsonSlurper().parse(file.toFile())
    for (def bench : json) {
        def byJava = results.computeIfAbsent(bench.benchmark, { [:] })
        def byVersion = byJava.computeIfAbsent(javaName, { [:] })
        
        def result = bench.primaryMetric.score.setScale(3, RoundingMode.CEILING)
        if (!bench.primaryMetric.scoreError.equals('NaN'))
            result += ' ± ' + bench.primaryMetric.scoreError.setScale(3, RoundingMode.CEILING)
        //result += bench.primaryMetric.scoreUnit
        
        byVersion.put(javaVersion, result)
    }
}
def output = ""
results.forEach { bench, byJava -> 
    final table = new Table.Builder()
        .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
        .addRow((['Vendor'] + versions).toArray())
        
    javas.forEach { javaName, javaVersions ->
        def row = [javaName]
        if (!byJava.containsKey(javaName)) {
            versions.forEach { javaVersion -> 
                row.add(javaVersions.contains(javaVersion) ? "MISSING" : "")
            }    
        } else {
            def byVersion = byJava.get(javaName)
            versions.forEach { javaVersion -> 
                if (javaVersions.contains(javaVersion)) {
                    row.add(byVersion.containsKey(javaVersion) ? byVersion.get(javaVersion) : "MISSING") 
                } else {
                    row.add("")
                }
            }
        }
        table.addRow(row.toArray())
    }
    
    output += '### `' + bench + '` results\n' +
              table.build() + '\n' +
              '\n'
}

new File('jmh_results.md').text = output