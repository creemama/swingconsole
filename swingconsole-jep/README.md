# Java Embedded Python (JEP) SwingConsole

> A Java Swing interactive console for [Java Embedded Python (JEP)](https://github.com/ninia/jep) with readline-esque support

<img alt="Java Embedded Python (JEP) SwingConsole - Hello, World!" src="https://raw.githubusercontent.com/creemama/swingconsole/master/swingconsole-jep/JEP-SwingConsole.gif" width="500">

Try it out by executing the following:

```sh
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:copy \
  -Dartifact=com.creemama.swingconsole:swingconsole-jep:3.9.0.0.0.1-SNAPSHOT:jar:jar-with-dependencies \
  -DoutputDirectory=. \
&& java -Djava.library.path=/path/to/jep.dll.jnilib.so/dir -jar swingconsole-jep-3.9.0.0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

Use the following code snippet as a guide to embed a `JEP SwingConsole` into your application:

```java
public static void main(String[] args) throws JepException {
	ConsoleConfig config = new ConsoleConfig()
			.evalFile("/path/to/startup/script.py")
			.put("java_variable", new StringBuilder("Console-Accessible Variable"))
			.banner("JEP {{VERSION}} Python {{PYTHON_VERSION}} Java " + System.getProperty("java.version") + "\n")
			.historyFile(new File(System.getProperty("user.home"), ".jep"));
	SwingConsoleFrame console = new SwingConsoleFrame("Java Embedded Python (JEP) Console");
	console.run(new JepSwingConsole(config));
}

```