package com.contrastsecurity;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.matcher.StringMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class SafeLog4J {

	public static BinaryScope logScope = new BinaryScope("log");
	public static BinaryScope testScope = new BinaryScope("test");

	public static boolean blockMode = true;
	public static boolean checkMode = true;
	public static boolean agentRunning = false;
	public static boolean log4jTested = false;
	public static boolean log4jFound = false;
	public static boolean log4ShellFound = false;

	public static void premain(String args, Instrumentation inst) {
		transform( args, inst );
	}

	public static void agentmain(String args, Instrumentation inst) {
		transform( args, inst );
	}

	public static void transform(String args, Instrumentation inst) {
		if ( agentRunning ) {
			Loggers.log( "Already running? Check for multiple -javaagent declarations" );
			return;
		}
		agentRunning = true;

		if ( args == null ) args = "both";
		switch(args) {
			case "block" : checkMode = false; break;
			case "check" : blockMode = false; break;
			case "none"  : checkMode = false; blockMode = false; break;
		}

		Loggers.log( "SafeLog4J from Contrast Security" );
		Loggers.log( "https://contrastsecurity.com" );
		Loggers.log( "" );
		Loggers.log( "Instrumenting application to prevent log4shell exploits" );
		Loggers.log( "Usage: -javaagent:safelog4j.jar         -- enable both check and block" );
		Loggers.log( "     : -javaagent:safelog4j.jar=check   -- check for log4j exploitability" );
		Loggers.log( "     : -javaagent:safelog4j.jar=block   -- block log4j exploits from succeeding" );
		Loggers.log( "     : -javaagent:safelog4j.jar=none    -- disable both check and block" );
		Loggers.log( "" );
		Loggers.log( "Check mode: " + ( checkMode ? "enabled" : "disabled" ) );
		Loggers.log( "Block mode: " + ( blockMode ? "enabled" : "disabled" ) );
		Loggers.log( "" );
		Loggers.log( "Java supports running multiple different log4j instances in separate classloaders." );
		Loggers.log( "SafeLog4J will analyze and protect each log4j instance when first loaded" );
		Loggers.log( "" );

		new AgentBuilder.Default()
		.with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
		.with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
		.with(new InstListener(new StringMatcher(".log4j.core.lookup.JndiLookup", StringMatcher.Mode.ENDS_WITH)))
		.with(RedefinitionStrategy.RETRANSFORMATION)
		.with(InitializationStrategy.NoOp.INSTANCE)
		.with(TypeStrategy.Default.REDEFINE)
		.disableClassFormatChanges()

		.type(nameEndsWith(".log4j.core.Logger"))
		.transform(new AgentBuilder.Transformer.ForAdvice()
			.include(ClassLoader.getSystemClassLoader(), inst.getClass().getClassLoader())
			.advice(isMethod(), LogAdvice.class.getName()))

		.type(nameEndsWith(".log4j.core.lookup.JndiLookup"))
        .transform(new AgentBuilder.Transformer.ForAdvice()
			.include(ClassLoader.getSystemClassLoader(), inst.getClass().getClassLoader())
			.advice(isMethod(), LookupAdvice.class.getName()))

		.installOn(inst);
		
	}

}
