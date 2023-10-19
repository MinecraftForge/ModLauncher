import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory

import java.nio.file.Files
import java.nio.file.Path
import java.math.RoundingMode

@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
    //@Grab('org.codehaus.groovy:groovy-xml:3.0.19')
    @Grab('org.apache.groovy:groovy-json:4.0.13')
])

final javas = [:] as TreeMap
final results = [:] as TreeMap

for (def dir : Files.list(Path.of('build/test_artifacts'))) {
    def javaName = dir.fileName.toString()
    if (!javaName.startsWith('test-results-'))
        continue
    
    (javaName,javaVersion) = javaName.substring('test-results-'.length()).split('-')
    javas.computeIfAbsent(javaName, { [] as SortedSet }).add(javaVersion)
    
    for (def file : Files.list(dir)) {
        def fileName = file.fileName.toString()
        if (!fileName.startsWith('TEST-') || !fileName.endsWith('.xml'))
            continue
        
        def data = DOMBuilder.parse(new StringReader(file.toFile().text)).documentElement
        use(DOMCategory) {
            def byTest = results.computeIfAbsent(data['@name'], { [:] })
            for (def testcase : data.testcase) {
                def byJava = byTest.computeIfAbsent(testcase['@name'], { [:] })
                def byVersion = byJava.computeIfAbsent(javaName, { [:] })
                byVersion.put(javaVersion, testcase.failure.isEmpty())
            }
        }
    }
}

def output = new StringBuilder("<html><body>")
output.append("""
<html>
  <style> 
    .tooltip-text {
      visibility: hidden;
      position: absolute;
      z-index: 1;
      width: 100px;
      color: white;
      font-size: 12px;
      background-color: #192733;
      border-radius: 10px;
      padding: 10px 15px 10px 15px;
      top: -40px;
      left: -50%;
    }

    .hover-text:hover .tooltip-text {
      visibility: visible;
    }
    
    .success {
      background-color: #008000;
    }
    
    .failure {
      background-color: #b60808;
    }

    .hover-text {
      font-size: 16px;
      position: relative;
      display: inline;
      font-family: monospace;
      text-align: center;
    }
    
    table, th, td {
      border: 1px solid black;
      border-collapse: collapse;
    }

    th, td {
       padding-left: 3px;
       padding-right: 3px;
    }

    .result {
        font-size: 0px;
    }
  </style>
  <body>
""")
results.forEach{ suite, byTest -> 
    output.append("<h1>${suite}</h1>\n")
    output.append("<table>\n")
    output.append("  <thead>\n")
    output.append("    <th>Test</th>\n")
    javas.keySet().forEach{ javaName -> 
        output.append("    <th>${javaName}</th>\n")
    }
    output.append("  </thead>\n")
    output.append("  <tbody>\n")
    byTest.forEach{ testName, byJava ->
        output.append("    <tr>\n")
        output.append("      <td>${testName}</td>\n")
        javas.forEach{ javaName, versions ->
            output.append("      <td class=\"result\">\n")
            def byVersion = byJava.get(javaName)
            versions.forEach { ver ->
                if (byVersion.containsKey(ver) && byVersion.get(ver)) {
                    output.append("        <span class=\"hover-text success\">O<span class=\"tooltip-text success\" id=\"failure\">${javaName} v${ver}</span></span>\n")
                } else {
                    output.append("        <span class=\"hover-text failure\">X<span class=\"tooltip-text failure\">${javaName} v${ver}</span></span>\n")
                }
            }
            output.append("      </td>\n")
        }
        output.append("    </tr>\n")
    }
    output.append("  </tbody>\n")
    output.append("</table>\n")
    
}
output += "</body></html>"


new File('test_results.html').text = output