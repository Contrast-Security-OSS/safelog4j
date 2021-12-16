package com.contrastsecurity;

import java.util.concurrent.Callable;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class LookupInterceptor {

    @RuntimeType
    public static Object intercept(@AllArguments Object[] args, @SuperCall Callable<String> zuper) throws Exception {

        // is this a test log?
        if ( SafeLog4J.testScope.inScope() ) {
            SafeLog4J.log4ShellFound = true;
            return null;
        }

        if ( SafeLog4J.blockMode ) {
            return null;
        }

        return zuper.call();
    }
}

  