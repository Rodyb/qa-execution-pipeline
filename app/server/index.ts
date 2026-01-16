import express, { type Request, Response, NextFunction } from "express";
import cookieParser from "cookie-parser";
import { registerRoutes } from "./routes";
import { createServer } from "http";
import path, { dirname } from "path";
import { fileURLToPath } from "url";

// Support both ESM during dev and CJS bundle in production
const resolvedFilename =
  typeof __filename !== "undefined" ? __filename : fileURLToPath(import.meta.url);
const resolvedDirname = typeof __dirname !== "undefined" ? __dirname : dirname(resolvedFilename);

const app = express();
app.use(cookieParser());
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

export function log(message: string, source = "express") {
  const formattedTime = new Date().toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  });
  console.log(`${formattedTime} [${source}] ${message}`);
}

(async () => {
  const httpServer = createServer(app);
  await registerRoutes(httpServer, app);

  const publicPath = path.resolve(resolvedDirname, "..", "client", "public");
  app.use(express.static(publicPath));

  // Handle routes for convenience
  app.get("/", (_req, res) => res.sendFile(path.join(publicPath, "login.html")));
  app.get("/login", (_req, res) => res.sendFile(path.join(publicPath, "login.html")));
  app.get("/shop", (_req, res) => res.sendFile(path.join(publicPath, "shop.html")));
  app.get("/confirmation", (_req, res) => res.sendFile(path.join(publicPath, "confirmation.html")));
  app.get("/profile", (_req, res) => res.sendFile(path.join(publicPath, "profile.html")));
  app.get("/help", (_req, res) => res.sendFile(path.join(publicPath, "help.html")));
  app.get("/basket-confirmation", (_req, res) => res.sendFile(path.join(publicPath, "basket-confirmation.html")));
  app.get("/orders", (_req, res) => res.sendFile(path.join(publicPath, "orders.html")));
  app.get("/order-details", (_req, res) => res.sendFile(path.join(publicPath, "order-details.html")));
  app.get("/forgot-password", (_req, res) => res.sendFile(path.join(publicPath, "forgot-password.html")));
  app.get("/search", (_req, res) => res.sendFile(path.join(publicPath, "search.html")));
  app.get("/search-results", (_req, res) => res.sendFile(path.join(publicPath, "search-results.html")));

  app.use((err: any, _req: Request, res: Response, _next: NextFunction) => {
    const status = err.status || err.statusCode || 500;
    const message = err.message || "Internal Server Error";
    res.status(status).json({ message });
  });

  const port = parseInt(process.env.PORT || "5000", 10);
  httpServer.listen(port, "0.0.0.0", () => {
    log(`serving on port ${port}`);
  });
})();
