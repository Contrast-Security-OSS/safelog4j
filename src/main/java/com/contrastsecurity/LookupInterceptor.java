package com.contrastsecurity;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class LookupInterceptor {

    public static String intercept(@SuperCall Callable<String> zuper, @AllArguments Object... args) throws Exception {

        // is this a test log?
        if ( SafeLog4J.testScope.inScope() ) {
            SafeLog4J.log4ShellFound = true;
            return null;
        }

        return zuper.call();
    }
}