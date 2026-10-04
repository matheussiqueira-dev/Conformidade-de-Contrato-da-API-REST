export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
export function authClient(fetcher = fetch) {
  async function request(path, { method = 'GET', body, token, signal } = {}) {
    const response = await fetcher(`/api/backend${path}`, {
      method, credentials: 'same-origin', cache: 'no-store', signal,
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { [token.headerName]: token.token } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    if (!response.ok) throw new ApiError(response.status,
      response.status === 401 ? 'E-mail ou senha inválidos, ou sessão expirada.' :
      response.status === 403 ? 'O acesso foi recusado. Atualize a página e tente novamente.' : 'Não foi possível concluir. Tente novamente.');
    return response.status === 204 ? null : response.json();
  }
  return {
    me: signal => request('/auth/me', { signal }),
    async login(email, password) {
      const token = await request('/auth/csrf');
      const result = await request('/auth/login', { method: 'POST', body: { email, password }, token });
      // Authentication invalidates the pre-login CSRF token.
      await request('/auth/csrf');
      return result.user;
    },
    async logout() {
      const token = await request('/auth/csrf');
      await request('/auth/logout', { method: 'POST', token });
    },
  };
}
