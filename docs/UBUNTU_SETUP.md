# Ubuntu Setup Guide: Jenkins + Docker CI/CD for the Calculator API

Follow these steps in order on your Ubuntu machine (22.04 / 24.04, VM or bare metal).
Lines starting with `$` are commands to run; drop the `$`.

Values used throughout (change them if yours differ):

| Thing              | Value                                                      |
|--------------------|------------------------------------------------------------|
| GitHub repo        | `https://github.com/Hitanshu078/SPE_Mini_Project.git`      |
| Docker Hub user    | `hitanshu078`                                              |
| Docker Hub image   | `hitanshu078/spe-calculator`                               |
| Notification email | `hitanshu078@gmail.com` (set in `Jenkinsfile`, `NOTIFY_EMAIL`) |
| Jenkins UI         | `http://localhost:8080`                                    |
| Deployed API       | `http://localhost:8081` (Nginx -> 2 calculator replicas)   |

> **Using WSL instead of a VM?** Enable systemd first: put
> `[boot]` / `systemd=true` in `/etc/wsl.conf`, run `wsl --shutdown` from Windows,
> reopen Ubuntu. Do **not** use Docker Desktop's WSL integration *and* the apt Docker
> at the same time; pick the apt install below.

---

## Step 0: Base packages

```bash
$ sudo apt update && sudo apt upgrade -y
$ sudo apt install -y git curl wget ca-certificates gnupg fontconfig openjdk-21-jdk
$ java -version          # should print 21.x
```

JDK 21 runs Jenkins and also builds the project (the code targets Java 17, which JDK 21 compiles fine).

---

## Step 1: Install Docker Engine + Compose v2

```bash
$ sudo install -m 0755 -d /etc/apt/keyrings
$ sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
$ sudo chmod a+r /etc/apt/keyrings/docker.asc
$ echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
$ sudo apt update
$ sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
$ sudo systemctl enable --now docker
```

Let your own user run docker without sudo:

```bash
$ sudo usermod -aG docker $USER
$ newgrp docker
$ docker run --rm hello-world     # must print "Hello from Docker!"
$ docker compose version          # must print v2.x
```

---

## Step 2: Install Jenkins

```bash
$ sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc \
    https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
$ echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
$ sudo apt update
$ sudo apt install -y jenkins
```

> If `apt update` complains about `NO_PUBKEY`, Jenkins has rotated its signing key.
> Open https://pkg.jenkins.io/debian-stable/ and use the key URL shown there.

Give Jenkins access to Docker, start it on boot:

```bash
$ sudo usermod -aG docker jenkins
$ sudo systemctl enable jenkins
$ sudo systemctl restart jenkins
$ sudo -u jenkins docker ps       # must NOT say "permission denied"
```

Get the unlock password:

```bash
$ sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

---

## Step 3: First-time Jenkins UI setup

1. Open `http://localhost:8080`, paste the password.
2. Click **Install suggested plugins** and wait.
3. Create your admin user. Keep the Jenkins URL as `http://localhost:8080/`.
4. **Manage Jenkins -> Plugins -> Available plugins**, make sure these are installed
   (most come with the suggested set): **Pipeline**, **Git**, **GitHub**, **JUnit**,
   **Email Extension**, **Credentials Binding**, **Pipeline: Stage View**.
   Restart Jenkins if asked.

---

## Step 4: Docker Hub credentials

1. Log in at https://hub.docker.com -> **Account Settings -> Personal access tokens ->
   Generate new token**. Name: `jenkins`, permissions: **Read & Write**. Copy the token.
2. Jenkins -> **Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials**:
   - Kind: **Username with password**
   - Username: `hitanshu078`
   - Password: *the access token* (not your Docker Hub password)
   - ID: `dockerhub-creds` (must be exactly this, the Jenkinsfile uses it)
   - Click **Create**.

---

## Step 5: Gmail email notifications

1. In your Google account, turn on **2-Step Verification**, then go to
   https://myaccount.google.com/apppasswords and create an App Password (name: `jenkins`).
   Copy the 16-character password.
2. Jenkins -> **Manage Jenkins -> System**:
   - **Jenkins Location -> System Admin e-mail address:** `hitanshu078@gmail.com`
     (Gmail rejects mail whose sender differs from the login account.)
   - **Extended E-mail Notification:**
     - SMTP server: `smtp.gmail.com`, SMTP Port: `465`
     - Advanced -> Credentials -> **Add -> Jenkins**: kind *Username with password*,
       username `hitanshu078@gmail.com`, password = the App Password. Select it.
     - Tick **Use SSL**.
     - Default Content Type: `text/plain`
   - **E-mail Notification** (the older mailer, used for the test button):
     - SMTP server: `smtp.gmail.com`
     - Advanced: tick **Use SMTP Authentication**, username `hitanshu078@gmail.com`,
       password = App Password, tick **Use SSL**, SMTP Port `465`
     - Tick **Test configuration by sending test e-mail**, enter your address, click
       **Test configuration**. You must receive the mail.
3. Click **Save**.

---

## Step 6: Create the pipeline job

1. Jenkins -> **New Item** -> name `SPE_Mini_Project` -> **Pipeline** -> OK.
2. Under **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/Hitanshu078/SPE_Mini_Project.git`
     (public repo, so no credentials needed)
   - Branch Specifier: `*/main`
   - Script Path: `Jenkinsfile`
3. **Save**, then click **Build Now**.

The first build is slow (it downloads Maven, dependencies and base images). After it
finishes, Jenkins has read the `triggers {}` block in the Jenkinsfile, so **Poll SCM and the
GitHub hook trigger are now active**. You don't need to tick them by hand.

### Check the result

```bash
$ docker compose -p spe-calculator ps            # 3 containers, all "healthy"
$ curl "http://localhost:8081/api/v1/add?a=2&b=3"
$ curl -i "http://localhost:8081/api/v1/divide?a=5&b=0"     # HTTP 400 + error JSON
$ curl "http://localhost:8081/actuator/health"
$ for i in 1 2 3 4; do curl -si "http://localhost:8081/api/v1/add?a=1&b=1" | grep X-Upstream; done
```

The last command shows two different upstream IPs, which proves Nginx load-balances over 2 replicas.

On Docker Hub, open `hitanshu078/spe-calculator`, check **Settings -> Visibility is Public**,
and check that the `latest` and `<build-number>` tags are there.

---

## Step 7: GitHub webhook (auto-trigger on push)

Poll SCM (every ~2 min) already works. A webhook makes builds start within seconds.
GitHub can't reach `localhost`, so expose Jenkins with ngrok:

```bash
$ sudo snap install ngrok
$ ngrok config add-authtoken <your-token-from-dashboard.ngrok.com>
$ ngrok http 8080
```

Your free ngrok account includes one static domain (Dashboard -> Domains). Using it keeps
the URL the same across restarts: `ngrok http --url=<your-domain>.ngrok-free.app 8080`.

Copy the `https://....ngrok-free.app` URL, then on GitHub go to **repo -> Settings -> Webhooks
-> Add webhook**:
- Payload URL: `https://<ngrok-domain>/github-webhook/` (the trailing slash matters)
- Content type: `application/json`
- Events: **Just the push event**
- **Add webhook**: the delivery should show a green tick.

Keep the `ngrok` terminal open whenever you want webhooks to work (e.g. during the demo).

---

## Step 8: Test the full auto-deploy loop

From any machine with the repo:

```bash
$ git checkout -b feature/trigger-test
# make a small visible change, e.g. edit README.md
$ git commit -am "Test webhook-triggered build"
$ git push -u origin feature/trigger-test
```

On GitHub: **open a Pull Request -> Merge**. Within seconds Jenkins starts a build on `main`,
runs all stages, deploys, and you get a SUCCESS email.

Optional: to get the **FAILURE email** for your screenshot, break a test on a branch,
merge, screenshot the email, then revert with a second PR.

---

## Step 9: Screenshots for the report

- [ ] Jenkins job page with **Stage View** all green
- [ ] Jenkins **Test Result** page (JUnit tests passed)
- [ ] **SUCCESS email** in Gmail (plus FAILURE email if you did it)
- [ ] `docker compose -p spe-calculator ps` showing 3 healthy containers (or `docker ps`)
- [ ] `curl` outputs: add, divide, divide-by-zero 400, health
- [ ] Docker Hub repo page showing the tags
- [ ] GitHub webhook "Recent Deliveries" with a green tick
- [ ] GitHub network graph / merged Pull Requests

---

## Step 10: Live demo checklist (before class)

1. `sudo systemctl status jenkins docker`: both running.
2. `ngrok http --url=<your-domain> 8080` running; webhook delivery green.
3. `docker compose -p spe-calculator ps`: stack healthy.
4. Have a small change ready on a branch. Push it, merge the PR, show the pipeline running
   live, then `curl` the API to show the change is deployed and that the email arrived.

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `permission denied ... /var/run/docker.sock` | `sudo usermod -aG docker jenkins && sudo systemctl restart jenkins` |
| `./mvnw: Permission denied` | Shouldn't happen (executable bit is committed). If it does: `git update-index --chmod=+x mvnw`, commit, push |
| `/usr/bin/env: 'sh\r'` | CRLF line endings; `.gitattributes` forces LF, re-clone the workspace: job -> **Workspace -> Wipe Out Current Workspace** |
| `docker compose: unknown command` / `--wait` not supported | Old `docker-compose` v1; install `docker-compose-plugin` (Step 1) |
| `Bind for 0.0.0.0:8081 failed: port is already allocated` | `sudo ss -ltnp \| grep 8081`, stop that process/container |
| Push stage `unauthorized` | Re-create the token with Read & Write; check credential ID is `dockerhub-creds` |
| No email / `535 Authentication failed` | Use the Gmail **App Password**, not your normal password; sender address = Gmail login |
| `No such DSL method 'emailext'` | Install the **Email Extension** plugin, restart Jenkins |
| Webhook delivery shows 403/404 | URL must end with `/github-webhook/`; ngrok must be running; job must have run once |
| Pushes don't trigger anything | Run the job manually once so Jenkins registers the triggers |
| Verify stage times out | `docker compose -p spe-calculator logs calculator` to see why the app didn't start |
| Disk filling up | `docker system prune -af` (removes unused images; the next build re-downloads them) |
