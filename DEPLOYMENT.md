# Deploying Tic-Tac-Toe to Render (Cloud Guide)

This guide walks you through deploying the unified Spring Boot application (backend API + responsive frontend) to **[Render](https://render.com/)** on their free tier.

Because the Spring Boot server already bundles the frontend static assets and dynamically binds to `$PORT`, you will receive a single public URL (e.g., `https://tictactoe-game.onrender.com`) that runs the entire game with zero cross-origin configuration.

---

## Prerequisites

1. A free account on **[Render.com](https://render.com/)**.
2. A free account on **[GitHub](https://github.com/)** (or GitLab).
3. This repository pushed to your GitHub account.

---

## Method 1: One-Click Blueprint Deployment (Recommended)

The repository includes a `render.yaml` configuration file that configures the entire service automatically.

1. Push this project to your GitHub repository:
   ```bash
   git add .
   git commit -m "Add Docker and Render deployment configuration"
   git push origin main
   ```
2. Log in to your **[Render Dashboard](https://dashboard.render.com/)**.
3. Click **New +** at the top right and select **Blueprint**.
4. Connect your GitHub repository.
5. Render will automatically detect `render.yaml` and display:
   - **Service Name**: `tictactoe-game`
   - **Runtime**: `Docker`
   - **Plan**: `Free`
6. Click **Apply**.
7. Render will build the container using the multi-stage `Dockerfile` and deploy your application. Once finished, your live URL will be shown at the top of the dashboard.

---

## Method 2: Manual Web Service Setup (Alternative)

If you prefer configuring the Web Service manually via Render's web interface:

1. Log in to **[Render Dashboard](https://dashboard.render.com/)**.
2. Click **New +** and select **Web Service**.
3. Choose **Build and deploy from a Git repository** and connect your repo.
4. Fill in the service details:
   - **Name**: `tictactoe-game` (or any unique name you like)
   - **Region**: Choose the region closest to you (e.g., Oregon, Frankfurt, Singapore)
   - **Branch**: `main`
   - **Runtime**: Select **Docker**
   - **Instance Type**: Select **Free**
5. Leave the Docker build context and Dockerfile path as defaults (it will automatically find the root `Dockerfile`).
6. Click **Deploy Web Service**.

---

## What Happens Behind the Scenes?

1. **Multi-Stage Build**:
   - The `Dockerfile` uses `maven:3.9-eclipse-temurin-21` to build and package `tictactoe-backend-0.0.1-SNAPSHOT.jar`.
   - The production container copies only the compiled JAR into a lightweight, secure `eclipse-temurin:21-jre-jammy` image running as a non-root system user.
2. **Dynamic Port Allocation**:
   - Render dynamically sets the `$PORT` environment variable (e.g. `PORT=10000`).
   - `application.properties` reads `server.port=${PORT:8080}`, automatically binding Spring Boot to Render's allocated port.
3. **Automatic API Routing**:
   - `app.js` detects that the application is running on a cloud domain (`*.onrender.com`) and automatically routes game moves to the relative path `/api/game/move`, eliminating CORS issues.

---

## Verifying Your Live Deployment

1. Click your Render service URL (e.g., `https://tictactoe-game.onrender.com/`).
2. The game should load immediately.
3. Play a game in **Easy** mode:
   - Verify computer places **O** in response.
   - Verify scoreboard updates on game end.
4. Switch to **Medium** mode:
   - Verify board resets for a new round while preserving the score.
   - Verify computer blocks player wins and seizes its own winning moves.
5. Click **New Game**:
   - Board resets and is ready for the next round.

> **Note on Free Tier Inactivity**: Free services on Render spin down after 15 minutes of inactivity. The first request after a sleep period may take ~30–50 seconds to wake up. Subsequent turns will be instantaneous.
