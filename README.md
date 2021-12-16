# safelog4j

Safelog4j is an instrumentation-based tool to help you discover, verify, and solve the log4shell vulnerability in log4jv2 [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046).

* accurately discovers the use of log4j
* verifies that the log4shell vulnerability is actually present and exploitable
* prevents the log4shell vulnerability from being exploited

You can use safelog4j on any JVM and it doesn't require source code.

![safelog4j-screenshot](https://github.com/Contrast-Security-OSS/safelog4j/blob/main/src/main/resources/safelog4j-screenshot.png?raw=true)

## Why should you use this

Unfortunately, log4j is deployed in a huge variety of ways that make traditional detection very difficult. Scanning file systems, code repos, or containers is very likely to fail to detect log4j accurately. Attempting to test, scan, or fuzz for log4shell is even more inaccurate, requiring exactly right input with the exactly right syntax.

* log4j could be buried in a fat jar, war, or ear
* log4j could be shaded in another jar
* log4j could be included in the appserver, not the code repo
* log4j could be part of dynamically loaded code or plugin
* log4j could have multiple different versions with different classloaders, some vulnerable some not
* log4j could be masked by use of slf4j or other layers
* log4j could be renamed, recompiled, or otherwise changed

Fortunately, we can use instrumentation to insert a few simple sensors that will detect log4j in all of these situations.

## How it works

Safelog4j is pretty simple. You add it to your JVM as described below. After that, it will simply "check" or "block" for log4shell.  There are four modes of operation.

* CHECK means that safelog4j will actually test every log4j instance for log4shell. This is done by generating a synthetic log message and a sensor to detect it in the vulnerable clsas. This is iron clad evidence the application is vulnerable -- provided unttrusted data reaches that logger.

* BLOCK means that safelog4j will stub out all the methods in the JNDI lookup class.  This is the recommended approach to ensure that log4j can't be exploited. It is harmless, except for the total prevention of this attack.

* BOTH simply means that both CHECK and BLOCK will occur.

* NONE disables both CHECK and BLOCK, allowing you to keep the agent in place but disabled.

## Usage

SafeLog4j operates in two modes:
1. Agent Mode, to defend Java applications as they are started and restarted.
1. Command Mode, to dynamically patch a running application.

When Command Mode is used, we recommend pairing it with Agent Mode to avoid re-work the next time the application is restarted.

### Agent Mode

SafeLog4j is a Java agent and can be used on any Java application from any OpenJDK vendor: Oracle, AdoptOpenJDK, Azul, Corretto, Liberica, etc.

1. Download the latest [safelog4j-1.0.1.jar](https://github.com/Contrast-Security-OSS/safelog4j/releases/download/v1.0.1/safelog4j-1.0.1.jar)
1. Place the jar file anywhere on the server you wish to defend - /opt for instance
1. Create an environment variable for the user, either in .bashrc or any other location that can affect the user:
  ```shell
  JAVA_TOOL_OPTIONS=-javaagent:/path/to/safelog4j-1.0.jar=[check|block|both|none]  # default is both
  ```
1. Restart the application.

Once in place, your application will be defended and you will see output like the picture below. Logs by the defense begin with [safelog4j].


### Command Mode

SafeLog4j can be run as a Java command to connect to and patch a running Java process. If you attempt to patch a process more than once, the first and correct patch will remain in place.

1. Access a terminal where the application is located, with the same user account as your application.
1. Run safelog4j as a Java application. It will list your Java processes, allowing you to patch each one or all.

To list Java processes:
```shell
java -Xbootclasspath/a:/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home/lib/tools.jar -jar safelog4j-1.0.jar
```

To patch a Java process
```shell
java -Xbootclasspath/a:/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home/lib/tools.jar -jar safelog4j-1.0.jar 123
```
To patch all Java processes
```shell
java -Xbootclasspath/a:/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home/lib/tools.jar -jar safelog4j-1.0.jar all
```
## Comparison to other Scanners and Structures
| Defense | How it compares |
| ------- | ----- |
| File scanning | Logging frameworks can missed when buried inside [fat jars](https://www.baeldung.com/gradle-fat-jar) that are commonly used. A single file does not exist and therefore will not be found. SafeLog4j will find those. |
| Shaded JARs | [Shading](https://maven.apache.org/plugins/maven-shade-plugin/) is a process of moving classes from one package name to another. Logging frameworks are often moved for compatibility reasons to avoid conflicts between libraries. SafeLog4j will find shaded copies of log4j2. |
| Application Servers | Scanning tools will miss vulnerable versions of log4j2 when they scan an application that goes inside the application server but do not scan the application server itself. SafeLog4j analyzes the entire stack to locate vulnerable copies of log4j2. |
| Multiple copies of log4j and other logging frameworks | Some applications, especially large enterprise applications, may contain multiple copies of logging frameworks. While many detection techniques will stop at the first, SafeLog4j will identify and neutralize each vulnerable Log4j2 as it is loaded. |

## License

This software is licensed under the Apache 2 license

Copyright 2021 Contrast Security. https://contrastsecurity.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
