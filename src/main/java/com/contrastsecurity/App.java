package com.contrastsecurity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class App {

    private static final String SELF;

    private static boolean CAN_ATTACH;

    static{
        String self;
        try {
            self = App.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .toURI()
            .getPath();
        } catch (URISyntaxException e) {
            self=null;
        }
        SELF=self;
        //Need the agent's location or Attach won't work.
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            CAN_ATTACH=SELF!=null;
        } catch (ClassNotFoundException e) {
            CAN_ATTACH=false;
        }
        
    }

    public static void main(String[] args){
        System.out.println("SafeLog4j by Contrast Security");

        if(args.length>0 && CAN_ATTACH){
            String options = args.length>=2 ? args[1] : null;
            patch(args[0].trim(), options);
        }else{
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

        if(!CAN_ATTACH){
            System.out.println("  -- Option 2 is not supported on this system.");
        }else{
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

    private static void patch(String process, String options){
        try{
            int pid = Integer.parseInt(process);
            patch(pid, options);
        }catch(NumberFormatException ex){
            if("all".equalsIgnoreCase(process)){
                System.out.println("Patching all...");
                List<VirtualMachineDescriptor> vms = VirtualMachine.list();
                vms.stream()
                    .filter(vm -> !vm.displayName().contains("safelog")) //No need to patch ourselves
                    .map(vm -> vm.id())
                    .forEach(id -> patch(Integer.parseInt(id), options));
            }else{
                System.err.println("Uknown process: " + process);
                System.err.println("  Use a number or all.");
            }
        }
    }

    private static void patch(int process, String options){
        try {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(process));
            vm.loadAgent(SELF, options);
            System.out.println("Successfully patched process "
                + process 
                + ". You should see safelog4j messages in its log.");
        } catch (AttachNotSupportedException | IOException e) {
            if("Non-numeric value found - int expected".equals(e.getMessage())){
                //Odd interplay between majors. https://bugs.eclipse.org/bugs/show_bug.cgi?id=534489
                String javaHome = System.getProperty("java.home");
                System.err.println("Unable to bridge Java version to process "
                    + process
                    + ". Please re-run this tool from its Java installation instead of " + javaHome);
            }else{
                System.err.println("Unable to connect to " + process + ". " + e.getMessage());
            }
        } catch (AgentLoadException | AgentInitializationException e) {
            System.err.println("Attempt to patch was unsuccessful. Process "
                + process
                + " must be restarted to be patched. "
                + e.getMessage());
        }
    }
}
