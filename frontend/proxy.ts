// Next.js 16: sem cookie de sessao, qualquer tela interna vai para /entrar?next=... (WP-3.4).
// A checagem de area acontece no layout de cada area, com /auth/me; a API e a autoridade final.
import { NextResponse, type NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  if (request.cookies.has('JSESSIONID')) return NextResponse.next();
  const login = new URL('/entrar', request.url);
  const next = request.nextUrl.pathname + request.nextUrl.search;
  if (next !== '/') login.searchParams.set('next', next);
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ['/((?!entrar|api/backend|_next/static|_next/image|favicon.ico|icon.svg|.*\\.[a-z0-9]+$).*)'],
};
