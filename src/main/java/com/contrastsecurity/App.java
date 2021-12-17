package com.contrastsecurity;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import net.bytebuddy.agent.ByteBuddyAgent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class App {
    public static void main(String[] args){
        System.out.println("SafeLog4j by Contrast Security");
        System.out.println( "https://contrastsecurity.com" );

        if ( args.length > 0 && args.length < 3 ) {
            try{
                String pid = args[0];
                String options = args.length>=2 ? args[1] : null;
                String agentFilePath = "safelog4j-1.0.2.jar";
                File agentFile = new File(agentFilePath);
                ByteBuddyAgent.attach(agentFile.getAbsoluteFile(), pid, options);
                System.out.println("Attached to target jvm and loaded agent successfully");
            }catch(Exception e){
                e.printStackTrace();
            }
        } else {
            showHelp();
        }
    }

    public static void listProcesses(){
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        vms.stream()
            .filter(vm -> !vm.displayName().contains("safelog")) //No need to patch ourselves
            .forEach(vm -> {
            System.out.println(vm.id() + " \t" + vm.displayName());
        });
    }

    private static void showHelp(){
        System.out.println("This tool can be used in two modes for custom and third party applications:");
        System.out.println("1. At application startup, through the -javaagent flag.");
        System.out.println("2. Without application restart, if supported.");
        System.out.println();
        System.out.println("SafeLog4j can connect to and patch the following Java processes:");
        try{
            listProcesses();
            System.out.println();
            System.out.println("To patch a process, add an argument of the ID or use the word all.");
        }catch(NoClassDefFoundError err){
            System.err.println("Please use jcmd to list Java processes.");
        }
    }

}
