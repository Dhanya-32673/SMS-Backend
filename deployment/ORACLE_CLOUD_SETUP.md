# 🌐 Oracle Cloud Always Free VM Deployment Guide — SICMS Backend

This document guides you through setting up an **Oracle Cloud Always Free VM (Ubuntu 22.04 LTS / Ampere A1)** to host the Spring Boot backend with **Systemd**, **Nginx Reverse Proxy**, and **Let's Encrypt SSL**.

---

## 📋 Step 1: Oracle Cloud Ingress & Firewall Configuration

1. In the **Oracle Cloud Console**, go to **Networking** -> **Virtual Cloud Networks** -> Select your VCN.
2. Under **Security Lists**, click **Default Security List**.
3. Add **Ingress Rules**:
   - **HTTP**: Source `0.0.0.0/0`, IP Protocol `TCP`, Destination Port `80`
   - **HTTPS**: Source `0.0.0.0/0`, IP Protocol `TCP`, Destination Port `443`

4. SSH into your Oracle Cloud VM and open the OS-level firewall ports:
   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```

---

## ☕ Step 2: Install Java 21 & Nginx

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-21-jre-headless nginx certbot python3-certbot-nginx
```

Verify Java installation:
```bash
java -version
```

---

## ⚙️ Step 3: Application Directory & Systemd Service

1. Create application directory:
   ```bash
   sudo mkdir -p /opt/sicms-backend
   sudo chown -R ubuntu:ubuntu /opt/sicms-backend
   ```

2. Upload your compiled `app.jar` to `/opt/sicms-backend/app.jar`.

3. Create the `.env` configuration file:
   ```bash
   nano /opt/sicms-backend/.env
   ```
   Paste environment variables:
   ```env
   SERVER_PORT=8080
   DB_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.ookzjdmkoaunbrufvmvq
   DB_PASSWORD=YourPassword
   JWT_SECRET=c2ljbXNfc3VwZXJfc2VjcmV0X2tleV9mb3Jfand0X2F1dGhlbnRpY2F0aW9uXzIwMjZfc2VjdXJlX2tleQ==
   SUPABASE_URL=https://ookzjdmkoaunbrufvmvq.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=YourServiceRoleKey
   ```

4. Install the Systemd Service:
   ```bash
   sudo cp deployment/sicms-backend.service /etc/systemd/system/sicms-backend.service
   sudo systemctl daemon-reload
   sudo systemctl enable sicms-backend
   sudo systemctl start sicms-backend
   ```

5. Verify service logs:
   ```bash
   sudo journalctl -u sicms-backend -f
   ```

---

## 🔒 Step 4: Nginx Reverse Proxy & SSL (Let's Encrypt)

1. Create Nginx site configuration:
   ```bash
   sudo nano /etc/nginx/sites-available/sicms
   ```
   Paste the contents of `deployment/nginx.conf`.

2. Enable the site and test configuration:
   ```bash
   sudo ln -s /etc/nginx/sites-available/sicms /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl reload nginx
   ```

3. Obtain Let's Encrypt SSL Certificate:
   ```bash
   sudo certbot --nginx -d api.sicms.yourdomain.com
   ```

---

## ✅ Step 5: Verification

Verify that your API responds cleanly over HTTPS:
```bash
curl -i https://api.sicms.yourdomain.com/api/auth/login
```
Your Spring Boot API is now fully operational, secure, and production-ready on Oracle Cloud VM!
