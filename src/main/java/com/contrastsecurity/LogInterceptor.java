package com.contrastsecurity;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class LogInterceptor {

    public static String intercept(@This Object logger, @SuperCall Callable<String> zuper) throws Exception {
		// we are in a log call, so enter scope and run synthetic log test one time
		SafeLog4J.logScope.enterScope();
		try {
			Class cl = logger.getClass();

			// do tests if this is the first time seeing this particular logger class
			if ( Loggers.get( cl ) == null ) {
				Loggers.put( logger );
				Loggers.log( "" );
				Loggers.log( "" );
				Loggers.log( "============================================================================================" );
				Loggers.log( "Potentially vulnerable log4j instance detected in " + cl.getClassLoader() + "...");

				if ( SafeLog4J.checkMode ) {
					check( cl );
				}

				if ( SafeLog4J.blockMode ) {
					block( cl );
				}
				Loggers.log( "============================================================================================" );
				Loggers.log( "" );
				Loggers.log( "" );
			}
		} catch( Exception e ) {
			Loggers.log( "Error during log interception -- " + e.getMessage() );
		} finally {
			SafeLog4J.logScope.leaveScope();
		}
		return zuper.call();
    }

	public static void check( Class cl ) throws Exception {
		if ( !SafeLog4J.log4jTested && !SafeLog4J.logScope.inNestedScope() ) {
			try {
				SafeLog4J.testScope.enterScope();
				String payload = "${jndi:rmi://192.168.1.1:1099/test}";
				Loggers.log( "" );
				Loggers.log( "CHECK: Testing for vulnerability to log4shell..." );
				Loggers.log( "  Sending synthetic FATAL log message (below) to log4j..." );
		
				// send payload through log4j
				Loggers.log4j( payload, cl );
		
				if ( SafeLog4J.log4ShellFound ) {
					Loggers.log("  Payload detected in JndiLookup" );
					Loggers.log("  Log4j is exploitable if any untrusted input is logged" );
					if ( !SafeLog4J.blockMode ) {
						Loggers.log("  Enable 'block' mode to prevent exploitation" );
					}
				}
				else {
					Loggers.log("  Payload not detected in JndiLookup" );
					Loggers.log("  Log4j is not exploitable" );
				}

				SafeLog4J.log4jTested = true;
			} finally {
				SafeLog4J.testScope.leaveScope();
			}
		}
	}

	public static void block( Class cl ) throws Exception {
		Loggers.log( "" );
		Loggers.log( "BLOCK: Searching for vulnerable JNDI lookup class..." );

		if ( SafeLog4J.log4jFound ) {
			Loggers.log( "  Log4J JNDI lookup class identified" );
			Loggers.log( "  Vulnerable methods neutralized" );
		} else {
			Loggers.log( "  Log4J JNDI lookup class not present" );
		}
		Loggers.log( "  No longer vulnerable to log4shell" );
		if ( !SafeLog4J.checkMode ) {
			Loggers.log("  Enable 'check' mode to verify exploitability" );
		}
	}
}