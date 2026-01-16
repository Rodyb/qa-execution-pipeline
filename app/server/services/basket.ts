
// In-memory basket storage: sessionId -> product[]
const baskets = new Map();

export const basketService = {
  getBasket: (sessionId) => {
    return baskets.get(sessionId) || [];
  },
  addItem: (sessionId, product) => {
    const current = baskets.get(sessionId) || [];
    current.push(product);
    baskets.set(sessionId, current);
    return current;
  },
  clearBasket: (sessionId) => {
    baskets.delete(sessionId);
  }
};
