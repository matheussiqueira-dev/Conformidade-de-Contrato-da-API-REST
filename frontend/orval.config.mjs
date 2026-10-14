// Contrato v4 -> schemas Zod 4 em lib/api/generated (WP-3.2). Gerado; nao editar a mao.
export default {
  sbi: {
    input: { target: '../config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json' },
    output: {
      client: 'zod',
      mode: 'single',
      target: './lib/api/generated/sbi.zod.ts',
      clean: true,
      override: { zod: { version: 4, strict: { response: true, body: true, query: true, param: true, header: true } } },
    },
  },
};
