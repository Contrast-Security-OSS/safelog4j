package com.contrastsecurity;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.StringMatcher;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class SafeLog4J {

	public static BinaryScope logScope = new BinaryScope("log");
	public static BinaryScope testScope = new BinaryScope("test");

	public static boolean blockMode = true;
	public static boolean checkMode = true;
	public static boolean agentRunning = false;
	public static boolean log4jTested = false;
	public static boolean log4jLibrariesFound = false;
	public static boolean log4ShellClassesFound = false;
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
		switch(args.toLowerCase()) {
			case "block" : checkMode = false; break;
			case "check" : blockMode = false; break;
			case "none"  : return;
		}

		Loggers.log( "     _____ ___    ______________    ____  ________ __      __" );
		Loggers.log( "    / ___//   |  / ____/ ____/ /   / __ \\/ ____/ // /     / /" );
		Loggers.log( "    \\__ \\/ /| | / /_  / __/ / /   / / / / / __/ // /___  / / " );
		Loggers.log( "   ___/ / ___ |/ __/ / /___/ /___/ /_/ / /_/ /__  __/ /_/ /" ); 
		Loggers.log( "  /____/_/  |_/_/   /_____/_____/\\____/\\____/  /_/  \\____/" );

		Loggers.log( "    by Contrast Security - https://contrastsecurity.com" );
		Loggers.log( "" );
		Loggers.log( " Safelog4j is an instrumentation-based security tool to help teams" );
		Loggers.log( "discover, verify, and solve log4shell without scanning or upgrading." );
		Loggers.log( "      https://github.com/Contrast-Security-OSS/safelog4j" );
		Loggers.log( "" );
		Loggers.log( "Checking: " + ( checkMode ? "enabled" : "disabled" ) );
		Loggers.log( "Blocking: " + ( blockMode ? "enabled" : "disabled" ) );
		Loggers.log( "" );

		new AgentBuilder.Default()
		// .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
		// .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
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

		.type(new AgentBuilder.RawMatcher() {
			@Override
			public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
				if ( typeDescription.getCanonicalName().contains( ".log4j.")) {
					log4jLibrariesFound = true;
					Libraries.addLibrary( protectionDomain.getCodeSource().getLocation().toString() );
				}
				return false;
			}
		})
		.transform(new AgentBuilder.Transformer() {
			@Override
			public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
				return null;
			}
		})
		
		.installOn(inst);

	}

}
