
import type { Express } from "express";
import { type Server } from "http";
import { authService } from "./services/auth";
import { basketService } from "./services/basket";
import { ordersService } from "./services/orders";
import { setupStaticFrontend } from "./static";

export async function registerRoutes(httpServer: Server, app: Express): Promise<Server> {
  // Move static setup to the end of the API routes or handle it differently
  // API Routes
  app.post('/api/login', (req, res) => {
    const { username, password } = req.body;
    const result = authService.login(username, password);
    if (result.success) {
        // Simple session simulation using a cookie or just returning success
        // For this demo, we rely on the client storing the 'user'
        // But let's set a simple cookie for the basket service
        res.cookie('sessionId', username, { httpOnly: true }); 
        res.json(result);
    } else {
        res.status(401).json(result);
    }
  });

  app.get('/api/products', (_req, res) => {
    res.json([
        { id: 1, name: "Premium Wireless Headphones", price: 129.99, image: "/images/modern_sleek_wireles_14243e2f.jpg" },
        { id: 2, name: "Smart Fitness Watch", price: 79.50, image: "/images/minimalist_smart_wat_80be4e20.jpg" },
        { id: 3, name: "Portable Bluetooth Speaker", price: 45.00, image: "/images/modern_cylinder_shap_79985bd2.jpg" }
    ]);
  });

  app.post('/api/basket', (req, res) => {
    const sessionId = req.cookies?.sessionId || 'anonymous';
    const { productId } = req.body;
    const basket = basketService.addItem(sessionId, productId);
    res.json({ success: true, basket });
  });

  app.get('/api/basket-items', (req, res) => {
    const sessionId = req.cookies?.sessionId || 'anonymous';
    const basket = basketService.getBasket(sessionId);
    res.json(basket);
  });

  app.post('/api/order', (req, res) => {
    const { username } = req.body;
    const sessionId = req.cookies?.sessionId || username || 'anonymous';
    const items = basketService.getBasket(sessionId);
    
    const order = ordersService.createOrder(username, items);
    basketService.clearBasket(sessionId);
    
    res.json({ success: true, order });
  });

  app.get('/api/order/history', (req, res) => {
    const username = req.query.username as string;
    const history = ordersService.getHistory(username);
    res.json(history);
  });

  return httpServer;
}
