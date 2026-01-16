import express, { type Express } from "express";
import fs from "fs";
import path, { dirname } from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export function serveStatic(app: Express) {
  const distPath = path.resolve(__dirname, "public");
  if (!fs.existsSync(distPath)) {
    throw new Error(
      `Could not find the build directory: ${distPath}, make sure to build the client first`,
    );
  }

  app.use(express.static(distPath));

  // fall through to index.html if the file doesn't exist
  app.use("*", (_req, res) => {
    res.sendFile(path.resolve(distPath, "index.html"));
  });
}

export function setupStaticFrontend(app: Express) {
  const publicPath = path.resolve(__dirname, "..", "client", "public");
  app.use(express.static(publicPath));
  
  // Also serve individual html files without the extension for convenience
  app.get("/login", (_req, res) => res.sendFile(path.join(publicPath, "login.html")));
  app.get("/shop", (_req, res) => res.sendFile(path.join(publicPath, "shop.html")));
  app.get("/confirmation", (_req, res) => res.sendFile(path.join(publicPath, "confirmation.html")));
}
