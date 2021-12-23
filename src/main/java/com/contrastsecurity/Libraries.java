package com.contrastsecurity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class Libraries {

    private static Set<String> addedSingle = new HashSet<String>();
    private static Set<String> addedAll = new HashSet<String>();
    private static Set<Library> libraries = new HashSet<Library>();

    public static void main( String[] args ) throws Exception {
        String url1 = "jar:file:/Users/jeffwilliams/Downloads/log4j%20demo/myproject-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/log4j-api-2.14.1.jar!/";
        String url2 = "jar:file:/Users/jeffwilliams/Downloads/log4j%20demo/myproject-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/log4j-core-2.14.1.jar!/";

        Libraries.addLibrary( url1 );
        dump();
        System.out.println( "=======" );
        Libraries.addAllLibraries( url2 );
        dump();
    }

    // add this specific jar file
    public static void addLibrary( String codesource ) {
        if ( addedSingle.contains( codesource ) ) {
            return;
        }

        if ( !isArchive( codesource ) ) {
            return;
        }

        try {
            String decoded = URLDecoder.decode( codesource, "UTF-8" );
            String filepath = decoded.substring( decoded.lastIndexOf(":") + 1);
            String parts[] = filepath.split( "!/" );
            String path = parts[0];
            File f = new File( path );
            JarFile jarFile = new JarFile( f );

            JarInputStream jis = new JarInputStream( new FileInputStream( f ) );
            JarEntry entry = null;
            while ((entry = jis.getNextJarEntry()) != null) {     
                String nestedName = entry.getName();
                if ( parts.length > 1 && nestedName.equals( parts[1] )) {
                    Library innerlib = new Library( nestedName );
                    libraries.add( innerlib );

                    InputStream nis1 = jarFile.getInputStream( entry );
                    innerlib.md5 = hash( nis1, MessageDigest.getInstance("MD5") );

                    InputStream nis2 = jarFile.getInputStream( entry );
                    innerlib.sha1 = hash( nis2, MessageDigest.getInstance("SHA1") );
                
                    InputStream nis3 = jarFile.getInputStream( entry );
                    JarInputStream innerJis = new JarInputStream( nis3 );

                    Manifest mf = innerJis.getManifest();
                    if ( mf != null ) {
                        Attributes attr = mf.getMainAttributes();
                        String group = attr.getValue( "Implementation-Vendor-Id" );
                        String artifact = attr.getValue( "Implementation-Title" );
                        if ( group != null ) innerlib.group = group;
                        if ( artifact != null ) innerlib.artifact = artifact;

                        // for ( Entry<Object, Object> e : mf.getMainAttributes().entrySet() ) {
                        //      System.out.println( "   manifest::  "+  e.getKey() + " -> " + e.getValue() );
                        // }
                    }
                }
            }
        } catch( Exception e ) {
            Loggers.log( "The safelog4j project needs your help to deal with unusual CodeSources." );
            Loggers.log( "Report issue here: https://github.com/Contrast-Security-OSS/safelog4j/issues/new/choose" );
            Loggers.log( "Please include URL: " + codesource );
        }
        addedSingle.add( codesource );
    }

    // find containing jar file and include ALL libraries
    public static void addAllLibraries( String codesource ) {
        if ( addedAll.contains( codesource ) ) {
            return;
        }
        
        if ( !isArchive( codesource ) ) {
            return;
        }

        try {
            // save this lib
            String decoded = URLDecoder.decode( codesource, "UTF-8" );
            String filepath = decoded.substring( decoded.lastIndexOf(":") + 1);
            String parts[] = filepath.split( "!/" );
            String path = parts[0];
            File f = new File( path );
            Library lib = new Library( path );
            libraries.add( lib );

            // then add any libraries inside
            JarInputStream jis1 = new JarInputStream( new FileInputStream( f ) );
            lib.sha1 = hash( jis1, MessageDigest.getInstance("SHA1") );
            JarInputStream jis2 = new JarInputStream( new FileInputStream( f ) );
            lib.md5 = hash( jis2, MessageDigest.getInstance("MD5") );

            // scan for nested libraries
            JarInputStream jis3 = new JarInputStream( new FileInputStream( f ) );
            JarFile jarfile = new JarFile( f );
            scan( jarfile, jis3 );
        } catch( Exception e ) {
            Loggers.log( "The safelog4j project needs your help to deal with unusual CodeSources." );
            Loggers.log( "Report issue here: https://github.com/Contrast-Security-OSS/safelog4j/issues/new/choose" );
            Loggers.log( "Please include URL: " + codesource );
        }
        addedAll.add( codesource );
    }

    public static void scan( JarFile jarFile, JarInputStream jis ) throws Exception {
        JarEntry entry = null;
        while ((entry = jis.getNextJarEntry()) != null) {
            String nestedName = entry.getName();
            if ( isArchive( nestedName) ) {
                Library innerlib = new Library( nestedName );
                libraries.add( innerlib );

                InputStream nis1 = jarFile.getInputStream( entry );
                innerlib.md5 = hash( nis1, MessageDigest.getInstance("MD5") );

                InputStream nis2 = jarFile.getInputStream( entry );
                innerlib.sha1 = hash( nis2, MessageDigest.getInstance("SHA1") );
            
                InputStream nis3 = jarFile.getInputStream( entry );
                JarInputStream innerJis = new JarInputStream( nis3 );

                Manifest mf = innerJis.getManifest();
                if ( mf != null ) {
                    Attributes attr = mf.getMainAttributes();
                    String group = attr.getValue( "Implementation-Vendor-Id" );
                    String artifact = attr.getValue( "Implementation-Title" );
                    if ( group != null ) innerlib.group = group;
                    if ( artifact != null ) innerlib.artifact = artifact;
                }

                // scan through all individual nested jar file entries
                InputStream nis4 = jarFile.getInputStream( entry );
                JarInputStream innerJis4 = new JarInputStream( nis4 );
                while ((entry = innerJis4.getNextJarEntry()) != null) {
                    if ( entry.getName().endsWith( "/pom.xml" ) ) {
                        parsePom( innerJis4, innerlib );
                    }
                }
            }
        }
    }

    public static boolean isArchive( String filename ) {
        if ( filename.endsWith( "!/" ) ) {
            filename = filename.substring( 0, filename.length()-2 );
        }
        boolean isArchive = 
            filename.endsWith( ".jar" ) 
            || filename.endsWith( "war" )
            || filename.endsWith( "ear" )
            || filename.endsWith( "zip" );
        return isArchive;
    }

    private static void parsePom(JarInputStream is, Library lib) throws Exception {
        //String pom = getPOM( is );
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(is);
        String g = model.getGroupId();
        String a = model.getArtifactId();
        String v = model.getVersion();
        if ( g != null ) lib.group = g;
        if ( a != null ) lib.artifact = a;
        if ( v != null ) lib.version = v;
    }

    public static Collection<Library> getLibraries() {
        return libraries;
    }

    public static void dump() {
        for ( Library lib : libraries ) {
            Loggers.log( lib.toString() );
        }
    }

    public static String getPOM( InputStream is ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buf = new byte[8192];    
        while ((len = is.read(buf, 0, buf.length)) != -1) {
            baos.write(buf, 0, len);
        }
        return new String( baos.toByteArray(), "UTF-8" );
    }

    // streaming hash, low memory use
    public static String hash( InputStream is, MessageDigest md ) throws Exception {
        DigestInputStream dis = new DigestInputStream(is, md);
        byte[] buf = new byte[8192];
        for (int len; (len = dis.read(buf)) != -1;) {
        }
        return toHexString(md.digest());
    }

    public static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
    
