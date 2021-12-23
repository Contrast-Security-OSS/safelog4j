package com.contrastsecurity;

public class Library {
    public String filename = null;
    public String name = "TBD";
    public String group = null;
    public String artifact = null;
    public String version = null;
    public String jar = null;
    public String path = null;
    public String md5 = null;
    public String sha1 = null;

    public Library( String fullpath ) {
        jar = fullpath.substring( fullpath.lastIndexOf("/") + 1 );
        path = fullpath.substring( 0, fullpath.lastIndexOf("/") );

        String fqn = fullpath.substring( 0, fullpath.lastIndexOf( ".jar" ) );
        name = fqn.substring( fqn.lastIndexOf( "/" ) + 1 );
        version = fqn.substring( fqn.lastIndexOf( "-" ) + 1 );
    }

    public String toString() {
        return "Library"
        + "\n    name     | " + name
        + "\n    group    | " + group
        + "\n    artifact | " + artifact
        + "\n    version  | " + version
        + "\n    jar      | " + jar
        + "\n    path     | " + path 
        + "\n    MD5      | " + md5 
        + "\n    SHA1     | " + sha1
        + "\n    maven    | " + "https://search.maven.org/search?q=1:" +sha1;
    }

}
