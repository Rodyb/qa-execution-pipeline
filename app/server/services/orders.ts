
// In-memory orders storage
const orders = [];

export const ordersService = {
  createOrder: (username, items) => {
    const order = {
      id: Math.floor(Math.random() * 10000),
      username,
      items,
      status: 'CONFIRMED', // Hardcoded as requested
      date: new Date().toISOString()
    };
    orders.push(order);
    return order;
  },
  getHistory: (username) => {
    return orders.filter(o => o.username === username);
  }
};
