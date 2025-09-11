# Server for Amphi Apps

Amphi Apps Server is easy to set up, flexible, and you can run it on any platform.

## Setup

### 1. Download Server

```bash
curl -L https://github.com/amphi2024/server/releases/download/v{LATEST_VERSION}/server-{LATEST_VERSION}.jar -o server.jar
```

### 2. Download Java Runtime

If you already have Java installed, you can skip this step.
If not, you can download it from [Adoptium](https://adoptium.net/temurin/releases/?package=jre), [Azul](https://www.azul.com/downloads/?package=jre#zulu), or another provider.

### 3. Create a Service File (for Auto Start on Linux)

Create a file at /etc/systemd/system/your-service.service:

```ini
[Unit]
Description=Amphi Apps Server
After=network.target

[Service]
Type=simple
User=<YOUR_USER>
ExecStart=java -jar /path/to/server/server.jar # or path/to/jre/bin/java -jar /path/to/server/server.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Apply the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable amphi-server
sudo systemctl start amphi-server
```

### 4. (Optional) Make it Accessible Remotely

Advanced users can set up remote access using methods such as:
- VPN (e.g., Tailscale)
- Port forwarding
- Running the server as a system service

Make sure to consider security when enabling remote access.