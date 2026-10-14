'use client';
import type { ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import type { z } from 'zod';
import { CreateCustomerBody, CreateCustomerResponse } from '../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../lib/api/http.ts';
import { contractResolver, optional } from '../lib/forms.ts';
import { cnpjKey, isValidCnpj, isValidCpf } from '../lib/documents.ts';
import { errorMessage } from './feedback.tsx';

export type CustomerDetail = z.infer<typeof CreateCustomerResponse>;

interface CustomerFormValues {
  type: 'INDIVIDUAL' | 'CORPORATE';
  name: string;
  companyName: string;
  cpf: string;
  cnpj: string;
  email: string;
  phone: string;
  birthDate: string;
}

function toBody(values: CustomerFormValues): unknown {
  const common = { name: values.name.trim(), email: values.email.trim(), phone: values.phone.trim() };
  if (values.type === 'CORPORATE') {
    return { type: 'CORPORATE', ...common, companyName: values.companyName.trim(), cnpj: cnpjKey(values.cnpj) };
  }
  const birthDate = optional(values.birthDate);
  return { type: 'INDIVIDUAL', ...common, cpf: values.cpf.trim(), ...(birthDate ? { birthDate } : {}) };
}

function documentCheck(values: CustomerFormValues): Record<string, string> {
  if (values.type === 'INDIVIDUAL' && values.cpf && !isValidCpf(values.cpf)) return { cpf: 'CPF inválido.' };
  if (values.type === 'CORPORATE' && values.cnpj && !isValidCnpj(values.cnpj)) return { cnpj: 'CNPJ inválido.' };
  return {};
}

/** Cadastro de cliente PF/PJ (DEC-ERP-05): usado no painel lateral do PDV e em /clientes. */
export default function CustomerForm({ onCreated, onCancel, idPrefix = 'customer' }: {
  onCreated: (customer: CustomerDetail) => void; onCancel?: () => void; idPrefix?: string;
}) {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<CustomerFormValues>({
    defaultValues: { type: 'INDIVIDUAL', name: '', companyName: '', cpf: '', cnpj: '', email: '', phone: '', birthDate: '' },
    resolver: contractResolver<CustomerFormValues>(CreateCustomerBody, toBody, documentCheck),
  });
  const type = watch('type');
  const create = useMutation({
    mutationFn: (values: CustomerFormValues) =>
      api.request('/customers', { method: 'POST', body: toBody(values), schema: CreateCustomerResponse }),
    onSuccess: onCreated,
  });
  const fieldError = (name: keyof CustomerFormValues) => errors[name]?.message;
  const id = (name: keyof CustomerFormValues) => `${idPrefix}-${name}`;
  const described = (name: keyof CustomerFormValues) => (errors[name] ? { 'aria-invalid': true, 'aria-describedby': `${id(name)}-error` } : {});

  return (
    <form className="stack" onSubmit={handleSubmit(values => create.mutate(values))} noValidate aria-busy={create.isPending}>
      <fieldset className="segmented" aria-label="Tipo de cliente">
        <legend className="sr-only">Tipo de cliente</legend>
        <label><input type="radio" value="INDIVIDUAL" {...register('type')} /><span>Pessoa física</span></label>
        <label><input type="radio" value="CORPORATE" {...register('type')} /><span>Pessoa jurídica</span></label>
      </fieldset>
      <div className="form-grid">
        <Field id={id('name')} label={type === 'CORPORATE' ? 'Nome fantasia' : 'Nome'} error={fieldError('name')}>
          <input className="input" id={id('name')} autoComplete="name" {...described('name')} {...register('name')} />
        </Field>
        {type === 'CORPORATE' ? <>
          <Field id={id('companyName')} label="Razão social" error={fieldError('companyName')}>
            <input className="input" id={id('companyName')} {...described('companyName')} {...register('companyName')} />
          </Field>
          <Field id={id('cnpj')} label="CNPJ" error={fieldError('cnpj')} hint="Numérico ou alfanumérico (ex.: 12.ABC.345/01DE-35).">
            <input className="input" id={id('cnpj')} autoCapitalize="characters" placeholder="00.000.000/0000-00" {...described('cnpj')} {...register('cnpj')} />
          </Field>
        </> : <>
          <Field id={id('cpf')} label="CPF" error={fieldError('cpf')}>
            <input className="input" id={id('cpf')} inputMode="numeric" placeholder="000.000.000-00" {...described('cpf')} {...register('cpf')} />
          </Field>
          <Field id={id('birthDate')} label="Data de nascimento (opcional)" error={fieldError('birthDate')}>
            <input className="input" id={id('birthDate')} type="date" {...described('birthDate')} {...register('birthDate')} />
          </Field>
        </>}
        <Field id={id('email')} label="E-mail" error={fieldError('email')}>
          <input className="input" id={id('email')} type="email" autoComplete="email" {...described('email')} {...register('email')} />
        </Field>
        <Field id={id('phone')} label="Telefone" error={fieldError('phone')}>
          <input className="input" id={id('phone')} type="tel" autoComplete="tel" placeholder="(85) 99999-0000" {...described('phone')} {...register('phone')} />
        </Field>
      </div>
      {create.error && <p className="notice notice-error" role="alert">{errorMessage(create.error)}
        {create.error instanceof ApiError && create.error.details.length > 0 && <> {create.error.details.join(' ')}</>}</p>}
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={create.isPending}>{create.isPending ? 'Salvando…' : 'Cadastrar cliente'}</button>
        {onCancel && <button type="button" className="btn" onClick={onCancel}>Cancelar</button>}
      </div>
    </form>
  );
}

export function Field({ id, label, error, hint, children }: { id: string; label: string; error?: string | undefined; hint?: string; children: ReactNode }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && <span className="hint">{hint}</span>}
      {error && <span className="field-error" id={`${id}-error`}>{error}</span>}
    </div>
  );
}
