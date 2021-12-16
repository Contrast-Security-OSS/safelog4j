# safelog4j

Safelog4j is an instrumentation-based tool to help you discover, verify, and solve the log4shell vulnerability in log4jv2 [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046).

* accurately discovers the use of log4j
* verifies that the log4shell vulnerability is actually present and exploitable
* prevents the log4shell vulnerability from being exploited

You can use safelog4j on any JVM and it doesn't require source code.

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

* CHECK means that safelog4j will actually test every log4j instance for log4shell. safelog4j will generate with a synthetic log message, send it through log4j, and detect whether it caused the JNDI lookup to occur.  If so, we know this application is susceptible to log4shell if it sends any untrusted input to that log4j instance.

* BLOCK means that safelog4j will stub out all the methods in the JNDI lookup class.  This is the recommended approach to ensure that log4j can't be exploited. It is harmless, except for the total prevention of this attack.

* BOTH simply means that both CHECK and BLOCK will occur.

* NONE disables both CHECK and BLOCK, allowing you to keep the agent in place but disabled.

## Usage

SafeLog4j is a Java agent and can be used on any Java application from any OpenJDK vendor: Oracle, AdoptOpenJDK, Azul, Corretto, Liberica, etc.

1. Download the latest safelog4j-1.0.jar
1. Place the jar file anywhere on the server you wish to defend - /opt for instance
1. Create an environment variable for the user, either in .bashrc or any other location that can affect the user:
  ```shell
  JAVA_TOOL_OPTIONS=-javaagent:/path/to/safelog4j-1.0.jar
  ```
1. Restart the application.

Once in place, your application will be defended and you will see output like the picture below. Logs by the defense begin with [safelog4j].

PICTURE


