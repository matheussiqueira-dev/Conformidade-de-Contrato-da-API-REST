// Formularios validados pelos schemas Zod gerados do contrato v4 (WP-3.2): o formulario monta o corpo da
// requisicao e o mesmo schema que valida a API valida a tela. Mensagens em pt-BR.
import type { FieldError, FieldErrors, FieldValues, Resolver } from 'react-hook-form';
import type { ZodType, core } from 'zod';

export function issueMessage(issue: core.$ZodIssue): string {
  switch (issue.code) {
    case 'too_small':
      return issue.origin === 'string' && issue.minimum === 1 ? 'Preencha este campo.' : `Mínimo: ${String(issue.minimum)}.`;
    case 'too_big':
      return `Máximo: ${String(issue.maximum)}.`;
    case 'invalid_format':
      return issue.format === 'email' ? 'Informe um e-mail válido.' : 'Formato inválido.';
    case 'invalid_type':
      return 'Preencha este campo.';
    case 'invalid_union':
    case 'invalid_value':
      return 'Escolha uma opção válida.';
    default:
      return 'Valor inválido.';
  }
}

function setPath(target: Record<string, unknown>, path: readonly PropertyKey[], error: FieldError) {
  let node = target;
  path.forEach((key, index) => {
    const name = String(key);
    if (index === path.length - 1) {
      if (!node[name]) node[name] = error;
      return;
    }
    node[name] = (node[name] as Record<string, unknown> | undefined) ?? {};
    node = node[name] as Record<string, unknown>;
  });
}

export type ExtraCheck<T> = (values: T) => Record<string, string>;

/** Resolver do react-hook-form: valida o corpo montado com o schema do contrato e checagens extras de tela. */
export function contractResolver<TForm extends FieldValues>(schema: ZodType, toBody: (values: TForm) => unknown,
                                                            extra?: ExtraCheck<TForm>): Resolver<TForm> {
  return async values => {
    const errors: Record<string, unknown> = {};
    const result = schema.safeParse(toBody(values));
    if (!result.success) {
      for (const issue of result.error.issues) {
        if (issue.path.length) setPath(errors, issue.path, { type: issue.code, message: issueMessage(issue) });
      }
    }
    for (const [field, message] of Object.entries(extra?.(values) ?? {})) setPath(errors, field.split('.'), { type: 'custom', message });
    return Object.keys(errors).length
      ? { values: {}, errors: errors as FieldErrors<TForm> }
      : { values, errors: {} };
  };
}

/** Texto vazio vira ausente (campo opcional do contrato). */
export function optional(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}
