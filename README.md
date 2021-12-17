# safelog4j

If you're wrestling with log4shell [CVE-2021-45046](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-45046), the best thing to do is to upgrade your log4j to the latest secure version.

But if you can't do that for whatever reason, you probably want to be *really* sure that you have a problem and an easy way to fix it.

Safelog4j is an instrumentation-based tool to help you discover, verify, and solve log4shell without scanning or upgrading.

* accurately discovers the use of log4j
* verifies that the log4shell vulnerability is actually present and exploitable
* prevents the log4shell vulnerability from being exploited

Safelog4j doesn't rely on version numbers or filenames. Instead, it instruments the application to find log4j and perform an internal test to prove the app is exploitable. Safelog4j also uses instrumentation to disable the JNDI lookup code used by the attack. This is the most effective way to inoculate an otherwise vulnerable application or API.

![safelog4j-screenshot](https://github.com/Contrast-Security-OSS/safelog4j/blob/main/src/main/resources/safelog4j-screenshot.png?raw=true)


## Why should you use this

Unfortunately, log4j is deployed in a huge variety of ways that make traditional detection very difficult. Scanning file systems, code repos, or containers is very likely to fail to detect log4j accurately. Determining exploitability by attempting to test, scan, or fuzz for log4shell is even more inaccurate, requiring exactly right input with the exactly right syntax.

* log4j could be buried in a fat jar, war, or ear
* log4j could be shaded in another jar
* log4j could be included in the appserver, not the code repo
* log4j could be part of dynamically loaded code or plugin
* log4j could have multiple different versions with different classloaders, some vulnerable some not
* log4j could be masked by use of slf4j or other layers
* log4j could be renamed, recompiled, or otherwise changed


## How it works

You can use safelog4j in just about any environment using Java.

1. Download the latest [safelog4j-1.0.1.jar](https://github.com/Contrast-Security-OSS/safelog4j/releases/download/v1.0.1/safelog4j-1.0.1.jar)
1. Place the jar file anywhere on the server you wish to defend
1. Either set the javaagent flag wherever you launch Java
  ```shell
  java -javaagent:/path/to/safelog4j-1.0.jar=[check|block|both|none] -jar yourjar.jar
  ```
  -or-
  ```
  JAVA_TOOL_OPTIONS=-javaagent:/path/to/safelog4j-1.0.jar=[check|block|both|none]
  ```
1. Restart the application.


## Safelog4j Options

* CHECK means that safelog4j will actually test every log4j instance for log4shell. This is done by generating a synthetic log message and a sensor to detect it in the vulnerable clsas. This is iron clad evidence the application is vulnerable -- provided unttrusted data reaches that logger.

* BLOCK means that safelog4j will stub out all the methods in the JNDI lookup class.  This is the recommended approach to ensure that log4j can't be exploited. It is harmless, except for the total prevention of this attack.

* BOTH simply means that both CHECK and BLOCK will occur.

* NONE disables both CHECK and BLOCK, allowing you to keep the agent in place but disabled.


## License

This software is licensed under the Apache 2 license

Copyright 2021 Contrast Security. https://contrastsecurity.com

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
