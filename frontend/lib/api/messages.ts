// ErrorResponse.code -> mensagem em pt-BR. A tela nunca mostra codigo HTTP (WP-3.3, voz do SBI v1.1).
export const ERROR_MESSAGES: Readonly<Record<string, string>> = {
  VALIDATION_FAILED: 'Revise os campos destacados e tente de novo.',
  MALFORMED_REQUEST: 'Não conseguimos ler os dados enviados. Atualize a página e tente de novo.',
  UNAUTHENTICATED: 'Sua sessão terminou. Entre novamente para continuar.',
  FORBIDDEN: 'Sem acesso a esta área.',
  NOT_FOUND: 'Não encontramos este registro.',
  METHOD_NOT_ALLOWED: 'Esta operação não está disponível.',
  UNSUPPORTED_MEDIA_TYPE: 'Esta operação não está disponível.',
  STOCK_INSUFFICIENT: 'Estoque insuficiente para concluir.',
  IDEMPOTENCY_CONFLICT: 'Esta venda já foi enviada com outros dados. Revise o carrinho.',
  STATE_CONFLICT: 'Não é possível fazer isso na situação atual do registro.',
  DUPLICATE_DOCUMENT: 'Já existe um cliente com este documento.',
  RESOURCE_IN_USE: 'Este registro está em uso e não pode ser excluído.',
  CONCURRENT_UPDATE: 'Os dados mudaram desde que você abriu a tela. Recarregue e tente de novo.',
  SESSION_REVOKED: 'Sua sessão foi encerrada porque o seu acesso mudou. Entre novamente.',
  LAST_MANAGER: 'A loja precisa de pelo menos um gerente ativo.',
  SELF_CHANGE_FORBIDDEN: 'Você não pode alterar o próprio papel, a própria situação nem redefinir a própria senha por aqui.',
  PASSWORD_CHANGE_REQUIRED: 'Troque a senha provisória para continuar.',
  DISCOUNT_ABOVE_LIMIT: 'Desconto acima do seu limite. Peça ao gerente para aprovar no pedido.',
  BELOW_COST: 'Com esse desconto, um dos itens sairia abaixo do custo. Reduza o desconto ou fale com o gerente.',
  INTERNAL_ERROR: 'Algo deu errado do nosso lado. Tente novamente.',
};

export const NETWORK_MESSAGE = 'Sem conexão com o sistema. Verifique a rede e tente novamente.';
/** 502, 503 e 504 sem ErrorResponse: o servidor esta subindo ou fora do ar, nao e erro do que foi digitado. */
export const STARTING_MESSAGE = 'O sistema está iniciando ou indisponível no momento. Aguarde um minuto e tente de novo.';
/** Espera longa na primeira chamada do dia no ambiente publicado (o servidor gratuito dorme sem acesso). */
export const SLOW_START_HINT = 'O servidor pode levar até 3 minutos para acordar na primeira entrada. Aguarde nesta página.';
export const LOGIN_FAILED_MESSAGE = 'E-mail ou senha inválidos.';
export const LOCKOUT_HINT = 'Após 5 tentativas incorretas, o acesso fica bloqueado por 15 minutos.';
export const WRONG_CURRENT_PASSWORD = 'Senha atual incorreta.';

export function messageFor(code: string | undefined, status: number): string {
  if (code && ERROR_MESSAGES[code]) return ERROR_MESSAGES[code];
  if (status === 401) return ERROR_MESSAGES.UNAUTHENTICATED ?? NETWORK_MESSAGE;
  if (status === 403) return ERROR_MESSAGES.FORBIDDEN ?? NETWORK_MESSAGE;
  if (status === 404) return ERROR_MESSAGES.NOT_FOUND ?? NETWORK_MESSAGE;
  if (status === 502 || status === 503 || status === 504) return STARTING_MESSAGE;
  return ERROR_MESSAGES.INTERNAL_ERROR ?? NETWORK_MESSAGE;
}
