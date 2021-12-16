package com.contrastsecurity;

import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.matcher.StringMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class SafeLog4J {

	public static BinaryScope logScope = new BinaryScope();

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
		Loggers.log( "Tests for log4shell will run on each instance of log4j when it is loaded" );
		Loggers.log( "" );

		AgentBuilder builder = new AgentBuilder.Default()
		.with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
		.with(new InstListener(new StringMatcher(".log4j.core.lookup.JndiLookup", StringMatcher.Mode.ENDS_WITH)));

		if ( blockMode ) {
			builder = builder
			.type(nameEndsWith(".log4j.core.lookup.JndiLookup"))
			.transform((b,t,c,m) -> b.method(any()).intercept(StubMethod.INSTANCE));
		}

		if ( checkMode ) {
			builder = builder
			.type(nameEndsWith(".log4j.core.Logger"))
			.transform((b,t,c,m) -> b.method(named("log")).intercept(MethodDelegation.to(LogInterceptor.class)));

			builder = builder
			.type(nameEndsWith(".log4j.core.lookup.JndiLookup"))
			.transform((b,t,c,m) -> b.method(named("lookup")).intercept(MethodDelegation.to(LookupInterceptor.class)));
		}

		builder.installOn(inst);
	}

}
