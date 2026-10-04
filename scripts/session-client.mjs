// Test client: per-user cookies and CSRF are kept only in memory, never in reports.
export function sessionClient(origin) {
  const cookies = new Map();
  let csrf;
  let lastSetCookies = [];
  async function request(path, method = 'GET', body, { sendCsrf = true } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (cookies.size) headers.Cookie = [...cookies].map(([key, value]) => `${key}=${value}`).join('; ');
    if (sendCsrf && csrf && !['GET', 'HEAD'].includes(method)) headers[csrf.headerName] = csrf.token;
    const response = await fetch(origin + path, { method, headers, redirect: 'error', signal: AbortSignal.timeout(10000),
      ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
    lastSetCookies = response.headers.getSetCookie();
    for (const cookie of lastSetCookies) {
      const [name, ...parts] = cookie.split(';')[0].split('=');
      const value = parts.join('=');
      if (value) cookies.set(name, value); else cookies.delete(name);
    }
    return { status: response.status, body: response.status === 204 ? null : await response.json() };
  }
  async function refreshCsrf() {
    const result = await request('/auth/csrf');
    if (result.status !== 200) throw new Error(`CSRF bootstrap failed: ${result.status}`);
    csrf = result.body;
    return result;
  }
  async function login(email, password) {
    await refreshCsrf();
    const result = await request('/auth/login', 'POST', { email, password });
    if (result.status === 200) await refreshCsrf();
    return result;
  }
  return { request, refreshCsrf, login, cookie: () => cookies.get('JSESSIONID'), setCookies: () => [...lastSetCookies] };
}
