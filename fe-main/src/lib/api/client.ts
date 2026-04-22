import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/main',
  timeout: 3000000,

  withCredentials: true,
});

// Log full request URL and payloads
api.interceptors.request.use((config) => {
  const base = (config.baseURL || '').replace(/\/$/, '');
  const path = `${config.url || ''}`.startsWith('/') ? `${config.url}` : `/${config.url}`;
  const fullUrl = `${base}${path}`;
  const method = (config.method || 'get').toUpperCase();
  // eslint-disable-next-line no-console
  console.log(`➡️ [API Request] ${method} ${fullUrl}`);
  if (config.params) {
    // eslint-disable-next-line no-console
    console.log('   ↳ params:', config.params);
  }
  if (config.data) {
    // eslint-disable-next-line no-console
    console.log('   ↳ body:', config.data);
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const base = (response.config.baseURL || '').replace(/\/$/, '');
    const path = `${response.config.url || ''}`.startsWith('/')
      ? `${response.config.url}`
      : `/${response.config.url}`;
    const fullUrl = `${base}${path}`;
    // eslint-disable-next-line no-console
    console.log(`✅ [API Response] ${response.status} ${fullUrl}`);
    return response;
  },
  (error) => {
    const cfg = error.config || {};
    const base = (cfg.baseURL || '').replace(/\/$/, '');
    const path = `${cfg.url || ''}`.startsWith('/') ? `${cfg.url}` : `/${cfg.url}`;
    const fullUrl = `${base}${path}`;
    // eslint-disable-next-line no-console
    console.error(`❌ [API Error] ${cfg.method ? cfg.method.toUpperCase() : ''} ${fullUrl}`, error);
    return Promise.reject(error);
  },
);

export default api;
