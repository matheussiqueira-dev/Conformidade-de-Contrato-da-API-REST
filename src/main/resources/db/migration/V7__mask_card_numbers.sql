-- D022 (DEC-ERP-04, B-05): o /payment e o /orders legados gravavam o numero completo do cartao em card_number.
-- O final passa para card_last4 e card_number deixa de guardar qualquer digito; o CHECK impede nova gravacao.
UPDATE payment
   SET card_last4 = right(regexp_replace(card_number, '[^0-9]', '', 'g'), 4)
 WHERE payment_type = 'CARD' AND card_last4 IS NULL
   AND length(regexp_replace(card_number, '[^0-9]', '', 'g')) >= 4;

UPDATE payment SET card_number = NULL WHERE card_number IS NOT NULL;

ALTER TABLE payment ADD CONSTRAINT ck_payment_card_number_absent CHECK (card_number IS NULL);
