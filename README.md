# Server for Apps

- [Setup](#setup)
- [Troubleshooting](#troubleshooting)

This is the server for all our apps. Follow the steps below to set it up.

## Setup

1. **Install Java**
    - Make sure to install Java (JDK 8 or higher).
    - You can download it from the official [Java website](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).

2. **Download the Server Jar File**
    - Download the server jar file from the following link: [https://amphi.site/archives/servers](https://amphi.site/archives/servers)

3. **Run the Server**
    - Use the following command to start the server:
      ```bash
      java -jar your-server-file.jar
      ```

4. **(Optional) Set Up HTTPS**
    - If you want to use HTTPS, you will need to install and configure a reverse proxy such as Nginx, Tomcat, or Kestrel.
    - You will also need to set up an SSL certificate and link it to your domain name.

## Troubleshooting

- If the server does not start, ensure that you have installed Java correctly and that the server file is in the correct directory.
- Common error: `java.lang.UnsupportedClassVersionError`. This usually means you're using an incompatible version of Java. Make sure you're using JDK 8 or higher.