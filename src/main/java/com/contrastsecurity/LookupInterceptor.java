package com.contrastsecurity;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class LookupInterceptor {

    public static String intercept(@SuperCall Callable<String> zuper, @AllArguments Object... args) throws Exception {
        // call to Lookup from within the scope of a logger call
        if ( SafeLog4J.logScope.inScope() ) {
            SafeLog4J.log4ShellFound = true;
        }

        if ( SafeLog4J.checkMode && SafeLog4J.testScope.inScope() ) {
            return null;
        }

        return zuper.call();
    }
}