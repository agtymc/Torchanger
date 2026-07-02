Release 1.1 Ubuntu

Installation (.deb):
sudo dpkg -i torchanger_1.1_amd64.deb
sudo apt install torchanger_1.1_amd64.deb

Run JAR:
java -jar torchanger-1.1.jar

Dependencies

Torchanger requires the following system packages on Ubuntu:

  - tor
  - curl
  - obfs4proxy
  - snowflake-client
  - lyrebird
  - openjdk-21-jdk for running the JAR build

Install core dependencies:

  sudo apt update
  sudo apt install -y tor curl obfs4proxy snowflake-client openjdk-21-jdk

Install lyrebird manually:

  git clone https://gitlab.torproject.org/tpo/anti-censorship/pluggable-transports/lyrebird.git
  cd lyrebird
  go build -o lyrebird ./cmd/lyrebird
  sudo install -m 755 lyrebird /usr/bin/lyrebird

Verify installed binaries:

  which tor
  which curl
  which obfs4proxy
  which snowflake-client
  which lyrebird
  java -version
