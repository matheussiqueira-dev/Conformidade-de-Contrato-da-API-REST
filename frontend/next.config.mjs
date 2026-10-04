const backend = new URL(process.env.BACKEND_ORIGIN ?? 'http://127.0.0.1:8080');
if (!['http:', 'https:'].includes(backend.protocol) || backend.username || backend.password || backend.pathname !== '/') {
  throw new Error('BACKEND_ORIGIN must be an HTTP(S) origin without credentials or path.');
}
export default {
  poweredByHeader: false,
  async rewrites() {
    return [{ source: '/api/backend/:path*', destination: `${backend.origin}/:path*` }];
  },
};
