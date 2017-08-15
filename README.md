# tix-time-client
TiX Time Client is the application that runs on the client. Its task is to send packages to the server and report the
full statistics from it.

How to run the client:
* GUI mode from the command line by using gradle jfxRun
* GUI mode by running java -jar tix-client.jar
* CLI mode by using java -jar tix-client.jar username password installation port

How to generate executable jar file:
* Run gradle jfxJar and look for tix-client.jar under build/jfx/app dir
