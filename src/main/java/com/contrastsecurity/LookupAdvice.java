package com.contrastsecurity;

import net.bytebuddy.asm.Advice;

public class LookupAdvice {

    @Advice.OnMethodEnter( skipOn = String.class )
    public static String onEnter() {
        // is this a test log?
        if ( SafeLog4J.testScope.inScope() ) {
            SafeLog4J.log4ShellFound = true;
            return "attack blocked by safelog4j";
        }

        if ( SafeLog4J.blockMode ) {
            return "attack blocked by safelog4j";
        }

        // in check mode so don't skip to the return
        return null;
    }

    @Advice.OnMethodExit
    public static void onExit( @Advice.Enter String enter, @Advice.Return(readOnly = false) String ret) {
        ret = enter;
    }

}