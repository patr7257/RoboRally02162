{
  description = "RoboRally backend unit tests (host + gateway)";

  inputs.nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";

  outputs = { self, nixpkgs, ... }:
  let
    systems = [ "x86_64-linux" "aarch64-darwin" ];
    forEachSystem = nixpkgs.lib.genAttrs systems;
  in {
    devShells = forEachSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk  = pkgs.openjdk23_headless;
      in {
        default = pkgs.mkShell {
          packages = [ jdk pkgs.maven ];
          MAVEN_OPTS = "-Xms256m -Xmx1g";
          shellHook = ''
            echo "Use: mvn -q -pl host,gateway -am -DskipITs test"
          '';
        };
      });

    apps = forEachSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk  = pkgs.openjdk23_headless;
        mvn  = pkgs.maven;
      in {
        test = {
          type = "app";
          program = (pkgs.writeShellApplication {
            name = "mvn-test";
            runtimeInputs = [ jdk mvn ];
            text = ''
              set -euo pipefail

              export JAVA_HOME=${jdk}
              export PATH=${jdk}/bin:${mvn}/bin:$PATH
              export MAVEN_OPTS="-Xms256m -Xmx1g"

              unset JAVA_HOME_17_X64 || true
              unset JAVA_HOME_11_X64 || true

              status=0
              mvn -B -q -f host/pom.xml -DskipITs test || status=1
              mvn -B -q -f gateway/pom.xml -DskipITs test || status=1
              exit $status
            '';
          }).outPath + "/bin/mvn-test";
        };
      });

  };
}
