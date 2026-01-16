
export const authService = {
  login: (username, password) => {
    if (username === 'user' && password === 'password') {
      return { success: true, token: 'fake-jwt-token' };
    }
    return { success: false, error: 'Invalid credentials' };
  }
};
